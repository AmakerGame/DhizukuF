package com.EdS.DhizukuF.logging

import android.content.Context
import android.os.Build
import android.util.Log
import com.EdS.DhizukuF.BuildConfig
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The one place that writes DhizukuF's journal (Settings > Logs).
 *
 * - **Off means off.** [log] looks at the cached configuration first and returns at once when logging is off,
 *   the level is too low or the category is switched off: no object, no thread, no file.
 * - Events are handed to a small background writer, so a binder call or the UI never waits for the disk. The queue
 *   is bounded: when it is full, new events are dropped instead of growing the memory.
 * - The writer thread starts with the first event and ends after 20 idle seconds: nothing runs in the background
 *   while nothing is logged.
 * - The file is trimmed to the chosen size by dropping the oldest entries.
 */
object LogManager {
    private const val TAG = "LogManager"
    private const val QUEUE_CAPACITY = 1000
    private const val BATCH = 200
    private const val IDLE_SECONDS = 20L

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var settings: SettingsRepo? = null

    @Volatile
    private var storeInstance: LogStore? = null

    private val queue = ArrayBlockingQueue<LogEntry>(QUEUE_CAPACITY)
    private val writerLock = Any()
    private var writer: Thread? = null

    private val changed = MutableStateFlow(0L)

    /** Counts saved batches: the Logs screen reloads when it changes. */
    val changes: StateFlow<Long> get() = changed

    /** Called once at start-up (before any content provider is used). Does nothing else: no file, no thread. */
    fun init(context: Context) {
        // Called from Application.attachBaseContext(), where applicationContext is still null: keep the context itself.
        appContext = context.applicationContext ?: context
    }

    // ---------------------------------------------------------------- writing

    fun config(): LogConfig {
        val repo = settings ?: try {
            KoinPlatform.getKoin().get<SettingsRepo>().also { settings = it }
        } catch (_: Throwable) {
            return LogConfig() // Koin is not up yet: treated as off
        }
        return try {
            repo.logConfig
        } catch (_: Throwable) {
            LogConfig()
        }
    }

    /** True when an event of this kind would be saved: lets a caller skip building its text. */
    fun isActive(category: LogCategory, level: LogLevel = LogLevel.Normal): Boolean =
        config().accepts(category, level)

    fun log(
        event: LogEvent,
        uid: Int = -1,
        packageName: String? = null,
        message: String? = null,
        command: String? = null,
        result: String? = null,
        error: String? = null,
        details: String? = null,
        level: LogLevel = event.level,
        category: LogCategory = event.category
    ) {
        if (!config().accepts(category, level)) return
        val entry = LogEntry(
            time = System.currentTimeMillis(),
            level = level,
            category = category,
            event = event,
            uid = uid,
            packageName = packageName,
            message = message,
            command = command,
            result = result,
            error = error,
            details = details
        )
        if (!queue.offer(entry)) return // full: drop rather than grow
        ensureWriter()
    }

    /** An exception that concerns DhizukuF. Only the exception text and a few stack frames are kept. */
    fun error(where: String, t: Throwable, uid: Int = -1, packageName: String? = null) {
        if (!config().accepts(LogCategory.Error, LogLevel.Error)) return
        log(
            LogEvent.ErrorOccurred, uid = uid, packageName = packageName,
            message = where, error = describe(t)
        )
    }

    /** For the crash handler: the process is about to die, so the entry is written now, on this thread. */
    fun crash(t: Throwable, threadName: String) {
        if (!config().accepts(LogCategory.Error, LogLevel.Error)) return
        val entry = LogEntry(
            System.currentTimeMillis(), LogLevel.Error, LogCategory.Error, LogEvent.ErrorOccurred,
            message = "Crash in thread $threadName", error = describe(t)
        )
        store()?.append(listOf(LogCodec.encode(entry)))
    }

