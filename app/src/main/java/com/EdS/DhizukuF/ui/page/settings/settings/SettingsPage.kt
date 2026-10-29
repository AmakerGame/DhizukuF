package com.EdS.DhizukuF.ui.page.settings.settings

import com.EdS.DhizukuF.ui.widget.tvFocusBorder

import com.EdS.DhizukuF.ui.page.settings.SettingsRoute

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.twotone.Description

import com.EdS.DhizukuF.ui.widget.TvIconButton

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.SwitchRow

import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    windowInsets: WindowInsets,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        viewModel.collect()
    }

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(windowInsets.exclude(WindowInsetsSides.Bottom))
            .fillMaxSize(),
        contentWindowInsets = windowInsets.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TvIconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Text(stringResource(R.string.settings))
                }
            )
        }) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CompatHubCard() }

            item {
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SwitchRow(
                            title = stringResource(R.string.enable_dhizuku),
                            description = stringResource(R.string.enable_dhizuku_desc),
                            checked = viewModel.state.dhizukuEnabled,
                            onCheckedChange = viewModel::setDhizukuEnabled
                        )

                        SwitchRow(
                            title = stringResource(R.string.enable_dish),
                            description = stringResource(R.string.enable_dish_desc),
                            checked = viewModel.state.dishEnabled,
                            onCheckedChange = viewModel::setDishEnabled
                        )

                        SwitchRow(
                            title = stringResource(R.string.advanced_confirmation),
                            description = stringResource(R.string.advanced_confirmation_desc),
                            checked = viewModel.state.advancedConfirmation,
                            onCheckedChange = viewModel::setAdvancedConfirmation
                        )

                        SwitchRow(
                            title = stringResource(R.string.show_dish_label),
                            description = stringResource(R.string.show_dish_label_desc),
                            checked = viewModel.state.showDishLabel,
                            onCheckedChange = viewModel::setShowDishLabel
                        )

                        SwitchRow(
                            title = stringResource(R.string.daemon_notification),
                            description = stringResource(R.string.daemon_notification_desc),
                            checked = viewModel.state.daemonNotification,
                            onCheckedChange = viewModel::setDaemonNotificationEnabled
                        )
                    }
                }
            }

            item {
                LogsEntry(onClick = { navController.navigate(SettingsRoute.LogSettings.route) })
            }
        }
    }
}

/** The "Logs" row of Settings: opens the page with the logging options. */
@Composable
private fun LogsEntry(onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusBorder(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = Icons.TwoTone.Description, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.logs), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.logs_settings_desc), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
