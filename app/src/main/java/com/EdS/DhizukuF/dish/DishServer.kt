package com.EdS.DhizukuF.dish

import com.EdS.DhizukuF.dish.DishRequests

import android.content.Context
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.SystemClock
import android.util.Log
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.server.DhizukuState
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Local console endpoint of Dhizuku. A terminal client (dish) connects over an abstract unix
 * socket; the caller is identified by SO_PEERCRED (uid) and must hold the same Dhizuku permission
 * as any other client app. Commands are executed here, in the Device Owner process.
 */
object DishServer : KoinComponent {
    private const val TAG = "DishServer"

    private val lock = Any()
    private var server: LocalServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    private val context by inject<Context>()
    private val appRepo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()

    private sealed interface Verdict {
        data object Allowed : Verdict
        data object NeedPermission : Verdict
        class Denied(val message: String) : Verdict
    }

    fun start() {
        synchronized(lock) {
            if (server != null) return
            val socket = try {
                LocalServerSocket(DishProtocol.SOCKET_NAME)
            } catch (e: IOException) {
                Log.w(TAG, "cannot bind ${DishProtocol.SOCKET_NAME}", e)
                return
            }
            server = socket
            Thread({ acceptLoop(socket) }, "dish-accept").apply {
                isDaemon = true
                start()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            try {
                server?.close()
            } catch (_: IOException) {
            }
            server = null
        }
    }

    private fun acceptLoop(socket: LocalServerSocket) {
        while (true) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                break
            }
            try {
                executor.execute { handle(client) }
            } catch (e: Throwable) {
                try {
                    client.close()
                } catch (_: IOException) {
                }
            }
        }
    }

    private fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun handle(client: LocalSocket) {
        try {
            client.soTimeout = 120_000
            val uid = client.peerCredentials.uid
            val input = DataInputStream(BufferedInputStream(client.inputStream))
            val output = DataOutputStream(BufferedOutputStream(client.outputStream))

            if (input.readInt() != DishProtocol.MAGIC || input.readInt() != DishProtocol.VERSION) {
                deny(output, str(R.string.dish_err_protocol))
                return
            }

            when (val verdict = authorize(uid)) {
                Verdict.Allowed -> output.writeByte(DishProtocol.ST_OK)
                is Verdict.Denied -> {
                    deny(output, verdict.message)
                    return
                }

                Verdict.NeedPermission -> {
                    DishApproval.results.remove(uid)
                    val name = label(uid)
                    output.writeByte(DishProtocol.ST_NEED_PERMISSION)
                    output.writeUTF(str(R.string.dish_need_permission, name))
                    output.flush()
                    // Our own registration request: notification with Allow / Deny / Block.
                    DishRequests.show(context, uid, name)
                    val approved = try {
                        waitForApproval(uid)
                    } finally {
                        DishRequests.cancel(context, uid)
                    }
                    if (!approved) {
                        deny(output, str(R.string.dish_err_denied))
                        return
                    }
                    output.writeByte(DishProtocol.ST_OK)
                }
            }
            output.flush()

            val argc = input.readInt()
            if (argc !in 0..DishProtocol.MAX_ARGS) {
                output.writeByte(DishProtocol.FR_ERR)
                output.writeUTF(str(R.string.dish_err_protocol) + "\n")
                output.writeByte(DishProtocol.FR_END)
                output.writeInt(2)
                output.flush()
                return
            }
            val args = List(argc) { input.readUTF() }

            val result = DishCommands(context).execute(args)
            output.writeChunked(DishProtocol.FR_OUT, result.out)
            output.writeChunked(DishProtocol.FR_ERR, result.err)
            output.writeByte(DishProtocol.FR_END)
            output.writeInt(result.code)
            output.flush()
        } catch (e: Throwable) {
            Log.w(TAG, "client failed", e)
        } finally {
            try {
                client.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun deny(output: DataOutputStream, message: String) {
        output.writeByte(DishProtocol.ST_DENIED)
        output.writeUTF(message)
        output.flush()
    }

    private fun DataOutputStream.writeChunked(type: Int, text: String) {
        var i = 0
        while (i < text.length) {
            var end = minOf(text.length, i + DishProtocol.MAX_CHUNK)
            if (end < text.length && Character.isHighSurrogate(text[end - 1])) end--
            writeByte(type)
            writeUTF(text.substring(i, end))
            i = end
        }
    }

    private fun authorize(uid: Int): Verdict {
        if (!DhizukuState.state.isOwner) return Verdict.Denied(str(R.string.dish_err_not_owner))
        if (!settingsRepo.isDhizukuEnabled) return Verdict.Denied(str(R.string.dish_err_disabled))

        val signature = context.packageManager.getPackageInfoForUid(uid)?.signature
            ?: return Verdict.Denied(str(R.string.dish_err_unknown_caller))
        val entity = runBlocking { appRepo.findByUID(uid) }

        if (entity?.blocked == true) return Verdict.Denied(str(R.string.dish_err_blocked))
        if (entity != null && entity.allowApi && entity.signature == signature) return Verdict.Allowed

        // Make the caller visible in DhizukuF > App management (switch off) so it can also be
        // approved from the list, exactly like every other client app.
        registerPending(uid, signature, entity)

        if (settingsRepo.isWhitelistMode) return Verdict.Denied(str(R.string.dish_err_whitelist))
        if (!settingsRepo.isConfirmationDialog) return Verdict.Denied(str(R.string.dish_err_no_confirmation))
        return Verdict.NeedPermission
    }

    private fun registerPending(uid: Int, signature: String, entity: AppEntity?) {
        try {
            runBlocking {
                if (entity == null) {
                    appRepo.insert(AppEntity(uid = uid, signature = signature, allowApi = false))
                } else if (entity.signature != signature) {
                    appRepo.update(entity.copy(signature = signature, allowApi = false))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "cannot register pending app $uid", e)
        }
    }

    private fun waitForApproval(uid: Int): Boolean {
        val deadline = SystemClock.elapsedRealtime() + DishProtocol.APPROVAL_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val decision = DishApproval.results[uid]
            if (decision == DishDecision.DENY || decision == DishDecision.BLOCK) return false
            when (authorize(uid)) {
                Verdict.Allowed -> return true
                is Verdict.Denied -> return false
                Verdict.NeedPermission -> Unit
            }
            Thread.sleep(300)
        }
        return false
    }

    private fun label(uid: Int): String {
        val pm = context.packageManager
        val pkg = pm.getPackagesForUid(uid)?.firstOrNull() ?: return "uid $uid"
        return try {
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (e: Exception) {
            pkg
        }
    }
}
