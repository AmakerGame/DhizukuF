package com.EdS.DhizukuF.plugin.loader

import android.content.Context
import android.os.Build
import android.util.Log
import com.EdS.DhizukuF.plugin.model.PluginInfo
import com.EdS.DhizukuF.plugin.model.PluginManifest
import com.EdS.DhizukuF.plugin.model.PluginState
import com.EdS.dhizukuf.pluginapi.DhizukuPlugin
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.ZipFile

/**
 * Reads plugin.json, extracts zips, loads entry class from classes.dex via DexClassLoader.
 */
class PluginLoader(private val appContext: Context) {

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
            if (targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    // Flatten unsafe paths; keep simple relative names
                    val name = entry.name.substringAfterLast('/').ifEmpty { entry.name }
                    if (entry.isDirectory) return@forEach
                    if (name.isBlank() || name == "." || name == "..") return@forEach
                    val out = File(targetDir, name)
                    zip.getInputStream(entry).use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
            val hasManifest = File(targetDir, "plugin.json").exists()
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
     * Load plugin entry class from classes.dex (required).
     * Throws with a clear message on failure (caller maps to Result).
     */
    fun loadInstance(pluginDir: File, manifest: PluginManifest): DhizukuPlugin {
        val dexFile = File(pluginDir, "classes.dex").takeIf { it.isFile }
            ?: pluginDir.listFiles()?.firstOrNull { it.isFile && it.name.endsWith(".dex") }
            ?: throw Exception(
                "No classes.dex in plugin folder (${pluginDir.absolutePath}). " +
                    "Re-install a zip that contains plugin.json + classes.dex at the root."
            )

        val parent = appContext.classLoader
        val clazz = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bytes = dexFile.readBytes()
                val loader = InMemoryDexClassLoader(ByteBuffer.wrap(bytes), parent)
                loader.loadClass(manifest.entryClass)
            } else {
                val optimized = appContext.getDir("plugin_dex_${manifest.id}", Context.MODE_PRIVATE)
                val loader = DexClassLoader(
                    dexFile.absolutePath,
                    optimized.absolutePath,
                    null,
                    parent
                )
                loader.loadClass(manifest.entryClass)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dex load failed for ${manifest.entryClass}", e)
            throw Exception(
                "Cannot load ${manifest.entryClass} from ${dexFile.name}: ${e.javaClass.simpleName}: ${e.message}"
            )
        }

        val instance = try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw Exception(
                "Cannot create ${manifest.entryClass}: ${e.javaClass.simpleName}: ${e.message}"
            )
        }

        return instance as? DhizukuPlugin
            ?: throw Exception("${manifest.entryClass} does not implement DhizukuPlugin")
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

    companion object {
        private const val TAG = "PluginLoader"
    }
}
