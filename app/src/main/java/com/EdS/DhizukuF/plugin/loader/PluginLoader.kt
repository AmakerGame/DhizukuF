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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Load plugins from classes.dex.
 *
 * Android expects a JAR/APK path for DexClassLoader (jar that contains classes.dex),
 * not always a naked .dex file. We also try InMemoryDexClassLoader with a direct buffer.
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
                    if (entry.isDirectory) return@forEach
                    val name = entry.name.substringAfterLast('/').ifEmpty { entry.name }
                    if (name.isBlank() || name == "." || name == "..") return@forEach
                    val out = File(targetDir, name)
                    zip.getInputStream(entry).use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
            if (!File(targetDir, "plugin.json").exists()) {
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

    fun loadInstance(pluginDir: File, manifest: PluginManifest): DhizukuPlugin {
        val dexFile = File(pluginDir, "classes.dex").takeIf { it.isFile && it.length() > 0 }
            ?: pluginDir.listFiles()?.firstOrNull { it.isFile && it.name.endsWith(".dex") && it.length() > 0 }
            ?: throw Exception(
                "No classes.dex in plugin folder (${pluginDir.absolutePath}). " +
                    "Re-install a zip that contains plugin.json + classes.dex at the root."
            )

        val parent = appContext.classLoader
        val errors = mutableListOf<String>()

        // 1) InMemory + direct ByteBuffer (no writable file; good for API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bytes = dexFile.readBytes()
                val buf = ByteBuffer.allocateDirect(bytes.size)
                buf.put(bytes)
                buf.flip()
                val loader = InMemoryDexClassLoader(buf, parent)
                return newPlugin(loader.loadClass(manifest.entryClass), manifest.entryClass)
            } catch (e: Exception) {
                errors += "InMemory: ${e.javaClass.simpleName}: ${e.message}"
                Log.w(TAG, "InMemoryDexClassLoader failed", e)
            }
        }

        // 2) JAR containing classes.dex + DexClassLoader (documented path form)
        try {
            val jar = buildDexJar(dexFile, manifest.id)
            jar.setReadable(true, false)
            jar.setWritable(false, false)
            jar.setReadOnly()
            val opt = File(jar.parentFile, "opt").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val loader = DexClassLoader(jar.absolutePath, opt.absolutePath, null, parent)
            return newPlugin(loader.loadClass(manifest.entryClass), manifest.entryClass)
        } catch (e: Exception) {
            errors += "JarDex: ${e.javaClass.simpleName}: ${e.message}"
            Log.w(TAG, "Jar DexClassLoader failed", e)
        }

        // 3) Raw read-only .dex last resort
        try {
            val local = prepareReadOnlyDexCopy(dexFile, manifest.id)
            val opt = File(local.parentFile, "opt").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val loader = DexClassLoader(local.absolutePath, opt.absolutePath, null, parent)
            return newPlugin(loader.loadClass(manifest.entryClass), manifest.entryClass)
        } catch (e: Exception) {
            errors += "RawDex: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "All dex load strategies failed", e)
        }

        throw Exception(
            "Cannot load ${manifest.entryClass}. Tried: ${errors.joinToString(" | ")}"
        )
    }

    private fun newPlugin(clazz: Class<*>, entryClass: String): DhizukuPlugin {
        val instance = try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw Exception("Cannot create $entryClass: ${e.javaClass.simpleName}: ${e.message}")
        }
        return instance as? DhizukuPlugin
            ?: throw Exception("$entryClass does not implement DhizukuPlugin")
    }

    /** Pack classes.dex into a jar — DexClassLoader expects jar/apk paths. */
    private fun buildDexJar(dexFile: File, pluginId: String): File {
        val safe = pluginId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val dir = appContext.getDir("plugin_jar_$safe", Context.MODE_PRIVATE)
        val jar = File(dir, "plugin.jar")
        if (jar.exists()) {
            jar.setWritable(true)
            jar.delete()
        }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(jar))).use { zos ->
            zos.putNextEntry(ZipEntry("classes.dex"))
            dexFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
        return jar
    }

    private fun prepareReadOnlyDexCopy(dexFile: File, pluginId: String): File {
        val safe = pluginId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val dir = appContext.getDir("plugin_dex_$safe", Context.MODE_PRIVATE)
        val local = File(dir, "classes.dex")
        if (local.exists()) {
            local.setWritable(true)
            local.delete()
        }
        dexFile.copyTo(local, overwrite = true)
        local.setReadable(true, false)
        local.setWritable(false, false)
        local.setReadOnly()
        return local
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
