package com.EdS.dhizukuf.pluginapi

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Context given to a plugin. Intentionally limited for safety.
 */
interface PluginContext {
    val appContext: Context

    val isOwner: Boolean

    val dangerousCommandsEnabled: Boolean

    val autoStartEnabled: Boolean

    fun getPreference(key: String, default: String? = null): String?
    fun putPreference(key: String, value: String?)

    /**
     * Post work on the main thread (UI-safe). Java-friendly [Runnable].
     */
    fun runOnMain(block: Runnable) {
        Handler(Looper.getMainLooper()).post(block)
    }
}
