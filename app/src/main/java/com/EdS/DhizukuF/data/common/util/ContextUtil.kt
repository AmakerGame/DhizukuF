package com.EdS.DhizukuF.data.common.util

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Handler
import android.content.ActivityNotFoundException
import android.os.Looper
import android.widget.Toast

import androidx.annotation.StringRes
import androidx.core.net.toUri

fun Context.openUrlInBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Android TV and some embedded images have no browser: keep the link for the user instead.
        copy(url)
    }
}

fun Context.copy(text: CharSequence) {
    Handler(Looper.getMainLooper()).post {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Label", text))
    }
}

fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, text, duration).show()
    }
}

fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    toast(getString(resId), duration)
}
