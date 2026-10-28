package com.EdS.DhizukuF.ui.page.settings.home

import kotlinx.coroutines.withContext

import kotlinx.coroutines.delay

import androidx.compose.runtime.LaunchedEffect

import com.EdS.DhizukuF.ui.page.settings.customization.AppIconImage

import com.EdS.DhizukuF.customization.HomeLayoutLogic

import androidx.compose.ui.draw.alpha

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material.icons.twotone.VisibilityOff

import androidx.compose.material.icons.twotone.Visibility

import androidx.compose.material.icons.twotone.Palette

import androidx.compose.material.icons.twotone.Edit

import androidx.compose.material.icons.twotone.Check

import androidx.compose.material.icons.twotone.ArrowUpward

import androidx.compose.material.icons.twotone.ArrowDownward

import androidx.compose.material.icons.twotone.Description

import com.EdS.DhizukuF.ui.widget.TvOutlinedButton

import com.EdS.DhizukuF.ui.widget.TvIconButton

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.twotone.Info
import androidx.compose.ui.focus.focusProperties
import com.EdS.DhizukuF.ui.widget.TvTextButton
import com.EdS.DhizukuF.ui.widget.tvFocusBorder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.EdS.DhizukuF.dish.DishExporter
import kotlinx.coroutines.Dispatchers
import com.EdS.DhizukuF.data.common.util.toast
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Adb
import androidx.compose.material.icons.twotone.ArrowDropDown
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.Cancel
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.DoNotDisturbOn
import androidx.compose.material.icons.twotone.RoomPreferences
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.SentimentVeryDissatisfied
import androidx.compose.material.icons.twotone.SentimentVerySatisfied
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.SwapHorizontalCircle
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material.icons.twotone.SupervisorAccount
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.checkShizukuWorked
import com.EdS.DhizukuF.data.common.util.openUrlInBrowser
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.server.DhizukuState
import com.EdS.DhizukuF.ui.page.settings.SettingsRoute
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.appearModifier

import kotlin.system.exitProcess

import kotlinx.coroutines.launch

