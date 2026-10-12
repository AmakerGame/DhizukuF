package com.EdS.DhizukuF.ui.page.settings.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.hub.CompatHub
import com.EdS.DhizukuF.ui.widget.TvTextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Install / update / remove the Dhizuku Compatibility Hub, the hidden plugin that lets apps written
 * for the original Dhizuku use DhizukuF.
 */
@Composable
fun CompatHubCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<CompatHub.Status?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        status = withContext(Dispatchers.IO) { CompatHub.status(context) }
    }

    fun run(action: () -> CompatHub.Outcome) {
        if (busy) return
        busy = true
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { action() }
            busy = false
            refresh++
            if (!outcome.ok) {
                Toast.makeText(
                    context,
                    context.getString(R.string.hub_failed, outcome.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.hub_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.hub_desc),
                style = MaterialTheme.typography.bodyMedium
            )
            val current = status
            if (current != null) {
                Text(
                    stringResource(
                        when (current) {
                            CompatHub.Status.NOT_INSTALLED -> R.string.hub_state_none
                            CompatHub.Status.UP_TO_DATE -> R.string.hub_state_ok
                            CompatHub.Status.OUTDATED -> R.string.hub_state_old
                            CompatHub.Status.CONFLICT -> R.string.hub_state_conflict
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (current == CompatHub.Status.CONFLICT) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (current) {
                        CompatHub.Status.NOT_INSTALLED ->
                            TvTextButton(onClick = { run { CompatHub.install(context) } }, enabled = !busy) {
                                Text(stringResource(R.string.hub_install))
                            }

                        CompatHub.Status.OUTDATED ->
                            TvTextButton(onClick = { run { CompatHub.install(context) } }, enabled = !busy) {
                                Text(stringResource(R.string.hub_update))
                            }

                        CompatHub.Status.UP_TO_DATE ->
                            TvTextButton(onClick = { run { CompatHub.uninstall(context) } }, enabled = !busy) {
                                Text(stringResource(R.string.hub_remove))
                            }

                        CompatHub.Status.CONFLICT -> Unit
                    }
                }
            }
        }
    }
}
