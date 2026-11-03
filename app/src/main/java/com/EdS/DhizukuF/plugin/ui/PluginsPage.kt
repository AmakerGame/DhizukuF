package com.EdS.DhizukuF.plugin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.EdS.DhizukuF.plugin.manager.PluginManager
import com.EdS.DhizukuF.plugin.model.PluginInfo
import com.EdS.DhizukuF.plugin.model.PluginState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream

/**
 * Plugins management screen.
 *
 * - Install from .zip (SAF file picker)
 * - Enable / Disable / Start / Stop / Delete
 * - Shows warning + "what it receives"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsPage(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val pluginManager: PluginManager = koinInject()
    val plugins by pluginManager.plugins.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }

    fun installFromUri(uri: Uri) {
        scope.launch {
            installing = true
            errorMessage = null
            val result = withContext(Dispatchers.IO) {
                try {
                    val cache = File(context.cacheDir, "plugin_install_${System.currentTimeMillis()}.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(cache).use { output -> input.copyTo(output) }
                    } ?: return@withContext Result.failure(Exception("Cannot open selected file"))
                    val r = pluginManager.installFromZip(cache)
                    cache.delete()
                    r
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            installing = false
            if (result.isFailure) {
                errorMessage = result.exceptionOrNull()?.message ?: "Install failed"
            }
        }
    }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) installFromUri(uri)
    }

    fun openZipPicker() {
        zipPicker.launch(arrayOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream",
            "*/*"
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { openZipPicker() }, enabled = !installing) {
                        Icon(Icons.Default.Add, contentDescription = "Install plugin")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Plugin warning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Plugins run with the same privileges as DhizukuF (including Device Owner). " +
                            "Only install plugins from sources you trust. " +
                            "Each plugin shows what it receives (dangerous commands, auto-start, UI access).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (installing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            if (plugins.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No plugins installed")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { openZipPicker() }, enabled = !installing) {
                            Text("Install from .zip")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(plugins, key = { it.manifest.id }) { plugin ->
                        PluginCard(
                            info = plugin,
                            onEnable = { enabled ->
                                val result = pluginManager.setEnabled(plugin.manifest.id, enabled)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message
                                }
                            },
                            onStart = {
                                val result = pluginManager.start(plugin.manifest.id)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message
                                }
                            },
                            onStop = { pluginManager.stop(plugin.manifest.id) },
                            onDelete = { pluginManager.delete(plugin.manifest.id) },
                            onAutoStartChange = { auto ->
                                pluginManager.setAutoStart(plugin.manifest.id, auto)
                            },
                            onDetails = { selectedPlugin = plugin }
                        )
                    }
                }
            }
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Cannot perform action") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }

    selectedPlugin?.let { plugin ->
        AlertDialog(
            onDismissRequest = { selectedPlugin = null },
            title = { Text(plugin.manifest.name) },
            text = {
                Column {
                    Text("Version: ${plugin.manifest.version}")
                    Text("Author: ${plugin.manifest.author.ifBlank { "—" }}")
                    Text("State: ${plugin.state}")
                    Spacer(Modifier.height(8.dp))
                    Text("What it receives:", fontWeight = FontWeight.Bold)
                    plugin.whatItReceives.forEach {
                        Text("• $it")
                    }
                    if (plugin.manifest.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(plugin.manifest.description)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPlugin = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PluginCard(
    info: PluginInfo,
    onEnable: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onDetails: () -> Unit
) {
    val enabled = info.state == PluginState.ENABLED

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(info.manifest.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${info.manifest.version} • ${info.state.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnable
                )
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text("Receives:", style = MaterialTheme.typography.labelMedium)
                    info.whatItReceives.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-start", modifier = Modifier.weight(1f))
                Switch(
                    checked = info.autoStart,
                    onCheckedChange = onAutoStartChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!info.autoStart && info.state != PluginState.ENABLED) {
                    TextButton(onClick = onStart) { Text("Start") }
                }
                if (info.state == PluginState.ENABLED) {
                    TextButton(onClick = onStop) { Text("Stop") }
                }
                TextButton(onClick = onDetails) { Text("Details") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }

            val errorMsg = info.errorMessage
            if (errorMsg != null) {
                Text(
                    errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