import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    windowInsets: WindowInsets,
    navController: NavController
) {
    val dhizukuState = DhizukuState.state
    val owner = dhizukuState.isOwner
    val settingsRepo = koinInject<SettingsRepo>()
    // "Logs" is hidden by default: it appears only while logging is switched on in Settings > Logs.
    val logsFlow = remember { settingsRepo.flowLogConfig() }
    val logsEnabled by logsFlow.collectAsState(initial = settingsRepo.logConfig)
    // Customization: the name and icon in the title, the order of the cards and the hidden ones.
    val appearanceFlow = remember { settingsRepo.flowAppearance() }
    val appearance by appearanceFlow.collectAsState(initial = settingsRepo.appearance)
    val layoutFlow = remember { settingsRepo.flowHomeLayout() }
    val layout by layoutFlow.collectAsState(initial = settingsRepo.homeLayout)
    var editMode by remember { mutableStateOf(false) }

    // The cards that exist on this screen right now (some depend on the state of DhizukuF).
    val available = buildSet {
        add("dhizuku_state")
        if (owner) add("settings")
        if (owner) add("security")
        add("customization")
        if (owner) add("app_management")
        add("user_management")
        if (owner) add("dhizuku")
        if (logsEnabled.enabled) add("logs")
        add("dish")
        if (!owner) add("shizuku")
        if (!owner) add("adb")
        if (owner) add("home_deactivate_title")
    }
    // What is on the screen is kept here and changes at once; the saved copy follows. Two quick taps then both work on
    // the latest order, and the writes cannot overtake each other (only the newest state is saved).
    var order by remember { mutableStateOf(layout.order) }
    var hiddenKeys by remember { mutableStateOf(layout.hidden) }
    LaunchedEffect(layout) {
        if (!editMode) {
            order = layout.order
            hiddenKeys = layout.hidden
        }
    }
    LaunchedEffect(order, hiddenKeys, editMode) {
        if (order != layout.order || hiddenKeys != layout.hidden) {
            if (editMode) delay(300) // while editing, wait until the taps stop; on leaving the mode, save at once
            withContext(Dispatchers.IO) {
                settingsRepo.homeOrder = order
                settingsRepo.homeHidden = hiddenKeys
            }
        }
    }
    val arranged = remember(order) { HomeLayoutLogic.arrange(HOME_DEFAULT_ORDER, order) }

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(windowInsets.exclude(WindowInsetsSides.Bottom))
            .fillMaxSize(),
        contentWindowInsets = windowInsets.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconImage(
                            choice = appearance.icon,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(appearance.name.label)
                    }
                },
                actions = {
                    TopBarActions(editMode = editMode, onToggleEdit = { editMode = !editMode })
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
            if (editMode) {
                item("edit_hint") {
                    EditHint(onReset = {
                        order = HOME_DEFAULT_ORDER
                        hiddenKeys = emptySet()
                    })
                }
                val shown = arranged.filter { key -> key in available }
                itemsIndexed(shown, key = { _, key -> "edit_$key" }) { index, key ->
                    HomeEditRow(
                        title = stringResource(homeTitleRes(key)),
                        hidden = key in hiddenKeys,
                        canUp = index > 0,
                        canDown = index < shown.lastIndex,
                        onUp = { order = HomeLayoutLogic.move(arranged, available, key, -1) },
                        onDown = { order = HomeLayoutLogic.move(arranged, available, key, +1) },
                        onToggle = { hiddenKeys = HomeLayoutLogic.toggleHidden(hiddenKeys, key) }
                    )
                }
            } else {
                val visible = HomeLayoutLogic.visible(arranged, available, hiddenKeys)
                items(visible, key = { key -> key }) { key ->
                    HomeCard(key, navController)
                }
                if (visible.isEmpty()) {
                    item("all_hidden") { AllHiddenHint() }
                }
            }
        }
    }
}

/** The order of the cards when nothing was changed. Logs stays above Terminal (dish). */
private val HOME_DEFAULT_ORDER = listOf(
    "dhizuku_state", "settings", "security", "customization", "app_management", "user_management",
    "dhizuku", "logs", "dish", "shizuku", "adb", "home_deactivate_title"
)

private fun homeTitleRes(key: String): Int = when (key) {
    "dhizuku_state" -> R.string.home_edit_status
    "settings" -> R.string.settings
    "security" -> R.string.security
    "customization" -> R.string.customization
    "app_management" -> R.string.home_app_management_title
    "user_management" -> R.string.user_manager
    "dhizuku" -> R.string.home_dhizuku_title
    "logs" -> R.string.logs
    "dish" -> R.string.home_dish_title
    "shizuku" -> R.string.home_shizuku_title
    "adb" -> R.string.home_adb_title
    else -> R.string.home_deactivate_title
}

@Composable
private fun LazyItemScope.HomeCard(key: String, navController: NavController) {
    when (key) {
        "dhizuku_state" -> DhizukuStateWidget()
        "settings" -> SettingsWidget(navController)
        "security" -> SecurityWidget(navController)
        "customization" -> CustomizationWidget(navController)
        "app_management" -> AppManagementWidget(navController)
        "user_management" -> UserManagementWidget(navController)
        "dhizuku" -> DhizukuWidget(navController)
        "logs" -> LogsWidget(navController)
        "dish" -> DishWidget()
        "shizuku" -> ShizukuWidget(navController)
        "adb" -> AdbWidget()
        "home_deactivate_title" -> DeactivateWidget()
    }
}

@Composable
private fun LazyItemScope.CustomizationWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.Customization.route)
    }, icon = {
        Icon(imageVector = Icons.TwoTone.Palette, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.customization))
    }, text = {
        Text(stringResource(R.string.customization_desc))
    })
}

