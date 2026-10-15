package com.EdS.DhizukuF.ui.page.settings.activate

import com.EdS.DhizukuF.ui.widget.TvOutlinedButton

import com.EdS.DhizukuF.ui.widget.TvIconButton

import kotlinx.coroutines.delay
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.lazy.itemsIndexed
import com.EdS.DhizukuF.ui.widget.tvFocusBorder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import com.EdS.DhizukuF.ui.widget.tvToggle
import com.EdS.DhizukuF.ui.widget.TvTextButton
import androidx.compose.ui.focus.focusProperties
import android.content.ComponentName

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.server.DhizukuState
import com.EdS.DhizukuF.ui.page.settings.SettingsRoute
import com.EdS.DhizukuF.ui.theme.AppIconCache
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.theme.plus

import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivatePage(
    windowInsets: WindowInsets,
    navController: NavController,
    mode: SettingsRoute.Activate.Mode,
    viewModel: ActivateViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        viewModel.collect()
    }

    val compState = remember {
        mutableStateOf<ComponentName?>(null)
    }

    val pm = LocalContext.current.packageManager
    val useListFooter = remember {
        pm.hasSystemFeature("android.software.leanback") || !pm.hasSystemFeature("android.hardware.touchscreen")
    }

    // One focus requester per list row; the rows point at each other explicitly.
    val rowFocus = remember(viewModel.state.data.size) {
        List(viewModel.state.data.size) { FocusRequester() }
    }

    LaunchedEffect(viewModel.state.data) {
        compState.value = viewModel.state.data.find { it.enabled }?.admin?.component
    }

    val title = stringResource(
        when (mode) {
            SettingsRoute.Activate.Mode.Dhizuku -> R.string.home_dhizuku_title
            SettingsRoute.Activate.Mode.Shizuku -> R.string.home_shizuku_title
        }
    )

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
                    }, modifier = Modifier.focusProperties {
                        // On a TV the remote's Back key does this; the D-pad stays inside the list.
                        canFocus = !useListFooter
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Text(title)
                }
            )
        },
        bottomBar = {
            val comp by compState
            // On a TV / box without a touch screen the confirm button is part of the list (see ItemsWidget).
            if (!useListFooter) AnimatedVisibility(
                visible = comp != null,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                ConfirmBar(mode = mode, viewModel = viewModel, compState = compState)
            }
        }) {
        val pullToRefreshState = rememberPullToRefreshState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(state = pullToRefreshState,
                    isRefreshing = viewModel.state.loading,
                    onRefresh = {
                        viewModel.collectData()
                    }
                )
        ) {
            ItemsWidget(
                viewModel = viewModel,
                contentPadding = it,
                state = compState,
                rowFocus = rowFocus,
                footer = if (useListFooter) {
                    {
                        AnimatedVisibility(
                            visible = compState.value != null,
                            enter = scaleIn(),
                            exit = scaleOut()
                        ) {
                            ConfirmBar(mode = mode, viewModel = viewModel, compState = compState)
                        }
                    }
                } else null
            )
            PullToRefreshDefaults.Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(it),
                state = pullToRefreshState,
                isRefreshing = viewModel.state.loading
            )
        }
    }

    if (viewModel.state.status == ActivateViewState.Status.Waiting) return
    AlertDialog(onDismissRequest = {
        viewModel.cancel()
    }, confirmButton = {
        val comp by compState
        val isRunning = viewModel.state.status !is ActivateViewState.Status.End
        val isSuccess =
            !isRunning && (viewModel.state.status as ActivateViewState.Status.End).error == null

        // The remote has to be able to press a button of this dialog at once: focus the first one.
        val dialogFocus = remember { FocusRequester() }
        LaunchedEffect(viewModel.state.status) {
            if (useListFooter) {
                delay(250)
                runCatching { dialogFocus.requestFocus() }
            }
        }

        if (isRunning || !isSuccess) TvTextButton(
            onClick = { viewModel.cancel() },
            modifier = Modifier.focusRequester(dialogFocus)
        ) {
            Text(stringResource(R.string.cancel))
        }
        if (isRunning) return@AlertDialog
        if (!isSuccess) TvTextButton(onClick = {
            when (viewModel.ownerType) {
                ActivateViewModel.OwnerType.Profile ->
                    viewModel.activateAsProfileOwner(mode = mode, comp = comp!!)

                else ->
                    viewModel.activateAsDeviceOwner(mode = mode, comp = comp!!)
            }
        }) {
            Text(stringResource(R.string.retry))
        } else TvTextButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier.focusRequester(dialogFocus)
        ) {
            Text(stringResource(R.string.finish))
        }
    }, title = {
        Text(title)
    }, text = {
        @Suppress("AnimatedContentLabel")
        AnimatedContent(
            viewModel.state.status,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            if (it == ActivateViewState.Status.Running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            else if (it is ActivateViewState.Status.End) {
                Text(
                    if (it.error == null) stringResource(
                        when (mode) {
                            SettingsRoute.Activate.Mode.Dhizuku -> R.string.home_status_owner_transferred
                            SettingsRoute.Activate.Mode.Shizuku -> R.string.home_status_owner_granted
                        },
                        stringResource(
                            when (viewModel.ownerType) {
                                ActivateViewModel.OwnerType.Profile -> R.string.confirm_profile_owner
                                else -> R.string.confirm_device_owner
                            }
                        )
                    )
                    else it.error.toString(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    })
}

/**
 * The owner type menu (Shizuku only) and the confirm button. On a phone it sits in the bottom bar; on a
 * TV / box without a touch screen it is the LAST ITEM OF THE LIST, so the D-pad moves through one single list
 * (apps, then OK) and never has to jump to another part of the screen.
 */
@Composable
private fun ConfirmBar(
    mode: SettingsRoute.Activate.Mode,
    viewModel: ActivateViewModel,
    compState: State<ComponentName?>
) {
    if (mode == SettingsRoute.Activate.Mode.Shizuku) {
        var ownerType by remember { mutableStateOf(ActivateViewModel.OwnerType.Device) }
        var menuExpanded by remember { mutableStateOf(false) }
        var profileOwnerWarningShow by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TvOutlinedButton(onClick = { menuExpanded = true }) {
                    Text(
                        stringResource(
                            when (ownerType) {
                                ActivateViewModel.OwnerType.Profile -> R.string.confirm_profile_owner
                                else -> R.string.confirm_device_owner
                            }
                        )
                    )
                    Icon(imageVector = Icons.TwoTone.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.confirm_device_owner)) },
                        onClick = {
                            ownerType = ActivateViewModel.OwnerType.Device
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.confirm_profile_owner)) },
                        onClick = {
                            menuExpanded = false
                            profileOwnerWarningShow = true
                        }
                    )
                }
            }

            if (profileOwnerWarningShow) {
                AlertDialog(
                    onDismissRequest = { profileOwnerWarningShow = false },
                    icon = {
                        Icon(
                            imageVector = Icons.TwoTone.Warning,
                            contentDescription = null
                        )
                    },
                    title = {
                        Text(stringResource(R.string.profile_owner_warning_title))
                    },
                    text = {
                        Text(stringResource(R.string.profile_owner_warning_dsp))
                    },
                    confirmButton = {
                        TvTextButton(onClick = {
                            ownerType = ActivateViewModel.OwnerType.Profile
                            profileOwnerWarningShow = false
                        }) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TvTextButton(onClick = { profileOwnerWarningShow = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            ActivateFab(
                textRes = R.string.confirm,
                onClick = {
                    compState.value?.let {
                        when (ownerType) {
                            ActivateViewModel.OwnerType.Profile ->
                                viewModel.activateAsProfileOwner(mode = mode, comp = it)

                            else ->
                                viewModel.activateAsDeviceOwner(mode = mode, comp = it)
                        }
                    }
                }
            )
        }
    } else {
        ActivateFab(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            textRes = R.string.confirm,
            onClick = {
                compState.value?.let {
                    if (DhizukuState.state.isProfileOwner)
                        viewModel.activateAsProfileOwner(mode = mode, comp = it)
                    else
                        viewModel.activateAsDeviceOwner(mode = mode, comp = it)
                }
            }
        )
    }
}

@Composable
private fun ActivateFab(
    modifier: Modifier = Modifier,
    textRes: Int,
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        modifier = modifier.tvFocusBorder(RoundedCornerShape(16.dp)),
        icon = {
            Icon(
                imageVector = Icons.TwoTone.Check,
                contentDescription = null
            )
        },
        text = {
            Text(stringResource(textRes))
        },
        onClick = onClick
    )
}

@Composable
private fun ItemsWidget(
    viewModel: ActivateViewModel,
    contentPadding: PaddingValues,
    state: MutableState<ComponentName?>,
    rowFocus: List<FocusRequester>,
    footer: (@Composable () -> Unit)? = null
) {
    // On a TV or a box without a touch screen put the focus on the first app, so the remote can
    // select at once.
    val pm = LocalContext.current.packageManager
    val isTv = pm.hasSystemFeature("android.software.leanback") ||
        !pm.hasSystemFeature("android.hardware.touchscreen")
    val firstItem = rowFocus.firstOrNull()
    LaunchedEffect(isTv, rowFocus) {
        if (isTv && firstItem != null) {
            delay(300)
            runCatching { firstItem.requestFocus() }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp) + contentPadding,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(
            viewModel.state.data,
            key = { _, item -> item.admin.component.flattenToShortString() }
        ) { index, item ->
            ItemWidget(
                viewModel = viewModel,
                data = item,
                state = state,
                rowModifier = rowFocus.getOrNull(index)?.let { own ->
                    Modifier
                        .focusRequester(own)
                        .focusProperties {
                            up = rowFocus.getOrNull(index - 1) ?: FocusRequester.Default
                            down = rowFocus.getOrNull(index + 1) ?: FocusRequester.Default
                        }
                } ?: Modifier
            )
        }
        if (footer != null) {
            item(key = "confirm") { footer() }
        }
    }
}

@Composable
private fun LazyItemScope.ItemWidget(
    viewModel: ActivateViewModel,
    data: ActivateViewData,
    state: MutableState<ComponentName?>,
    rowModifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    fun setCheckedChange(bool: Boolean = state.value?.compareTo(data.admin.component) != 0) {
        state.value = if (bool) data.admin.component
        else null
    }

    val checked = state.value?.compareTo(data.admin.component) == 0

    // The whole row is one focus target (D-pad / remote friendly) and toggles the selection.
    ElevatedCard(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
    ) {
        Row(
            modifier = rowModifier
                .fillMaxWidth()
                .tvFocusBorder(RoundedCornerShape(12.dp))
                .tvToggle(
                    checked = checked,
                    role = Role.Checkbox,
                    onValueChange = { setCheckedChange(it) }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null
            )

            val packageManager = LocalContext.current.packageManager
            val imageBitmap by AppIconCache.rememberImageBitmapState(data.admin)
            val label = data.admin.loadLabel(packageManager).toString()
            val text = data.admin.component.flattenToShortString()
            Image(
                bitmap = imageBitmap,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically),
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
