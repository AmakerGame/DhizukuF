package com.EdS.DhizukuF.dish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import kotlin.concurrent.thread

/**
 * Handshake endpoint for the dish client. The client broadcasts its own Binder; we answer by
 * calling it with ours. Starting the broadcast also wakes this app up if it was not running.
 */
class DishReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val client = intent.getBundleExtra("dish")?.getBinder("cb") ?: return
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
                pending.finish()
            }
        }
    }
}