/** One card in the editing mode: no navigation here, only the order and the visibility. */
@Composable
private fun LazyItemScope.HomeEditRow(
    title: String,
    hidden: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onToggle: () -> Unit
) {
    ElevatedCard(modifier = Modifier
        .fillMaxWidth()
        .animateItem()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (hidden) 0.45f else 1f)
            )
            TvIconButton(onClick = onUp, enabled = canUp) {
                Icon(Icons.TwoTone.ArrowUpward, contentDescription = stringResource(R.string.home_edit_move_up))
            }
            TvIconButton(onClick = onDown, enabled = canDown) {
                Icon(Icons.TwoTone.ArrowDownward, contentDescription = stringResource(R.string.home_edit_move_down))
            }
            TvIconButton(onClick = onToggle) {
                Icon(
                    if (hidden) Icons.TwoTone.VisibilityOff else Icons.TwoTone.Visibility,
                    contentDescription = stringResource(if (hidden) R.string.home_edit_show else R.string.home_edit_hide)
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.EditHint(onReset: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.home_edit_hint), style = MaterialTheme.typography.bodyMedium)
            TvOutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text(stringResource(R.string.home_edit_reset))
            }
        }
    }
}

@Composable
private fun LazyItemScope.AllHiddenHint() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.home_edit_all_hidden),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarActions(editMode: Boolean, onToggleEdit: () -> Unit) {
    var shutdownDialogShow by remember { mutableStateOf(false) }
    // The pencil: edit the home screen (order of the cards, hide / show them). It is left of the X.
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(stringResource(R.string.home_edit_title)) } },
        state = rememberTooltipState()
    ) {
        TvIconButton(onClick = onToggleEdit) {
            Icon(
                if (editMode) Icons.TwoTone.Check else Icons.TwoTone.Edit,
                contentDescription = stringResource(R.string.home_edit_title)
            )
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(stringResource(R.string.home_shutdown_title)) } },
        state = rememberTooltipState()
    ) {
        TvIconButton(onClick = { shutdownDialogShow = true }) {
            Icon(Icons.TwoTone.Cancel, contentDescription = stringResource(R.string.home_shutdown_title))
        }
    }

    if (shutdownDialogShow) {
        AlertDialog(onDismissRequest = {
            shutdownDialogShow = false
        }, confirmButton = {
            TvTextButton(onClick = {
                shutdownDialogShow = false
            }) {
                Text(stringResource(R.string.cancel))
            }
            TvTextButton(onClick = {
                exitProcess(0)
            }) {
                Text(stringResource(R.string.confirm))
            }
        }, title = {
            Text(stringResource(R.string.home_shutdown_title))
        }, text = {
            Text(stringResource(R.string.home_shutdown_dsp))
        })
    }
}

@Composable
private fun LazyItemScope.DhizukuStateWidget() {
    val isOwner = DhizukuState.state.isOwner

    @Suppress("AnimateAsStateLabel")
    val iconContainerColor by animateColorAsState(
        if (isOwner) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer
    )

    @Suppress("AnimateAsStateLabel")
    val iconColor by animateColorAsState(
        if (isOwner) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onErrorContainer
    )
    val context = LocalContext.current
    CardWidget(colors = CardDefaults.cardColors(
        containerColor = iconContainerColor
    ), icon = {
        Icon(
            imageVector = Icons.TwoTone.run {
                if (isOwner) SentimentVerySatisfied
                else SentimentVeryDissatisfied
            },
            contentDescription = null
        )
    }, iconColors = IconButtonDefaults.iconButtonColors(
        containerColor = iconContainerColor,
        contentColor = iconColor
    ), title = {
        @Suppress("AnimatedContentLabel")
        AnimatedContent(targetState = isOwner) {
            Text(
                if (it) stringResource(
                    R.string.home_status_owner_granted,
                    stringResource(
                        if (DhizukuState.state.isProfileOwner) R.string.confirm_profile_owner
                        else R.string.confirm_device_owner
                    )
                )
                else stringResource(R.string.home_status_owner_denied)
            )
        }
    }, onClick = { DhizukuState.sync(context )})
}

