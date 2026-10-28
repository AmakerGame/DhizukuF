package com.EdS.DhizukuF.ui.theme

import org.koin.compose.koinInject

import com.EdS.DhizukuF.data.settings.repo.SettingsRepo

import com.EdS.DhizukuF.customization.ThemeMode

import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.ColorScheme

import androidx.compose.runtime.remember

import androidx.compose.runtime.getValue

import androidx.compose.runtime.collectAsState

import android.graphics.drawable.ColorDrawable

import android.app.Activity
import android.os.Build

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Settings > Customization > Theme: System (default), Light, Dark or AMOLED (dark with a pure black background).
 * The choice is read here, so it changes at once when it is changed in Customization, in every screen and dialog.
 */
@Composable
fun DhizukuTheme(
    content: @Composable () -> Unit
) {
    val settingsRepo = koinInject<SettingsRepo>()
    val flow = remember { settingsRepo.flowThemeConfig() }
    val config by flow.collectAsState(initial = settingsRepo.themeConfig)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (config.mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Amoled -> true
    }
    val amoled = config.mode == ThemeMode.Amoled

    val base = when {
        config.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = if (amoled) base.toAmoled() else base

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)

            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            // The window itself: no light flash behind the first frame (black on AMOLED). A translucent window (the
            // permission dialog is drawn over other apps) must stay transparent.
            if (!activity.isTranslucentWindow()) {
                window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * The dark scheme with a black background. Cards stay a little lighter than the background, so that they can still
 * be told apart; the colours of the accent (primary and so on) are not touched.
 */
private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF242424),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFF1A1A1A),
    inverseOnSurface = Color.Black
)

private fun Activity.isTranslucentWindow(): Boolean {
    val attrs = obtainStyledAttributes(intArrayOf(android.R.attr.windowIsTranslucent))
    return try {
        attrs.getBoolean(0, false)
    } finally {
        attrs.recycle()
    }
}
