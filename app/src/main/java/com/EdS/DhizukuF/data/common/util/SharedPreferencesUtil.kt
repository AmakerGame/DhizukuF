package com.EdS.DhizukuF.data.common.util

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

inline fun <reified T> SharedPreferences.asFlow(
    key: String,
    defaultValue: T,
    crossinline getter: () -> T = {
        val value = all.getOrElse(key) { defaultValue }
        if (value is T) value
        else defaultValue
    }
) = callbackFlow {
    // Emit the current value first.
    trySend(getter())

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (changedKey == key) {
            trySend(getter())
        }
    }
    registerOnSharedPreferenceChangeListener(listener)

    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}

fun SharedPreferences.stringFlow(key: String, defaultValue: String?) = asFlow(key, defaultValue)
