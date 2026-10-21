package com.EdS.DhizukuF.customization

/** Settings > Customization > Theme. */
enum class ThemeMode {
    /** Follows the system (default). */
    System,
    Light,
    Dark,

    /** Dark with a pure black background: saves power and looks deeper on an AMOLED screen. */
    Amoled
}

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.System,
    /** Colours of the wallpaper (Android 12+). */
    val dynamicColors: Boolean = true
)

/** The icon that is chosen. [Default] and [Original] are built in and can never be removed. */
sealed interface IconChoice {
    val key: String

    /** The DhizukuF icon. */
    data object Default : IconChoice {
        override val key = "default"
    }

    /** The icon of the original Dhizuku. */
    data object Original : IconChoice {
        override val key = "original"
    }

    /** An icon of the user (a picture that was added in Customization). */
    data class Custom(val id: String) : IconChoice {
        override val key = "custom:$id"
    }

    companion object {
        fun parse(key: String?): IconChoice = when {
            key == null || key == Default.key -> Default
            key == Original.key -> Original
            key.startsWith("custom:") && CustomIcons.isValidId(key.removePrefix("custom:")) ->
                Custom(key.removePrefix("custom:"))

            else -> Default
        }
    }
}

object CustomIcons {
    /** How many pictures of the user are kept. */
    const val MAX = 8

    private val ID = Regex("[a-z0-9]{6,32}")

    fun isValidId(id: String): Boolean = ID.matches(id)

    fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
}

data class AppearanceConfig(
    /** The name as it was typed. Shown in DhizukuF and on the launcher shortcut of a custom icon; blank = "DhizukuF". */
    val displayName: String = "",
    val icon: IconChoice = IconChoice.Default,
    /** Keep the name and the icon when DhizukuF is updated (beta to release and so on). */
    val rememberForUpdates: Boolean = true,
    val customIcons: List<String> = emptyList()
) {
    /** The name to show, or null when it is blank (then the default name is used). */
    val shownName: String? get() = displayName.trim().ifEmpty { null }

    /** A selected custom icon that was deleted must never stay selected. */
    val effectiveIcon: IconChoice
        get() = (icon as? IconChoice.Custom)?.takeIf { it.id in customIcons } ?: icon.takeUnless { it is IconChoice.Custom }
            ?: IconChoice.Default
}

/** The order of the cards of the home screen and which of them are hidden (Home > pencil). */
data class HomeLayout(
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet()
)

object HomeLayoutLogic {
    /**
     * The saved order, completed with the cards that it does not know (a card that a new version added is put
     * after the card that precedes it in the default order) and cleaned of cards that no longer exist.
     */
    fun arrange(defaultKeys: List<String>, saved: List<String>): List<String> {
        val result = saved.filter { it in defaultKeys }.distinct().toMutableList()
        for ((i, key) in defaultKeys.withIndex()) {
            if (key in result) continue
            val previous = defaultKeys.subList(0, i).lastOrNull { it in result }
            result.add(if (previous == null) 0 else result.indexOf(previous) + 1, key)
        }
        return result
    }

    /**
     * Moves [key] one place up (-1) or down (+1) among the cards that are on the screen right now ([available]);
     * cards that are not shown at the moment (for example Shizuku while DhizukuF is the owner) keep their places.
     */
    fun move(arranged: List<String>, available: Set<String>, key: String, delta: Int): List<String> {
        val shown = arranged.filter { it in available }
        val from = shown.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in shown.indices) return arranged
        val result = arranged.toMutableList()
        val a = result.indexOf(key)
        val b = result.indexOf(shown[to])
        result[a] = shown[to]
        result[b] = key
        return result
    }

    fun toggleHidden(hidden: Set<String>, key: String): Set<String> =
        if (key in hidden) hidden - key else hidden + key

    /** What the screen shows: arranged, available and not hidden. */
    fun visible(arranged: List<String>, available: Set<String>, hidden: Set<String>): List<String> =
        arranged.filter { it in available && it !in hidden }
}

/** What to do at start-up so that the name and the icon survive an update (or are given up, as the user asked). */
data class RestorePlan(
    /** "Remember for updates" is off and DhizukuF was updated: name and icon go back to the default ones. */
    val resetToDefault: Boolean,
    /** The built-in launcher entry that must be switched on, or null when the right one is on already. */
    val switchTo: IconChoice?,
    /** Remember that the name and icon belong to this version now. */
    val storeVersion: Boolean
)

object RestoreLogic {
    fun plan(config: AppearanceConfig, storedVersion: Int, currentVersion: Int, inUse: IconChoice): RestorePlan {
        val updated = storedVersion != currentVersion
        // storedVersion 0 = nothing was ever put in place (a fresh install): there is nothing to give up
        if (updated && !config.rememberForUpdates && storedVersion != 0) {
            return RestorePlan(
                resetToDefault = true,
                switchTo = IconChoice.Default.takeIf { inUse != IconChoice.Default },
                storeVersion = true
            )
        }
        if (!config.rememberForUpdates) return RestorePlan(false, null, updated)
        // the launcher entry is the original icon only when that was chosen; a custom icon is a shortcut and the
        // normal entry is then the DhizukuF icon
        val wanted = if (config.effectiveIcon == IconChoice.Original) IconChoice.Original else IconChoice.Default
        return RestorePlan(false, wanted.takeIf { it != inUse }, updated)
    }
}
