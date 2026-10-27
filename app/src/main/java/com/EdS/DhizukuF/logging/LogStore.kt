package com.EdS.DhizukuF.logging

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.ArrayDeque

/**
 * The journal file. Appending, trimming, clearing and export exclude each other.
 * Lightweight reads (readNewest) do not take the lock: the file only grows at its end, and a trim
 * replaces it atomically, so a reader sees either the old or the new file.
 */
class LogStore(private val file: File) {
    private val lock = Any()

    fun size(): Long = if (file.exists()) file.length() else 0L

    fun hasData(): Boolean = size() > 0L

    /** Adds the lines (without line breaks) and returns the new size of the file. */
    fun append(lines: List<String>): Long = synchronized(lock) {
        if (lines.isEmpty()) return file.length()
        file.parentFile?.mkdirs()
        FileOutputStream(file, true).use { out ->
            val text = StringBuilder()
            for (line in lines) text.append(line).append('\n')
            out.write(text.toString().toByteArray(Charsets.UTF_8))
        }
        file.length()
    }

    /**
     * Keeps the journal under [maxBytes]: when it is larger, the oldest entries are dropped until about 80 % of
     * the limit is left (so the work is not repeated on every write). Streams, never loads the file.
     */
    fun trimTo(maxBytes: Long) = synchronized(lock) {
        val length = size()
        if (length <= maxBytes) return@synchronized
        val keep = (maxBytes * 8) / 10
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileInputStream(file).use { input ->
            val channel = input.channel
            channel.position((length - keep).coerceAtLeast(0))
            // Throw away the partial line at the cut.
            while (true) {
                val b = input.read()
                if (b == -1 || b == '\n'.code) break
            }
            FileOutputStream(tmp).use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                }
            }
        }
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    fun clear() = synchronized(lock) {
        file.delete()
        File(file.parentFile, file.name + ".tmp").delete()
        Unit
    }

    /** The newest [limit] entries, newest first. Damaged lines are skipped. */
    fun readNewest(limit: Int): List<LogEntry> {
        if (!file.exists()) return emptyList()
        val window = ArrayDeque<LogEntry>()
        forEachEntry { entry ->
            window.addLast(entry)
            if (window.size > limit) window.removeFirst()
        }
        return window.toList().asReversed()
    }

    /** Every entry, oldest first, one at a time. */
    fun forEachEntry(action: (LogEntry) -> Unit) {
        if (!file.exists()) return
        BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8), 64 * 1024).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                LogCodec.decode(line)?.let(action)
            }
        }
    }

    /**
     * Writes the whole journal as a readable text file, oldest entry first. Returns the number of entries.
     * Holds [lock] for the whole export so append/trim cannot change the file mid-read.
     */
    fun writeReadable(out: OutputStream, header: String): Int = synchronized(lock) {
        val writer = out.bufferedWriter(Charsets.UTF_8)
        var count = 0
        val body = StringBuilder()
        forEachEntry { count++ }
        writer.write(header)
        writer.write("Entries: $count\n")
        writer.write("----------------------------------------\n")
        forEachEntry { entry ->
            body.setLength(0)
            body.append(LogCodec.readable(entry))
            writer.write(body.toString())
        }
        writer.flush()
        count
    }
}
