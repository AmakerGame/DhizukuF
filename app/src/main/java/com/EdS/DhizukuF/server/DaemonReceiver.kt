package com.EdS.DhizukuF.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DaemonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        // Refresh the owner state
        DhizukuState.sync(context)
    }
}