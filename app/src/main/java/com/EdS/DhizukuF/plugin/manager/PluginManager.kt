package com.EdS.DhizukuF.plugin.manager

import android.content.Context
import com.EdS.DhizukuF.plugin.loader.PluginLoader
import com.EdS.DhizukuF.plugin.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Central manager for installed plugins.
 *
 * - Install from .zip
 * - Enable / Disable / Stop / Delete
 * - Auto-start handling
 * - Collects settings entries, activation methods, dangerous commands from enabled plugins
 */
class PluginManager(
    private val context: Context,
    private val loader: PluginLoader,
    private val isDangerousCommandsEnabled: () -> Boolean,
    private val isOwner: () -> Boolean
) {
    private val pluginsDir = File(context.filesDir, "plugins").also { it.mkdirs() }

    private val _plugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val plugins: StateFlow<List<PluginInfo>> = _plugins.asStateFlow()

    private val loadedInstances = mutableMapOf<String, DhizukuPlugin>()

    init {
        refresh()
    }

    fun refresh() {
        val list = loader.scanInstalled(pluginsDir).map { info ->
            // rebuild "what it receives" for UI
            info.copy(whatItReceives = buildWhatItReceives(info.manifest, info.autoStart))
        }
        _plugins.value = list
    }

    /**
     * Attempt to install a plugin from a .zip file.
     * Returns Result with PluginInfo or a human-readable error.
     */
    fun installFromZip(zipFile: File): Result<PluginInfo> {
        val manifest = loader.readManifestFromZip(zipFile)
            ?: return Result.failure(Exception("Invalid plugin: missing or broken plugin.json"))

        // Security gate: dangerous commands
        if (manifest.requestsDangerousCommands && !isDangerousCommandsEnabled()) {
            return Result.failure(
                Exception(
                    "This plugin requests Dangerous commands.\n" +
                    "Enable «Dangerous commands» in Settings → Security first, then try again."
                )
            )
        }

        val targetDir = File(pluginsDir, manifest.id)
        if (targetDir.exists()) {
            // update path – remove old first
            targetDir.deleteRecursively()
        }

        val extractResult = loader.extractZip(zipFile, targetDir)
        if (extractResult.isFailure) {
            return Result.failure(extractResult.exceptionOrNull() ?: Exception("Extract failed"))
        }

        val info = PluginInfo(
            manifest = manifest,
            state = PluginState.INSTALLED,
            autoStart = manifest.autoStart,
            installPath = targetDir.absolutePath,
            whatItReceives = buildWhatItReceives(manifest, manifest.autoStart)
        )

        // Persist auto-start preference
        saveAutoStart(manifest.id, manifest.autoStart)

        refresh()
        return Result.success(info)
    }

    fun setEnabled(pluginId: String, enabled: Boolean): Result<Unit> {
        val info = _plugins.value.find { it.manifest.id == pluginId }
            ?: return Result.failure(Exception("Plugin not found"))

        if (enabled) {
            if (info.manifest.requestsDangerousCommands && !isDangerousCommandsEnabled()) {
                return Result.failure(
                    Exception("Cannot enable: plugin requires Dangerous commands (currently off)")
                )
            }
            return loadPlugin(info)
        } else {
            unloadPlugin(pluginId)
            updateState(pluginId, PluginState.DISABLED)
            return Result.success(Unit)
        }
    }

    fun stop(pluginId: String) {
        unloadPlugin(pluginId)
        updateState(pluginId, PluginState.STOPPED)
    }

    fun start(pluginId: String): Result<Unit> {
        val info = _plugins.value.find { it.manifest.id == pluginId }
            ?: return Result.failure(Exception("Plugin not found"))
        return loadPlugin(info)
    }

    fun delete(pluginId: String) {
        unloadPlugin(pluginId)
        File(pluginsDir, pluginId).deleteRecursively()
        // clear prefs
        context.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)
            .edit().clear().apply()
        refresh()
    }

    fun setAutoStart(pluginId: String, autoStart: Boolean) {
        saveAutoStart(pluginId, autoStart)
        refresh()
    }

    /** Called on app start – load all plugins that have auto-start + are not disabled */
    fun loadAutoStartPlugins() {
        _plugins.value
            .filter { it.autoStart && it.state != PluginState.DISABLED && it.state != PluginState.ERROR }
            .forEach { loadPlugin(it) }
    }

    // ---- aggregation for the rest of the app ----

    fun getAllSettingsEntries(): List<PluginSettingsEntry> {
        return loadedInstances.values.flatMap { plugin ->
            try {
                plugin.provideSettingsEntries(createContext(plugin.id))
            } catch (_: Exception) {
                emptyList()
            }
        }.sortedBy { it.order }
    }

    fun getAllActivationMethods(): List<PluginActivationMethod> {
        return loadedInstances.values.flatMap { plugin ->
            try {
                plugin.provideActivationMethods(createContext(plugin.id))
            } catch (_: Exception) {
                emptyList()
            }
        }.sortedBy { it.order }
    }

    fun getAllDangerousCommands(): List<PluginDangerousCommand> {
        if (!isDangerousCommandsEnabled()) return emptyList()
        return loadedInstances.values.flatMap { plugin ->
            try {
                plugin.provideDangerousCommands(createContext(plugin.id))
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    // ---- internals ----

    private fun loadPlugin(info: PluginInfo): Result<Unit> {
        return try {
            val instance = loader.loadInstance(File(info.installPath), info.manifest)
            val ctx = createContext(info.manifest.id)
            instance.onLoad(ctx)
            loadedInstances[info.manifest.id] = instance
            updateState(info.manifest.id, PluginState.ENABLED)
            Result.success(Unit)
        } catch (e: Exception) {
            updateState(info.manifest.id, PluginState.ERROR, e.message)
            Result.failure(e)
        }
    }

    private fun unloadPlugin(pluginId: String) {
        loadedInstances.remove(pluginId)?.let { plugin ->
            try {
                plugin.onUnload(createContext(pluginId))
            } catch (_: Exception) { }
        }
    }

    private fun updateState(pluginId: String, state: PluginState, error: String? = null) {
        _plugins.value = _plugins.value.map {
            if (it.manifest.id == pluginId) it.copy(state = state, errorMessage = error) else it
        }
    }

    private fun saveAutoStart(pluginId: String, value: Boolean) {
        context.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)
            .edit().putBoolean("auto_start", value).apply()
    }

    private fun createContext(pluginId: String): PluginContext {
        return object : PluginContext {
            override val appContext: Context = context.applicationContext
            override val isOwner: Boolean = isOwner()
            override val dangerousCommandsEnabled: Boolean = isDangerousCommandsEnabled()
            override val autoStartEnabled: Boolean =
                context.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)
                    .getBoolean("auto_start", false)

            override fun getPreference(key: String, default: String?): String? =
                context.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)
                    .getString(key, default)

            override fun putPreference(key: String, value: String?) {
                context.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)
                    .edit().putString(key, value).apply()
            }
        }
    }

    private fun buildWhatItReceives(manifest: PluginManifest, autoStart: Boolean): List<String> {
        val list = mutableListOf<String>()
        if (manifest.requestsDangerousCommands) {
            list.add("Dangerous commands")
        }
        if (autoStart) {
            list.add("Auto-start")
        } else {
            list.add("Manual start only")
        }
        if (manifest.permissions.contains("settings_entry") || manifest.permissions.contains("page")) {
            list.add("Can add menu items / pages")
        }
        if (manifest.permissions.contains("activation_method")) {
            list.add("Can add activation methods")
        }
        if (list.isEmpty()) {
            list.add("Basic plugin (no special privileges)")
        }
        return list
    }
}
