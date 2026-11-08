package com.EdS.DhizukuF.ui.page.settings.security

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.settings.repo.AuthMode
import com.EdS.DhizukuF.security.DeviceAuth
import com.EdS.DhizukuF.security.findFragmentActivity
import com.EdS.DhizukuF.security.rememberAuthGate
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.SwitchRow
import com.EdS.DhizukuF.ui.widget.TvButton
import com.EdS.DhizukuF.ui.widget.TvIconButton
import com.EdS.DhizukuF.ui.widget.TvTextButton
import com.EdS.DhizukuF.ui.widget.tvFocusBorder
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPage(
    windowInsets: WindowInsets,
    navController: NavController,
    viewModel: SecurityViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        viewModel.collect()
    }

    val context = LocalContext.current
    val gate = rememberAuthGate()
    val state = viewModel.state

    // The screen lock can be set or removed in Android settings while this page is open: look again on return.
    var hasLock by remember { mutableStateOf(DeviceAuth.hasScreenLock(context)) }
    var canAuth by remember { mutableStateOf(DeviceAuth.canAuthenticate(context)) }
    val activity = context.findFragmentActivity()
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLock = DeviceAuth.hasScreenLock(context)
                canAuth = DeviceAuth.canAuthenticate(context)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    var noLockDialog by remember { mutableStateOf(false) }

    val promptTitle = stringResource(R.string.auth_prompt_title)
    val promptSecurity = stringResource(R.string.auth_prompt_security)

    fun openLockSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Many TV boxes have no such screen.
            Toast.makeText(context, R.string.security_open_lock_settings_failed, Toast.LENGTH_LONG).show()
        }
    }

    fun requestMode(mode: AuthMode) {
        if (mode == state.authMode) return
        if (mode != AuthMode.Off && !canAuth) {
            noLockDialog = true
            return
        }
        // Changing the protection itself is protected: this also proves that the prompt works before it is relied on.
        gate.confirmAlways(promptTitle, promptSecurity) { viewModel.setAuthMode(mode) }
    }

    if (noLockDialog) {
        AlertDialog(
            onDismissRequest = { noLockDialog = false },
            title = { Text(stringResource(R.string.security_no_lock_title)) },
            text = {
                Text(
                    stringResource(
                        if (hasLock) R.string.security_auth_unavailable_msg else R.string.security_no_lock_msg
                    )
                )
            },
            dismissButton = {
                TvTextButton(onClick = {
                    noLockDialog = false
                    openLockSettings()
                }) {
                    Text(stringResource(R.string.security_open_lock_settings))
                }
            },
            confirmButton = {
                TvTextButton(onClick = { noLockDialog = false }) {
                    Text(stringResource(R.string.confirm))
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
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = { Text(stringResource(R.string.security)) }
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
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SwitchRow(
                            title = stringResource(R.string.whitelist_mode),
                            description = stringResource(R.string.whitelist_mode_desc),
                            checked = state.whitelistMode,
                            onCheckedChange = viewModel::setWhitelistMode
                        )

                        SwitchRow(
                            title = stringResource(R.string.enable_dangerous),
                            description = stringResource(R.string.enable_dangerous_desc),
                            checked = state.dangerousEnabled,
                            onCheckedChange = viewModel::setDangerousEnabled,
                            titleColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.security_auth_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.security_auth_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!canAuth) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.security_no_lock_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        stringResource(
                                            if (hasLock) R.string.security_auth_unavailable_msg
                                            else R.string.security_no_lock_msg
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (state.authMode != AuthMode.Off) {
                                        Text(
                                            stringResource(R.string.security_not_enforced),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    TvButton(
                                        onClick = { openLockSettings() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(stringResource(R.string.security_open_lock_settings))
                                    }
                                }
                            }
                        }

                        ModeRow(
                            title = stringResource(R.string.auth_mode_off),
                            description = stringResource(R.string.auth_mode_off_desc),
                            selected = state.authMode == AuthMode.Off,
                            onClick = { requestMode(AuthMode.Off) }
                        )
                        ModeRow(
                            title = stringResource(R.string.auth_mode_confirm),
                            description = stringResource(R.string.auth_mode_confirm_desc),
                            selected = state.authMode == AuthMode.Confirm,
                            onClick = { requestMode(AuthMode.Confirm) }
                        )
                        ModeRow(
                            title = stringResource(R.string.auth_mode_entry),
                            description = stringResource(R.string.auth_mode_entry_desc),
                            selected = state.authMode == AuthMode.Entry,
                            onClick = { requestMode(AuthMode.Entry) }
                        )
                        ModeRow(
                            title = stringResource(R.string.auth_mode_both),
                            description = stringResource(R.string.auth_mode_both_desc),
                            selected = state.authMode == AuthMode.Both,
                            onClick = { requestMode(AuthMode.Both) }
                        )
                    }
                }
            }
        }
    }
}

/** One choice of the list. A plain selectable row: a normal D-pad focus target with a visible frame. */
@Composable
private fun ModeRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusBorder(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
