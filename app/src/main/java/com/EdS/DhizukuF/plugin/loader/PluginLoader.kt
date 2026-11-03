package com.EdS.DhizukuF.plugin.loader

import com.EdS.DhizukuF.plugin.model.PluginInfo
import com.EdS.DhizukuF.plugin.model.PluginManifest
import com.EdS.DhizukuF.plugin.model.PluginState
import com.EdS.dhizukuf.pluginapi.DhizukuPlugin
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/**
 * Responsible for reading plugin.json, extracting zips and loading the entry class.
 *
 * Note: Full dynamic DEX loading (DexClassLoader) is left as a clear extension point.
 * For the first version we support plugins that ship a pre-registered entry or
 * simple reflection when the class is already on the classpath (debug / built-in).
 */
class PluginLoader {

    fun readManifestFromZip(zipFile: File): PluginManifest? {
        return try {
            ZipFile(zipFile).use { zip ->
                // Prefer root plugin.json; otherwise first */plugin.json in the archive
                val entry = zip.getEntry("plugin.json")
                    ?: zip.entries().asSequence().firstOrNull { e ->
                        !e.isDirectory && (e.name == "plugin.json" || e.name.endsWith("/plugin.json"))
                    }
                    ?: return null
                val text = zip.getInputStream(entry).bufferedReader().readText()
                parseManifest(text)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun readManifestFromDir(dir: File): PluginManifest? {
        val file = File(dir, "plugin.json").takeIf { it.exists() }
            ?: dir.walkTopDown().firstOrNull { it.isFile && it.name == "plugin.json" }
            ?: return null
        return try {
            parseManifest(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun extractZip(zipFile: File, targetDir: File): Result<Unit> {
        return try {
            targetDir.mkdirs()
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val out = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            out.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
            // basic validation (root or nested plugin.json)
            val hasManifest = File(targetDir, "plugin.json").exists() ||
                targetDir.walkTopDown().any { it.isFile && it.name == "plugin.json" }
            if (!hasManifest) {
                targetDir.deleteRecursively()
                return Result.failure(Exception("plugin.json missing after extract"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            Result.failure(e)
        }
    }

    fun scanInstalled(pluginsDir: File): List<PluginInfo> {
        if (!pluginsDir.exists()) return emptyList()
        return pluginsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val manifest = readManifestFromDir(dir) ?: return@mapNotNull null
                val prefs = dir.contextPrefs() // helper below is not available; use simple default
                PluginInfo(
                    manifest = manifest,
                    state = PluginState.INSTALLED,
                    autoStart = manifest.autoStart, // real value is restored by PluginManager
                    installPath = dir.absolutePath
                )
            } ?: emptyList()
    }

    /**
     * Load the plugin instance.
     * Current implementation tries Class.forName (works for built-in / debug plugins).
     * Production dynamic loading would use DexClassLoader on classes.dex inside the plugin dir.
     */
    fun loadInstance(pluginDir: File, manifest: PluginManifest): DhizukuPlugin? {
        return try {
            // TODO: real implementation with DexClassLoader when classes.dex is present
            val clazz = Class.forName(manifest.entryClass)
            clazz.getDeclaredConstructor().newInstance() as? DhizukuPlugin
        } catch (_: Exception) {
            null
        }
    }

    private fun parseManifest(json: String): PluginManifest {
        val obj = JSONObject(json)
        return PluginManifest(
            id = obj.getString("id"),
            name = obj.getString("name"),
            version = obj.getString("version"),
            versionCode = obj.getInt("versionCode"),
            description = obj.optString("description", ""),
            author = obj.optString("author", ""),
            minDhizukuF = obj.optInt("minDhizukuF", 1),
            entryClass = obj.getString("entryClass"),
            autoStart = obj.optBoolean("autoStart", false),
            requestsDangerousCommands = obj.optBoolean("requestsDangerousCommands", false),
            permissions = obj.optJSONArray("permissions")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )
    }

    // placeholder to keep scanInstalled compiling cleanly
    private fun File.contextPrefs(): Boolean = false
}
