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

/** The name that the launcher shows for DhizukuF. The package name never changes. */
enum class AppName(val key: String, val label: String) {
    DhizukuF("dhizukuf", "DhizukuF"),
    Dhizuku("dhizuku", "Dhizuku"),
    ProfileOwner("profile_owner", "Profile Owner"),
    DeviceOwner("device_owner", "Device Owner");

    companion object {
        fun parse(key: String?): AppName = entries.firstOrNull { it.key == key } ?: DhizukuF
    }
}

/** The icon: the new one, or the icon of the original Dhizuku. Both are built in. */
enum class IconChoice(val key: String) {
    /** The DhizukuF icon (default). */
    Default("default"),

    /** The icon of the original Dhizuku. */
    Original("original");

    companion object {
        fun parse(key: String?): IconChoice = entries.firstOrNull { it.key == key } ?: Default
    }
}

/** One launcher entry of the manifest: a name together with an icon. Exactly one is switched on at a time. */
data class LauncherEntry(
    val name: AppName = AppName.DhizukuF,
    val icon: IconChoice = IconChoice.Default
) {
    companion object {
        val DEFAULT = LauncherEntry()
    }
}

/** The names of the launcher entries (`activity-alias`) of the manifest. */
object LauncherAliases {
    private const val PACKAGE = "com.EdS.DhizukuF"

    /** Every name with every icon: the name and the icon are chosen independently of each other. */
    val all: List<LauncherEntry> = AppName.entries.flatMap { name -> IconChoice.entries.map { LauncherEntry(name, it) } }

    /**
     * The default entry (DhizukuF, new icon) keeps the OLD name of the launcher activity: icons that earlier versions
     * left on the home screen point to that name and keep working after an update.
     */
    fun className(entry: LauncherEntry): String =
        if (entry == LauncherEntry.DEFAULT) "$PACKAGE.ui.activity.SettingsActivity"
        else "$PACKAGE.alias.${entry.name.name}${entry.icon.name}"
}

data class AppearanceConfig(
    val name: AppName = AppName.DhizukuF,
    val icon: IconChoice = IconChoice.Default,
    /** Keep the name and the icon when DhizukuF is updated (beta to release and so on). */
    val rememberForUpdates: Boolean = true
) {
    val entry: LauncherEntry get() = LauncherEntry(name, icon)
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
    /** The launcher entry that must be switched on, or null when the right one is on already. */
    val switchTo: LauncherEntry?,
    /** Remember that the name and icon belong to this version now. */
    val storeVersion: Boolean
)

object RestoreLogic {
    /** [inUse] is the entry that is switched on now, or null when it cannot be told. */
    fun plan(config: AppearanceConfig, storedVersion: Int, currentVersion: Int, inUse: LauncherEntry?): RestorePlan {
        val updated = storedVersion != currentVersion
        // storedVersion 0 = nothing was ever put in place (a fresh install): there is nothing to give up
        if (updated && !config.rememberForUpdates && storedVersion != 0) {
            return RestorePlan(
                resetToDefault = true,
                switchTo = LauncherEntry.DEFAULT.takeIf { inUse != it },
                storeVersion = true
            )
        }
        if (!config.rememberForUpdates) return RestorePlan(false, null, updated)
        return RestorePlan(false, config.entry.takeIf { it != inUse }, updated)
    }
}
