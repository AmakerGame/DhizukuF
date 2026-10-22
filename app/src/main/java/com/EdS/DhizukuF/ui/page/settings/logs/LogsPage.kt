package com.EdS.DhizukuF.ui.page.settings.logs

import com.EdS.DhizukuF.ui.widget.tvTextFieldNavigation

import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.focus.FocusDirection

import androidx.compose.foundation.text.KeyboardActions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.copy
import com.EdS.DhizukuF.logging.LogCodec
import com.EdS.DhizukuF.logging.LogEntry
import com.EdS.DhizukuF.logging.LogEvent
import com.EdS.DhizukuF.logging.LogLevel
import com.EdS.DhizukuF.logging.LogManager
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.TvIconButton
import com.EdS.DhizukuF.ui.widget.TvTextButton
import com.EdS.DhizukuF.ui.widget.tvFocusBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsPage(
    windowInsets: WindowInsets,
    navController: NavController,
    viewModel: LogsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val export = rememberLogExporter()
    val focusManager = LocalFocusManager.current
    var opened by remember { mutableStateOf<LogEntry?>(null) }

    LaunchedEffect(Unit) {
        // Translated event titles are part of what the search looks at.
        viewModel.provideTitles(LogEvent.entries.associateWith { context.getString(it.titleRes()) })
        viewModel.load()
        // New entries while the screen is open: reload shortly after the last one (not once per entry).
        LogManager.changes.drop(1).collectLatest {
            delay(600)
            viewModel.load()
        }
    }

    opened?.let { entry ->
        DetailsDialog(entry = entry, onDismiss = { opened = null })
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
                title = { Text(stringResource(R.string.logs)) },
                actions = {
                    TvIconButton(onClick = export) {
                        Icon(imageVector = Icons.TwoTone.Share, contentDescription = stringResource(R.string.logs_export))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::updateQuery,
                singleLine = true,
                label = { Text(stringResource(R.string.logs_search)) },
                leadingIcon = { Icon(imageVector = Icons.TwoTone.Search, contentDescription = null) },
                trailingIcon = {
                    if (viewModel.query.isNotEmpty()) {
                        TvIconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(imageVector = Icons.TwoTone.Clear, contentDescription = null)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .tvTextFieldNavigation()
            )

            val state = viewModel.state
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (state.total == 0) R.string.logs_empty else R.string.logs_no_results),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    val dateTimeFormat = remember { SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()) }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.rows, key = { it.id }) { row ->
                            EntryCard(
                                entry = row.entry,
                                timeText = (if (isToday(row.entry.time)) timeFormat else dateTimeFormat)
                                    .format(Date(row.entry.time)),
                                onClick = { opened = row.entry }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isToday(time: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = time }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.Error -> MaterialTheme.colorScheme.error
    LogLevel.Warning -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun EntryCard(entry: LogEntry, timeText: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusBorder(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    stringResource(entry.event.titleRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = levelColor(entry.level)
                )
            }
            if (entry.appLabel != null) Line(stringResource(R.string.log_field_app), entry.appLabel)
            if (entry.packageName != null) Line(stringResource(R.string.log_field_package), entry.packageName)
            if (entry.message != null) Line(null, entry.message)
            if (entry.command != null) Line(stringResource(R.string.log_field_command), entry.command)
            if (entry.result != null) Line(stringResource(R.string.log_field_result), entry.result)
            if (entry.error != null) Line(stringResource(R.string.log_field_error), entry.error.lineSequence().first())
        }
    }
}

@Composable
private fun Line(label: String?, value: String) {
    Text(
        text = if (label == null) value else "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DetailsDialog(entry: LogEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(entry.event.titleRes())) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // only what is known: an unknown field is not shown at all
                Field(R.string.log_field_time, LogCodec.timestamp(entry.time))
                Field(R.string.log_field_event, entry.event.english)
                Field(R.string.log_field_level, stringResource(entry.level.entryLabelRes()))
                Field(R.string.log_field_category, stringResource(entry.category.labelRes()))
                entry.appLabel?.let { Field(R.string.log_field_app, it) }
                entry.packageName?.let { Field(R.string.log_field_package, it) }
                if (entry.uid >= 0) Field(R.string.log_field_uid, entry.uid.toString())
                entry.message?.let { Field(R.string.log_field_message, it) }
                entry.command?.let { Field(R.string.log_field_command, it, mono = true) }
                entry.result?.let { Field(R.string.log_field_result, it, mono = true) }
                entry.error?.let { Field(R.string.log_field_error, it, mono = true) }
                entry.details?.let { Field(R.string.log_field_details, it, mono = true) }
            }
        },
        dismissButton = {
            TvTextButton(onClick = { context.copy(LogCodec.readable(entry)) }) {
                Text(stringResource(R.string.logs_copy))
            }
        },
        confirmButton = {
            TvTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}

@Composable
private fun Field(labelRes: Int, value: String, mono: Boolean = false) {
    Column {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        )
    }
}