@Composable
private fun LazyItemScope.SettingsWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.Settings.route)
    }, icon = {
        Icon(imageVector = Icons.TwoTone.Settings, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.settings))
    }, text = {
        Text(stringResource(R.string.settings_desc))
    })
}

@Composable
private fun LazyItemScope.LogsWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.Logs.route)
    }, icon = {
        Icon(imageVector = Icons.TwoTone.Description, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.logs))
    }, text = {
        Text(stringResource(R.string.logs_desc))
    })
}

@Composable
private fun LazyItemScope.SecurityWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.Security.route)
    }, icon = {
        Icon(imageVector = Icons.TwoTone.Security, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.security))
    }, text = {
        Text(stringResource(R.string.security_desc))
    })
}

@Composable
private fun LazyItemScope.AppManagementWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.AppManagement.route)
    }, icon = {
        Icon(imageVector = Icons.TwoTone.RoomPreferences, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.home_app_management_title))
    }, text = {
        Text(stringResource(R.string.home_app_management_dsp))
    })
}

@Composable
private fun LazyItemScope.UserManagementWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.UserManagement.route)
    }, icon = {
        Icon(imageVector = Icons.TwoTone.SupervisorAccount, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.user_manager))
    }, text = {
        Text(stringResource(R.string.home_user_management_dsp))
    })
}

@Composable
private fun LazyItemScope.DhizukuWidget(navController: NavController) {
    CardWidget(onClick = {
        navController.navigate(SettingsRoute.Activate.route(SettingsRoute.Activate.Mode.Dhizuku))
    }, icon = {
        Icon(imageVector = Icons.TwoTone.SwapHorizontalCircle, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.home_dhizuku_title))
    }, text = {
        HtmlText(stringResource(R.string.home_dhizuku_dsp, "https://github.com/iamr0s/Dhizuku"))
    })
}

@Composable
private fun LazyItemScope.ShizukuWidget(navController: NavController) {
    CardWidget(onClick = {
        if (checkShizukuWorked()) {
            navController.navigate(SettingsRoute.Activate.route(SettingsRoute.Activate.Mode.Shizuku))
        } else {
            return@CardWidget
        }
    }, icon = {
        Icon(imageVector = Icons.TwoTone.Terminal, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.home_shizuku_title))
    }, text = {
        HtmlText(stringResource(R.string.home_shizuku_dsp, "https://shizuku.rikka.app/"))
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LazyItemScope.DishWidget() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var doneDialogShow by remember { mutableStateOf(false) }
    var helpDialogShow by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val success = try {
                DishExporter.export(context, uri)
                true
            } catch (e: Exception) {
                Log.w("Dish", "export failed", e)
                false
            }
            if (success) {
                exportPath = null
                doneDialogShow = true
            } else context.toast(R.string.dish_export_failed)
        }
    }

    // Many Android TV boxes have no folder picker: fall back to the app's own folder.
    val startExport = {
        try {
            picker.launch(null)
        } catch (e: ActivityNotFoundException) {
            scope.launch(Dispatchers.IO) {
                try {
                    exportPath = DishExporter.exportToAppFolder(context).absolutePath
                    doneDialogShow = true
                } catch (e2: Exception) {
                    Log.w("Dish", "fallback export failed", e2)
                    context.toast(R.string.dish_export_failed)
                }
            }
        }
    }

    CardWidget(onClick = { helpDialogShow = true }, focusable = false, icon = {
        Icon(imageVector = Icons.TwoTone.Terminal, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.home_dish_title))
    }, content = {
        Text(stringResource(R.string.home_dish_dsp))
        val ownerState = DhizukuState.state
        if (!ownerState.isOwner) {
            Text(
                stringResource(R.string.home_dish_need_owner),
                color = MaterialTheme.colorScheme.error
            )
        } else if (!ownerState.isDeviceOwner) {
            Text(stringResource(R.string.home_dish_profile_owner_note))
        }
        TvTextButton(onClick = { startExport() }) {
            Icon(imageVector = Icons.TwoTone.Code, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.home_dish_btn_export))
        }
        TvTextButton(onClick = { helpDialogShow = true }) {
            Icon(imageVector = Icons.TwoTone.Info, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.home_dish_help_title))
        }
    })

    if (helpDialogShow) {
        AlertDialog(onDismissRequest = { helpDialogShow = false }, confirmButton = {
            TvTextButton(onClick = {
                helpDialogShow = false
                startExport()
            }) {
                Text(stringResource(R.string.home_dish_btn_export))
            }
        }, dismissButton = {
            TvTextButton(onClick = { helpDialogShow = false }) {
                Text(stringResource(R.string.confirm))
            }
        }, title = {
            Text(stringResource(R.string.home_dish_help_title))
        }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.home_dish_help_text))
            }
        })
    }

    if (doneDialogShow) {
        AlertDialog(onDismissRequest = { doneDialogShow = false }, confirmButton = {
            TvTextButton(onClick = { doneDialogShow = false }) {
                Text(stringResource(R.string.confirm))
            }
        }, title = {
            Text(stringResource(R.string.dish_export_done_title))
        }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.dish_export_done_dsp))
                exportPath?.let {
                    Text(
                        it,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        })
    }
}

