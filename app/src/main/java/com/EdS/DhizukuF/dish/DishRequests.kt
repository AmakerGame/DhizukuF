package com.EdS.DhizukuF.dish

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.ui.activity.RequestPermissionActivity
import com.rosan.dhizuku.shared.DhizukuVariables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Registration requests of the dish terminal client, owned by DhizukuF itself.
 *
 * The dish client can only try to start the confirmation window with `am start`, and Android
 * silently drops that start when the terminal is not in the foreground. So on every request the
 * server also posts its own heads-up / full-screen notification with Allow, Deny and Block
 * buttons. Whatever the user presses first (window or notification) wins.
 */
object DishRequests : KoinComponent {
    private const val TAG = "DishRequests"
    private const val CHANNEL_ID = "dish_requests"
    private const val NOTIFICATION_BASE = 20_000
    const val ACTION_DECISION = "com.EdS.DhizukuF.dish.action.DECISION"
    const val EXTRA_UID = "uid"
    const val EXTRA_DECISION = "decision"

    private val appRepo by inject<AppRepo>()

    fun notificationId(uid: Int) = NOTIFICATION_BASE + uid

    fun show(context: Context, uid: Int, label: String) {
        val manager = NotificationManagerCompat.from(context)
        try {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(
                    CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH
                ).setName(context.getString(R.string.dish_request_channel)).build()
            )

            val open = PendingIntent.getActivity(
                context,
                uid,
                Intent().setComponent(
                    ComponentName(context, RequestPermissionActivity::class.java)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(DhizukuVariables.PARAM_CLIENT_UID, uid),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.round_hourglass_empty_black_24)
                .setContentTitle(context.getString(R.string.dish_request_title))
                .setContentText(context.getString(R.string.dish_request_text, label))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.dish_request_text, label))
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(DishProtocol.APPROVAL_TIMEOUT_MS)
                .addAction(0, context.getString(R.string.agree), action(context, uid, DishDecision.ALLOW))
                .addAction(0, context.getString(R.string.refuse), action(context, uid, DishDecision.DENY))
                .addAction(0, context.getString(R.string.block), action(context, uid, DishDecision.BLOCK))

            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
            ) {
                manager.notify(notificationId(uid), builder.build())
            } else {
                Log.w(TAG, "notifications are not permitted, only the window can be used")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "cannot show request notification", e)
        }
    }

    fun cancel(context: Context, uid: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId(uid))
        } catch (_: Throwable) {
        }
    }

    private fun action(context: Context, uid: Int, decision: DishDecision): PendingIntent {
        val intent = Intent(ACTION_DECISION)
            .setComponent(ComponentName(context, DishRequestReceiver::class.java))
            .setData(Uri.parse("dish://decision/$uid/${decision.name}"))
            .putExtra(EXTRA_UID, uid)
            .putExtra(EXTRA_DECISION, decision.name)
        return PendingIntent.getBroadcast(
            context,
            uid * 4 + decision.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Stores the decision for [uid] in the app list and wakes the waiting dish connection. */
    suspend fun decide(context: Context, uid: Int, decision: DishDecision) {
        val signature = context.packageManager.getPackageInfoForUid(uid)?.signature
        if (signature != null) {
            val entity = appRepo.findByUID(uid)
            val allow = decision == DishDecision.ALLOW
            val block = decision == DishDecision.BLOCK
            if (entity == null) {
                appRepo.insert(
                    AppEntity(uid = uid, signature = signature, allowApi = allow, blocked = block)
                )
            } else {
                appRepo.update(
                    entity.copy(
                        signature = signature,
                        allowApi = allow,
                        blocked = if (block) true else entity.blocked && !allow,
                        modifiedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        DishApproval.publish(uid, decision)
        cancel(context, uid)
    }
}

class DishRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DishRequests.ACTION_DECISION) return
        val uid = intent.getIntExtra(DishRequests.EXTRA_UID, -1)
        val decision = runCatching {
            DishDecision.valueOf(intent.getStringExtra(DishRequests.EXTRA_DECISION).orEmpty())
        }.getOrNull()
        if (uid < 0 || decision == null) return

        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DishRequests.decide(app, uid, decision)
            } catch (e: Throwable) {
                Log.w("DishRequests", "decision failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
