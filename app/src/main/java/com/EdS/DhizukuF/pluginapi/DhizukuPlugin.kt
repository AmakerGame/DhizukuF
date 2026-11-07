package com.EdS.dhizukuf.pluginapi

/**
 * Main entry point that every DhizukuF plugin must implement.
 *
 * The class named in `plugin.json` → `entryClass` must have a public
 * no-arg constructor and implement this interface.
 */
interface DhizukuPlugin {

    /** Unique stable ID (usually reverse-domain). Must match plugin.json `id`. */
    val id: String

    val name: String
    val version: String
    val description: String
        get() = ""

    /** Lower number = higher priority in lists. Default 100. */
    val priority: Int
        get() = 100

    /**
     * Called once when the plugin is loaded (after install or on app start
     * if auto-start / previously enabled).
     */
    fun onLoad(context: PluginContext) {}

    /**
     * Called when the plugin is unloaded (disabled, stopped or deleted).
     */
    fun onUnload(context: PluginContext) {}

    /**
     * Called when DhizukuF becomes or loses Device/Profile Owner.
     */
    fun onOwnerStateChanged(context: PluginContext, isOwner: Boolean) {}

    /**
     * Provide entries that appear on Home / Settings navigation.
     * Host shows them as soon as the plugin is ENABLED.
     */
    fun provideSettingsEntries(context: PluginContext): List<PluginSettingsEntry> = emptyList()

    /**
     * Called when the user taps a [PluginSettingsEntry] from this plugin.
     *
     * @return true if the click was handled (host will not navigate to [PluginSettingsEntry.route]).
     *         Use this to run code (toggle, command, toast, etc.) without opening a page.
     */
    fun onSettingsEntryClick(context: PluginContext, entryId: String): Boolean = false

    /**
     * Provide activation methods shown on the Home / Activate screen.
     */
    fun provideActivationMethods(context: PluginContext): List<PluginActivationMethod> = emptyList()

    /**
     * Provide extra dangerous commands (only used when the global
     * “Dangerous commands” switch is ON).
     */
    fun provideDangerousCommands(context: PluginContext): List<PluginDangerousCommand> = emptyList()

    /**
     * Optional custom pages that can be navigated to from entries.
     * Key = route id, Value = page factory.
     */
    fun providePages(context: PluginContext): Map<String, PluginPageFactory> = emptyMap()
}
