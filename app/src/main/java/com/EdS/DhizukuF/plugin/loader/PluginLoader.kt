package com.EdS.DhizukuF.plugin.loader

import android.content.Context
import com.EdS.DhizukuF.plugin.model.PluginInfo
import com.EdS.DhizukuF.plugin.model.PluginManifest
import com.EdS.DhizukuF.plugin.model.PluginState
import com.EdS.dhizukuf.pluginapi.DhizukuPlugin
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/**
 * Reads plugin.json, extracts zips, loads entry class via DexClassLoader (classes.dex)
 * or Class.forName (built-in / on classpath).
 */
class PluginLoader(private val appContext: Context? = null) {

    fun readManifestFromZip(zipFile: File): PluginManifest? {
        return try {
            ZipFile(zipFile).use { zip ->
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
                PluginInfo(
                    manifest = manifest,
                    state = PluginState.INSTALLED,
                    autoStart = manifest.autoStart,
                    installPath = dir.absolutePath
                )
            } ?: emptyList()
    }

    /**
     * 1) classes.dex / *.dex in plugin dir → DexClassLoader
     * 2) else Class.forName (built-in sample plugins on app classpath)
     */
    fun loadInstance(pluginDir: File, manifest: PluginManifest): DhizukuPlugin? {
        val dexFiles = pluginDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".dex") }
            .toList()

        if (dexFiles.isNotEmpty()) {
            val ctx = appContext ?: return null
            val optimized = File(ctx.codeCacheDir, "plugin_dex_${manifest.id}").apply { mkdirs() }
            val dexPath = dexFiles.joinToString(File.pathSeparator) { it.absolutePath }
            return try {
                val loader = DexClassLoader(
                    dexPath,
                    optimized.absolutePath,
                    null,
                    ctx.classLoader
                )
                val clazz = loader.loadClass(manifest.entryClass)
                clazz.getDeclaredConstructor().newInstance() as? DhizukuPlugin
            } catch (_: Exception) {
                null
            }
        }

        // No classes.dex in the plugin package
        return null
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
}
