package com.example.helloplugin

import com.EdS.dhizukuf.pluginapi.DhizukuPlugin
import com.EdS.dhizukuf.pluginapi.PluginContext
import com.EdS.dhizukuf.pluginapi.PluginSettingsEntry

/**
 * Built-in sample plugin so "Hello Extension" zip (plugin.json only) can Enable
 * without shipping classes.dex. Real third-party plugins should ship classes.dex.
 */
class HelloPlugin : DhizukuPlugin {

    override val id: String = "com.example.helloplugin"
    override val name: String = "Hello Extension"
    override val version: String = "1.0.0"
    override val description: String =
        "Built-in sample plugin. Adds a settings entry when enabled."

    override fun onLoad(context: PluginContext) {
        context.putPreference("loaded_once", "true")
    }

    override fun provideSettingsEntries(context: PluginContext): List<PluginSettingsEntry> {
        return listOf(
            PluginSettingsEntry(
                id = "hello_settings",
                title = "Hello Extension",
                subtitle = "Sample settings entry from HelloPlugin",
                route = "plugin/hello/settings",
                order = 90
            )
        )
    }
}
