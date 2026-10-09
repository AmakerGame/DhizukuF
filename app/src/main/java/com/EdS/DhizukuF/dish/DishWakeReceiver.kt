package com.EdS.DhizukuF.dish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.EdS.DhizukuF.server.DhizukuState

/**
 * Sent by the dish client when it cannot reach the server (for example the app process was
 * killed). Receiving it starts the process; Application.onCreate then starts the server.
 */
class DishWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DhizukuState.sync(context)
    }
}
