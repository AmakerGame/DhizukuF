package com.EdS.DhizukuF.plugin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.EdS.DhizukuF.plugin.manager.PluginManager
import com.EdS.DhizukuF.plugin.model.PluginInfo
import com.EdS.DhizukuF.plugin.model.PluginState
import org.koin.compose.koinInject

/**
 * Plugins management screen.
 *
 * - List of installed plugins
 * - Install from .zip
 * - Enable / Disable / Start / Stop / Delete
 * - Shows warning + "what it receives" at the bottom of each card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsPage(
    onBack: () -> Unit = {},
    onInstallZipRequested: () -> Unit = {}   // host opens file picker
) {
    val pluginManager: PluginManager = koinInject()
    val plugins by pluginManager.plugins.collectAsState()

    var selectedPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onInstallZipRequested) {
                        Icon(Icons.Default.Add, contentDescription = "Install plugin")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Global warning
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "⚠ Plugin warning",
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

            if (plugins.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No plugins installed")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onInstallZipRequested) {
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

    // Error dialog
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

    // Details bottom sheet / dialog
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

            // What it receives (always visible at the bottom of the card)
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

            // Auto-start toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-start", modifier = Modifier.weight(1f))
                Switch(
                    checked = info.autoStart,
                    onCheckedChange = onAutoStartChange
                )
            }

            // Actions
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

            if (info.errorMessage != null) {
                Text(
                    info.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
