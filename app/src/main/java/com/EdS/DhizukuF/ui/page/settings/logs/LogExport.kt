package com.EdS.DhizukuF.ui.page.settings.logs

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.toast
import com.EdS.DhizukuF.logging.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Export logs": saves the journal as a text file (dhizukuf-logs-2026-10-18.txt). The system file dialog chooses
 * the place; a TV box without such a dialog gets the file in the app's own folder (the toast shows the path).
 */
@Composable
fun rememberLogExporter(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { writeTo(context, uri) }
            context.toast(if (ok) R.string.logs_export_done else R.string.logs_export_failed)
        }
    }
    return {
        if (!LogManager.hasData()) {
            context.toast(R.string.logs_export_empty)
        } else {
            val name = fileName()
            try {
                launcher.launch(name)
            } catch (_: ActivityNotFoundException) {
                scope.launch {
                    val path = withContext(Dispatchers.IO) { saveInAppFolder(context, name) }
                    if (path != null) context.toast(context.getString(R.string.logs_export_saved_to, path), android.widget.Toast.LENGTH_LONG)
                    else context.toast(R.string.logs_export_failed)
                }
            }
        }
    }
}

private fun fileName(): String =
    "dhizukuf-logs-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) + ".txt"

private fun writeTo(context: Context, uri: Uri): Boolean = try {
    context.contentResolver.openOutputStream(uri, "wt")?.use { LogManager.exportReadable(it) } != null
} catch (_: Throwable) {
    false
}

private fun saveInAppFolder(context: Context, name: String): String? = try {
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    val file = File(dir, name)
    file.outputStream().use { LogManager.exportReadable(it) }
    file.absolutePath
} catch (_: Throwable) {
    null
}
