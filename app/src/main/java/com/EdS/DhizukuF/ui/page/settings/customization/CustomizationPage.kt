package com.EdS.DhizukuF.ui.page.settings.customization

import android.os.Build
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavController
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.customization.AppName
import com.EdS.DhizukuF.customization.AppearanceManager
import com.EdS.DhizukuF.customization.IconChoice
import com.EdS.DhizukuF.customization.ThemeMode
import com.EdS.DhizukuF.data.common.util.toast
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.SwitchRow
import com.EdS.DhizukuF.ui.widget.TvIconButton
import com.EdS.DhizukuF.ui.widget.TvRadioRow
import com.EdS.DhizukuF.ui.widget.rememberIsTv
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
    val isTv = rememberIsTv()
    // The launcher entry that is really switched on (read from the system, not from the saved choice).
    var entryInUse by remember { mutableStateOf(AppearanceManager.inUse(context)) }

    fun report(switched: Boolean) {
        entryInUse = AppearanceManager.inUse(context)
        context.toast(if (switched) R.string.customization_changed else R.string.customization_apply_failed)
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
                    // Ready-made names: the launcher entries of the manifest. The icon that is chosen stays as it is.
                    AppName.entries.forEach { name ->
                        TvRadioRow(
                            title = name.label,
                            selected = appearance.name == name,
                            onClick = { viewModel.chooseName(context, name, ::report) }
                        )
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
                    if (isTv) {
                        Text(
                            stringResource(R.string.customization_tv_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    // The two built-in icons: always there, they cannot be deleted. The name that is chosen stays as it is.
                    IconRow(
                        icon = IconChoice.Default,
                        name = appearance.name,
                        title = stringResource(R.string.icon_default_title),
                        description = stringResource(R.string.icon_default_desc),
                        selected = appearance.icon == IconChoice.Default,
                        tvBanner = isTv,
                        onClick = { viewModel.chooseIcon(context, IconChoice.Default, ::report) }
                    )
                    IconRow(
                        icon = IconChoice.Original,
                        name = appearance.name,
                        title = stringResource(R.string.icon_original_title),
                        description = stringResource(R.string.icon_original_desc),
                        selected = appearance.icon == IconChoice.Original,
                        tvBanner = isTv,
                        onClick = { viewModel.chooseIcon(context, IconChoice.Original, ::report) }
                    )
                    IconRow(
                        icon = IconChoice.Material3,
                        name = appearance.name,
                        title = stringResource(R.string.icon_material3_title),
                        description = stringResource(R.string.icon_material3_desc),
                        selected = appearance.icon == IconChoice.Material3,
                        tvBanner = isTv,
                        onClick = { viewModel.chooseIcon(context, IconChoice.Material3, ::report) }
                    )
                    // What the system really has switched on: tells at once whether the switch worked.
                    entryInUse?.let { entry ->
                        Text(
                            stringResource(
                                R.string.customization_entry_now,
                                entry.name.label,
                                stringResource(
                                    when (entry.icon) {
                                        IconChoice.Original -> R.string.icon_original_title
                                        IconChoice.Material3 -> R.string.icon_material3_title
                                        IconChoice.Default -> R.string.icon_default_title
                                    }
                                )
                            ),
                            style = MaterialTheme.typography.labelLarge
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

/**
 * An icon of the list: a normal D-pad target. On a TV it shows the banner that a TV launcher would show for this icon
 * together with the name that is chosen.
 */
@Composable
private fun IconRow(
    icon: IconChoice,
    name: AppName,
    title: String,
    description: String,
    selected: Boolean,
    tvBanner: Boolean,
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
        if (tvBanner) {
            TvBannerImage(
                name = name,
                icon = icon,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(width = 128.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            AppIconImage(
                choice = icon,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