@Composable
private fun LazyItemScope.AdbWidget() {
    val deviceOwnerCommand =
        "adb shell dpm set-device-owner ${DhizukuState.admin.flattenToShortString()}"
    val profileOwnerCommand =
        "adb shell dpm set-profile-owner ${DhizukuState.admin.flattenToShortString()}"

    var commandDialogShow by remember { mutableStateOf(false) }
    var useProfileOwner by remember { mutableStateOf(false) }
    var ownerMenuExpanded by remember { mutableStateOf(false) }
    var profileOwnerWarningShow by remember { mutableStateOf(false) }

    CardWidget(icon = {
        Icon(imageVector = Icons.TwoTone.Adb, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.home_adb_title))
    }, content = {
        HtmlText(
            stringResource(
                R.string.home_adb_dsp,
                "https://developer.android.com/tools/adb"
            )
        )
        TvTextButton(onClick = { commandDialogShow = true }) {
            Icon(imageVector = Icons.TwoTone.Code, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.home_adb_btn_view_command))
        }
    })

    if (profileOwnerWarningShow) {
        AlertDialog(onDismissRequest = {
            profileOwnerWarningShow = false
        }, icon = {
            Icon(imageVector = Icons.TwoTone.Warning, contentDescription = null)
        }, confirmButton = {
            TvTextButton(onClick = {
                useProfileOwner = true
                profileOwnerWarningShow = false
            }) {
                Text(stringResource(R.string.confirm))
            }
        }, dismissButton = {
            TvTextButton(onClick = { profileOwnerWarningShow = false }) {
                Text(stringResource(R.string.cancel))
            }
        }, title = {
            Text(stringResource(R.string.profile_owner_warning_title))
        }, text = {
            Text(stringResource(R.string.profile_owner_warning_dsp))
        })
    }

    if (!commandDialogShow) return
    val command = if (useProfileOwner) profileOwnerCommand else deviceOwnerCommand
    AlertDialog(onDismissRequest = {
        commandDialogShow = false
    }, confirmButton = {
        TvTextButton(onClick = {
            commandDialogShow = false
        }) {
            Text(stringResource(R.string.cancel))
        }
        val manager = LocalClipboard.current
        val scope = rememberCoroutineScope()
        TvTextButton(onClick = {
            scope.launch {
                manager.setClipEntry(ClipEntry(ClipData.newPlainText("command", command)))
                commandDialogShow = false
            }
        }) {
            Text(stringResource(R.string.copy))
        }
    }, title = {
        Text(stringResource(R.string.home_adb_btn_view_command))
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box {
                TvOutlinedButton(onClick = { ownerMenuExpanded = true }) {
                    Text(
                        stringResource(
                            if (useProfileOwner) R.string.confirm_profile_owner
                            else R.string.confirm_device_owner
                        )
                    )
                    Icon(imageVector = Icons.TwoTone.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = ownerMenuExpanded,
                    onDismissRequest = { ownerMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.confirm_device_owner)) },
                        onClick = {
                            useProfileOwner = false
                            ownerMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.confirm_profile_owner)) },
                        onClick = {
                            ownerMenuExpanded = false
                            profileOwnerWarningShow = true
                        }
                    )
                }
            }
            SelectionContainer {
                Text(command)
            }
        }
    })
}