    /** What a command did, in a few words: the first [words] words, never the values behind them. */
    fun summarize(args: List<String>, words: Int = 3): String {
        if (args.isEmpty()) return ""
        val shown = args.take(words).joinToString(" ")
        return if (args.size > words) "$shown ..." else shown
    }

    private fun describe(t: Throwable): String = buildString {
        append(t.toString())
        t.stackTrace.take(8).forEach { append("\n    at ").append(it) }
        t.cause?.let { append("\nCaused by: ").append(it) }
    }

    private fun ensureWriter() {
        synchronized(writerLock) {
            if (writer?.isAlive == true) return
            writer = Thread({ runWriter() }, "dhizukuf-log").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun runWriter() {
        try {
            while (true) {
                val first = queue.poll(IDLE_SECONDS, TimeUnit.SECONDS) ?: break
                val batch = ArrayList<LogEntry>(BATCH)
                batch.add(first)
                queue.drainTo(batch, BATCH - 1)
                try {
                    write(batch)
                } catch (t: Throwable) {
                    Log.w(TAG, "cannot write the journal", t)
                }
            }
        } finally {
            synchronized(writerLock) {
                writer = null
                // an event that arrived while the thread was ending must not be left behind
                if (queue.isNotEmpty()) ensureWriter()
            }
        }
    }

    private fun write(batch: List<LogEntry>) {
        val cfg = config()
        // Logging was switched off while the events waited: they are thrown away, never saved.
        if (!cfg.enabled) return
        val context = appContext ?: return
        val store = store() ?: return
        val lines = batch
            .filter { cfg.accepts(it.category, it.level) }
            .map { LogCodec.encode(withApp(it, context)) }
        if (lines.isEmpty()) return
        val size = store.append(lines)
        if (size > cfg.maxBytes) store.trimTo(cfg.maxBytes)
        changed.value = changed.value + 1
    }

    /** Finds the app name for a package or a uid. Runs on the writer thread, and only when logging is on. */
    private fun withApp(e: LogEntry, context: Context): LogEntry {
        if (e.appLabel != null || (e.packageName == null && e.uid < 0)) return e
        val pm = context.packageManager
        val pkg = e.packageName ?: runCatching { pm.getPackagesForUid(e.uid)?.firstOrNull() }.getOrNull()
        val label = pkg?.let {
            runCatching { pm.getApplicationInfo(it, 0).loadLabel(pm).toString() }.getOrNull()
        }
        return e.copy(packageName = pkg, appLabel = label)
    }

    // ---------------------------------------------------------------- reading and managing

    private fun store(): LogStore? {
        storeInstance?.let { return it }
        val context = appContext ?: return null
        // Device protected storage: the journal also works before the first unlock, like the settings do.
        val base = if (context.isDeviceProtectedStorage) context else context.createDeviceProtectedStorageContext()
        return synchronized(this) {
            storeInstance ?: LogStore(File(File(base.filesDir, "logs"), "dhizukuf.log")).also { storeInstance = it }
        }
    }

    fun readNewest(limit: Int): List<LogEntry> = store()?.readNewest(limit) ?: emptyList()

    fun sizeBytes(): Long = store()?.size() ?: 0L

    fun hasData(): Boolean = store()?.hasData() == true

    /** Removes every saved entry (and the ones still waiting to be written). */
    fun clear() {
        queue.clear()
        store()?.clear()
        changed.value = changed.value + 1
    }

    /** After the user lowered the maximum size: shrink the journal now, not at the next event. */
    fun enforceLimit() {
        store()?.trimTo(config().maxBytes)
        changed.value = changed.value + 1
    }

    /** Writes the readable text file (see [LogCodec.readable]). Returns the number of entries. */
    fun exportReadable(out: OutputStream): Int {
        val header = buildString {
            append("DhizukuF logs\n")
            append("Exported: ").append(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
            ).append('\n')
            append("DhizukuF ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(") | ")
            append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(") | ")
            append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
        }
        return store()?.writeReadable(out, header) ?: 0
    }
}
