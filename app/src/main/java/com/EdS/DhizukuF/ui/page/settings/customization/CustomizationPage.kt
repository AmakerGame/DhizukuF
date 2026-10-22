package com.EdS.DhizukuF.ui.page.settings.customization

import com.EdS.DhizukuF.ui.widget.tvTextFieldNavigation

import com.EdS.DhizukuF.ui.widget.rememberIsTv

import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.focus.FocusDirection

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.text.KeyboardActions

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.navigation.NavController
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.customization.AppearanceManager
import com.EdS.DhizukuF.customization.CustomIcons
import com.EdS.DhizukuF.customization.IconChoice
import com.EdS.DhizukuF.customization.ThemeMode
import com.EdS.DhizukuF.data.common.util.toast
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.SwitchRow
import com.EdS.DhizukuF.ui.widget.TvButton
import com.EdS.DhizukuF.ui.widget.TvIconButton
import com.EdS.DhizukuF.ui.widget.TvRadioRow
import com.EdS.DhizukuF.ui.widget.TvTextButton
import com.EdS.DhizukuF.ui.widget.tvFocusBorder
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationPage(
    windowInsets: WindowInsets,
    navController: NavController,
    viewModel: CustomizationViewModel = koinViewModel()
) {
    LaunchedEffect(true) {
        viewModel.collect()
    }
    val context = LocalContext.current
    val theme = viewModel.theme
    val appearance = viewModel.appearance
    val pinSupported = remember { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }
    val isTv = rememberIsTv()
    // The launcher entry that is really switched on (read from the system, not from the saved choice).
    var entryInUse by remember { mutableStateOf(AppearanceManager.builtInInUse(context)) }
    val focusManager = LocalFocusManager.current
    // The text field keeps its own text once the user types: the saved value must not overwrite it half way.
    var typedName by remember { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.addIcon(context, uri) { id ->
                context.toast(if (id != null) R.string.icon_added else R.string.icon_add_failed)
            }
        }
    }

    askDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { askDelete = null },
            title = { Text(stringResource(R.string.icon_delete_title)) },
            text = { Text(stringResource(R.string.icon_delete_text)) },
            confirmButton = {
                TvTextButton(onClick = {
                    askDelete = null
                    viewModel.deleteCustom(context, id, context.getString(R.string.icon_deleted_shortcut))
                }) {
                    Text(stringResource(R.string.icon_delete))
                }
            },
            dismissButton = {
                TvTextButton(onClick = { askDelete = null }) {
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
                title = { Text(stringResource(R.string.customization)) }
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
            // ------------------------------------------------------------ theme
            item {
                SectionCard(R.string.customization_theme_title) {
                    TvRadioRow(
                        title = stringResource(R.string.theme_system),
                        description = stringResource(R.string.theme_system_desc),
                        selected = theme.mode == ThemeMode.System,
                        onClick = { viewModel.changeThemeMode(ThemeMode.System) }
                    )
                    TvRadioRow(
                        title = stringResource(R.string.theme_light),
                        selected = theme.mode == ThemeMode.Light,
                        onClick = { viewModel.changeThemeMode(ThemeMode.Light) }
                    )
                    TvRadioRow(
                        title = stringResource(R.string.theme_dark),
                        selected = theme.mode == ThemeMode.Dark,
                        onClick = { viewModel.changeThemeMode(ThemeMode.Dark) }
                    )
                    TvRadioRow(
                        title = stringResource(R.string.theme_amoled),
                        description = stringResource(R.string.theme_amoled_desc),
                        selected = theme.mode == ThemeMode.Amoled,
                        onClick = { viewModel.changeThemeMode(ThemeMode.Amoled) }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SwitchRow(
                            title = stringResource(R.string.customization_dynamic),
                            description = stringResource(R.string.customization_dynamic_desc),
                            checked = theme.dynamicColors,
                            onCheckedChange = viewModel::changeDynamicColors
                        )
                    }
                }
            }

            // ------------------------------------------------------------ name
            item {
                SectionCard(R.string.customization_name_title) {
                    Text(
                        stringResource(R.string.customization_name_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = typedName ?: appearance.displayName,
                        onValueChange = { value ->
                            val text = value.take(40)
                            typedName = text
                            viewModel.changeName(text)
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.customization_name_label)) },
                        placeholder = { Text(stringResource(R.string.app_name)) },
                        // Done on the keyboard leaves the field; so do Up and Down of a remote
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvTextFieldNavigation()
                    )
                    if ((typedName ?: appearance.displayName).isNotEmpty()) {
                        TvTextButton(onClick = {
                            typedName = ""
                            viewModel.changeName("")
                        }) {
                            Text(stringResource(R.string.customization_name_reset))
                        }
                    }
                }
            }

            // ------------------------------------------------------------ icon
            item {
                SectionCard(R.string.customization_icon_title) {
                    Text(
                        stringResource(R.string.customization_icon_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val selected = appearance.effectiveIcon
                    Text(
                        stringResource(
                            R.string.customization_entry_now,
                            stringResource(if (entryInUse == IconChoice.Original) R.string.app_name_original else R.string.app_name)
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (isTv) {
                        Text(
                            stringResource(R.string.customization_tv_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // The two built-in icons: always there, they cannot be deleted.
                    IconRow(
                        icon = IconChoice.Default,
                        title = stringResource(R.string.icon_default_title),
                        description = stringResource(R.string.icon_default_desc),
                        selected = selected == IconChoice.Default,
                        tvBanner = isTv,
                        onClick = {
                            viewModel.chooseBuiltIn(context, IconChoice.Default) { switched ->
                                entryInUse = AppearanceManager.builtInInUse(context)
                                context.toast(if (switched) R.string.icon_changed else R.string.icon_apply_failed)
                            }
                        },
                        onDelete = null
                    )
                    IconRow(
                        icon = IconChoice.Original,
                        title = stringResource(R.string.icon_original_title),
                        description = stringResource(R.string.icon_original_desc),
                        selected = selected == IconChoice.Original,
                        tvBanner = isTv,
                        onClick = {
                            viewModel.chooseBuiltIn(context, IconChoice.Original) { switched ->
                                entryInUse = AppearanceManager.builtInInUse(context)
                                context.toast(if (switched) R.string.icon_changed else R.string.icon_apply_failed)
                            }
                        },
                        onDelete = null
                    )
                    // The icons of the user: can be chosen and deleted.
                    appearance.customIcons.forEachIndexed { index, id ->
                        IconRow(
                            icon = IconChoice.Custom(id),
                            title = stringResource(R.string.icon_custom_title, index + 1),
                            description = stringResource(R.string.icon_custom_desc),
                            selected = selected == IconChoice.Custom(id),
                            tvBanner = isTv,
                            onClick = {
                                viewModel.chooseCustom(context, id) { result ->
                                    context.toast(
                                        when (result) {
                                            AppearanceManager.PinResult.Requested -> R.string.icon_pin_requested
                                            AppearanceManager.PinResult.Unsupported -> R.string.icon_pin_unsupported
                                            AppearanceManager.PinResult.Failed -> R.string.icon_pin_failed
                                        }
                                    )
                                }
                            },
                            onDelete = { askDelete = id }
                        )
                    }
                    if (appearance.customIcons.size < CustomIcons.MAX) {
                        TvButton(
                            onClick = {
                                try {
                                    picker.launch("image/*")
                                } catch (_: ActivityNotFoundException) {
                                    context.toast(R.string.icon_picker_missing)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.icon_add))
                        }
                    } else {
                        Text(
                            stringResource(R.string.icon_add_limit, CustomIcons.MAX),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!pinSupported) {
                        Text(
                            stringResource(R.string.customization_no_pin_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ------------------------------------------------------------ remember
            item {
                SectionCard(null) {
                    SwitchRow(
                        title = stringResource(R.string.customization_remember),
                        description = stringResource(R.string.customization_remember_desc),
                        checked = appearance.rememberForUpdates,
                        onCheckedChange = viewModel::changeRemember
                    )
                }
            }
        }
    }
}

/** The card of the settings screens: the same look as in Settings, Security and Logs. */
@Composable
private fun SectionCard(title: Int?, content: @Composable () -> Unit) {
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

/** An icon of the list: a normal D-pad target (choose it), and a delete button next to the icons of the user. */
@Composable
private fun IconRow(
    icon: IconChoice,
    title: String,
    description: String,
    selected: Boolean,
    tvBanner: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .tvFocusBorder(RoundedCornerShape(12.dp))
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            // On a TV the TV launcher shows the wide banner, so the banner is what is shown here too.
            AppIconImage(
                choice = icon,
                tvBanner = tvBanner,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .then(
                        if (tvBanner && icon !is IconChoice.Custom) Modifier.size(width = 128.dp, height = 72.dp)
                        else Modifier.size(48.dp)
                    )
                    .clip(RoundedCornerShape(if (tvBanner && icon !is IconChoice.Custom) 8.dp else 12.dp))
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (onDelete != null) {
            TvIconButton(onClick = onDelete) {
                Icon(Icons.TwoTone.Delete, contentDescription = stringResource(R.string.icon_delete))
            }
        }
    }
}
