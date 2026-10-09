package com.EdS.DhizukuF.dish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlin.concurrent.thread

/**
 * Entry point for the dish terminal client (`am broadcast -n .../.dish.DishReceiver`).
 * The answer is returned as the ordered-broadcast result, which `am` prints to the terminal.
 */
class DishReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        val sentFrom = if (Build.VERSION.SDK_INT >= 34) sentFromUid else -1
        val token = intent.getStringExtra("t")
        val args = intent.getStringExtra("a")
        thread(name = "dish-handler") {
            try {
                val reply = DishEngine.handle(app, token, args, sentFrom)
                pending.resultCode = reply.code
                pending.resultData = Base64.encodeToString(
                    reply.body.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
                )
            } catch (t: Throwable) {
                Log.w("DishReceiver", "dish request failed", t)
                pending.resultCode = DishEngine.CODE_DENIED
                pending.resultData = Base64.encodeToString(
                    "Internal error: ${t.javaClass.simpleName}".toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
                )
            } finally {
                pending.finish()
            }
        }
    }
}
