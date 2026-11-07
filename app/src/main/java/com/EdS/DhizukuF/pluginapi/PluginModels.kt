package com.EdS.dhizukuf.pluginapi

/**
 * Entry that appears on Home or in Settings.
 *
 * - If [runAction] is true, the host calls [DhizukuPlugin.onSettingsEntryClick] first.
 * - If that returns false (or [runAction] is false), the host navigates to [route].
 */
data class PluginSettingsEntry(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val route: String,
    val order: Int = 100,
    val iconResName: String? = null,
    /** When true, host prefers [DhizukuPlugin.onSettingsEntryClick] over navigation. */
    val runAction: Boolean = false
)

/**
 * Activation method shown on the home / activate screen.
 */
data class PluginActivationMethod(
    val id: String,
    val title: String,
    val description: String,
    val order: Int = 100,
    val onActivate: () -> Unit
)

/**
 * Extra dangerous command exposed by the plugin.
 */
data class PluginDangerousCommand(
    val id: String,
    val name: String,
    val description: String,
    val requiresDeviceOwner: Boolean = true
)

/**
 * Factory that creates a UI page (Compose or View).
 * The host decides how to host it.
 */
fun interface PluginPageFactory {
    fun create(context: PluginContext): Any
}

/**
 * Parsed content of plugin.json
 */
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val description: String = "",
    val author: String = "",
    val minDhizukuF: Int = 1,
    val entryClass: String,
    val autoStart: Boolean = false,
    val requestsDangerousCommands: Boolean = false,
    val permissions: List<String> = emptyList()
)

enum class PluginState {
    INSTALLED,
    ENABLED,
    DISABLED,
    STOPPED,
    ERROR
}

data class PluginInfo(
    val manifest: PluginManifest,
    val state: PluginState,
    val autoStart: Boolean,
    val installPath: String,
    val errorMessage: String? = null,
    val whatItReceives: List<String> = emptyList()
)
