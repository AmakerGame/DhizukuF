package com.EdS.DhizukuF.ui.page.settings.logs

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.toast
import com.EdS.DhizukuF.logging.LogCategory
import com.EdS.DhizukuF.logging.LogConfig
import com.EdS.DhizukuF.logging.LogLevel
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.SwitchRow
import com.EdS.DhizukuF.ui.widget.TvButton
import com.EdS.DhizukuF.ui.widget.TvIconButton
import com.EdS.DhizukuF.ui.widget.TvOutlinedButton
import com.EdS.DhizukuF.ui.widget.TvRadioRow
import com.EdS.DhizukuF.ui.widget.TvTextButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSettingsPage(
    windowInsets: WindowInsets,
    navController: NavController,
    viewModel: LogSettingsViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        viewModel.collect()
    }
    val context = LocalContext.current
    val export = rememberLogExporter()
    val config = viewModel.config
    val hasLogs = viewModel.usedBytes > 0L
    var askClear by remember { mutableStateOf(false) }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text(stringResource(R.string.logs_clear_confirm_title)) },
            text = { Text(stringResource(R.string.logs_clear_confirm_text)) },
            confirmButton = {
                TvTextButton(onClick = {
                    askClear = false
                    viewModel.clear()
                    context.toast(R.string.logs_cleared)
                }) {
                    Text(stringResource(R.string.logs_clear))
                }
            },
            dismissButton = {
                TvTextButton(onClick = { askClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(windowInsets.exclude(WindowInsetsSides.Bottom))
            .fillMaxSize(),
        contentWindowInsets = windowInsets.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TvIconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.TwoTone.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(R.string.logs)) }
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    SwitchRow(
                        title = stringResource(R.string.logs_enable),
                        description = stringResource(R.string.logs_enable_desc),
                        checked = config.enabled,
                        onCheckedChange = viewModel::setEnabled
                    )
                }
            }

            item {
                Card(title = R.string.logs_level_title) {
                    // Errors only / Warnings / Normal / Debug: from the strictest to the most talkative
                    listOf(LogLevel.Error, LogLevel.Warning, LogLevel.Normal, LogLevel.Debug).forEach { level ->
                        TvRadioRow(
                            title = stringResource(level.settingLabelRes()),
                            description = stringResource(level.settingDescRes()),
                            selected = config.level == level,
                            onClick = { viewModel.setLevel(level) },
                            enabled = config.enabled
                        )
                    }
                }
            }

            item {
                Card(title = R.string.logs_categories_title) {
                    LogCategory.entries.forEach { category ->
                        SwitchRow(
                            title = stringResource(category.labelRes()),
                            description = stringResource(category.descRes()),
                            checked = category in config.categories,
                            onCheckedChange = { on -> viewModel.setCategory(category, on) },
                            enabled = config.enabled
                        )
                    }
                }
            }

            item {
                Card(title = R.string.logs_size_title) {
                    Text(
                        stringResource(
                            R.string.logs_size_used,
                            Formatter.formatShortFileSize(context, viewModel.usedBytes),
                            config.maxSizeMb
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    LogConfig.SIZE_CHOICES_MB.forEach { mb ->
                        TvRadioRow(
                            title = stringResource(R.string.logs_size_mb, mb),
                            selected = config.maxSizeMb == mb,
                            onClick = { viewModel.setMaxSize(mb) },
                            enabled = config.enabled
                        )
                    }
                }
            }

            item {
                Card {
                    TvButton(
                        onClick = export,
                        enabled = hasLogs,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.logs_export))
                    }
                    Text(
                        stringResource(R.string.logs_export_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    // Clear when there is something to delete (works with logging off too).
                    TvOutlinedButton(
                        onClick = { askClear = true },
                        enabled = hasLogs,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.logs_clear))
                    }
                    Text(
                        stringResource(R.string.logs_clear_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/** The card of the settings screens: same look as in Settings and Security. */
@Composable
private fun Card(title: Int? = null, content: @Composable () -> Unit) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title != null) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