@Composable
private fun LazyItemScope.DeactivateWidget() {
    val context = LocalContext.current
    val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    var state by remember {
        mutableStateOf(false)
    }
    CardWidget(onClick = {
        state = true
    }, icon = {
        Icon(imageVector = Icons.TwoTone.DoNotDisturbOn, contentDescription = null)
    }, title = {
        Text(stringResource(R.string.home_deactivate_title))
    })
    if (!state) return
    AlertDialog(onDismissRequest = {
        state = false
    }, confirmButton = {
        TvTextButton(onClick = {
            state = false
        }) {
            Text(stringResource(R.string.cancel))
        }
        TvTextButton(onClick = {
            try {
                @Suppress("DEPRECATION")
                if (DhizukuState.state.isProfileOwner) {
                    manager.clearProfileOwner(DhizukuState.admin)
                } else {
                    manager.clearDeviceOwnerApp(context.packageName)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            state = false
            DhizukuState.sync(context)
        }) {
            Text(stringResource(R.string.confirm))
        }
    }, title = {
        Text(stringResource(R.string.home_deactivate_title))
    }, text = {
        Text(stringResource(R.string.home_deactivate_dsp))
    })
}

@Composable
private fun HtmlText(text: String) {
    val linkColor = MaterialTheme.colorScheme.primary
    // Parsing HTML is expensive: do it once per (text, color) instead of on every recomposition.
    val annotated = remember(text, linkColor) {
        AnnotatedString.fromHtml(
            text,
            TextLinkStyles(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            )
        )
    }
    // A device without a browser (many TV boxes) must not crash when a link is opened.
    val base = LocalUriHandler.current
    val safe = remember(base) {
        object : UriHandler {
            override fun openUri(uri: String) {
                try {
                    base.openUri(uri)
                } catch (_: Exception) {
                    // nothing can open the link here
                }
            }
        }
    }
    CompositionLocalProvider(LocalUriHandler provides safe) {
        Text(annotated)
    }
}

@Composable
private fun LazyItemScope.CardWidget(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    // null = the card is not an action itself (it only holds text and buttons)
    onClick: (() -> Unit)? = null,
    // false = the card still reacts to touch, but the D-pad skips it and goes to its buttons
    focusable: Boolean = true,
    icon: @Composable () -> Unit,
    iconColors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    title: @Composable () -> Unit,
    text: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(40.dp),
                    color = iconColors.containerColor,
                    contentColor = iconColors.contentColor
                ) {
                    Box(Modifier.padding(8.dp)) {
                        icon.invoke()
                    }
                }
                Column {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        title.invoke()
                    }
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        @Suppress("AnimatedContentLabel")
                        AnimatedContent(targetState = text) {
                            it?.invoke()
                        }
                    }
                }
            }
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                content?.invoke()
            }
        }
    }
    val base = modifier.animateItem().then(appearModifier())
    if (onClick == null) {
        // A card without an action is not a focus target, so the buttons inside stay reachable.
        ElevatedCard(modifier = base, colors = colors, content = body)
    } else {
        ElevatedCard(
            modifier = base.then(
                if (focusable) Modifier.tvFocusBorder(RoundedCornerShape(20.dp))
                else Modifier.focusProperties { canFocus = false }
            ),
            colors = colors,
            onClick = onClick,
            content = body
        )
    }
}
