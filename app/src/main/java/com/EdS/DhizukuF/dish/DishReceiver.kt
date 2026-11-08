package com.EdS.DhizukuF.dish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

/**
 * Handshake endpoint for the dish client. The client broadcasts its own Binder; we answer by
 * calling it with ours. Starting the broadcast also wakes this app up if it was not running.
 *
 * Any app can send this broadcast, so it is hardened: malformed extras are ignored (they must not
 * crash the Device Owner app) and the number of parallel handshakes is limited. The Binder we hand
 * out authorizes every call by Binder.getCallingUid(), so receiving it gives no access by itself.
 */
class DishReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val client = try {
            intent.getBundleExtra("dish")?.getBinder("cb")
        } catch (t: Throwable) {
            Log.w("DishReceiver", "ignored a malformed request", t)
            null
        } ?: return

        if (!slots.tryAcquire()) {
            Log.w("DishReceiver", "too many handshakes, request dropped")
            return
        }
        val app = context.applicationContext
        val pending = goAsync()
        thread(name = "dish-handshake") {
            val data = Parcel.obtain()
            try {
                data.writeStrongBinder(DishEngine.binder(app))
                client.transact(1, data, null, IBinder.FLAG_ONEWAY)
            } catch (t: Throwable) {
                Log.w("DishReceiver", "handshake failed", t)
            } finally {
                data.recycle()
                slots.release()
                pending.finish()
            }
        }
    }

    private companion object {
        val slots = Semaphore(8)
    }
}
