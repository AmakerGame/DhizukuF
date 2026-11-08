package com.EdS.DhizukuF.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The text format of the journal: one entry per line, fields separated by a tab, with `\`, tab and line breaks
 * escaped, so one entry is always one line. A damaged line (for example the last one after a crash) is skipped
 * when the file is read.
 */
object LogCodec {
    private const val VERSION = "v1"

    // Limits keep one entry small, and keep long values (a stack trace, a long command) from filling the journal.
    private const val MAX_LABEL = 100
    private const val MAX_PACKAGE = 200
    private const val MAX_MESSAGE = 500
    private const val MAX_COMMAND = 300
    private const val MAX_RESULT = 500
    private const val MAX_ERROR = 3000
    private const val MAX_DETAILS = 1000

    fun encode(e: LogEntry): String {
        val fields = listOf(
            VERSION,
            e.time.toString(),
            e.level.name,
            e.category.name,
            e.event.name,
            e.uid.toString(),
            cut(e.appLabel, MAX_LABEL),
            cut(e.packageName, MAX_PACKAGE),
            cut(e.message, MAX_MESSAGE),
            cut(e.command, MAX_COMMAND),
            cut(e.result, MAX_RESULT),
            cut(e.error, MAX_ERROR),
            cut(e.details, MAX_DETAILS)
        )
        return fields.joinToString("\t") { escape(it) }
    }

    fun decode(line: String): LogEntry? {
        if (!line.startsWith("$VERSION\t")) return null
        val f = line.split('\t')
        if (f.size < 13) return null
        return try {
            LogEntry(
                time = f[1].toLong(),
                level = LogLevel.valueOf(f[2]),
                category = LogCategory.valueOf(f[3]),
                event = LogEvent.valueOf(f[4]),
                uid = f[5].toInt(),
                appLabel = unescape(f[6]).ifEmpty { null },
                packageName = unescape(f[7]).ifEmpty { null },
                message = unescape(f[8]).ifEmpty { null },
                command = unescape(f[9]).ifEmpty { null },
                result = unescape(f[10]).ifEmpty { null },
                error = unescape(f[11]).ifEmpty { null },
                details = unescape(f[12]).ifEmpty { null }
            )
        } catch (_: Exception) {
            null
        }
    }

    /** The plain-text block of one entry, for the exported file (easy to read and to paste into a GitHub issue). */
    fun readable(e: LogEntry): String = buildString {
        append(timestamp(e.time)).append("  [").append(e.level.name.uppercase(Locale.ROOT)).append("] ")
        append(e.event.english).append('\n')
        if (e.appLabel != null || e.packageName != null) {
            append("    App: ")
            append(e.appLabel ?: "?")
            if (e.packageName != null) append(" (").append(e.packageName).append(')')
            if (e.uid >= 0) append(" uid=").append(e.uid)
            append('\n')
        } else if (e.uid >= 0) {
            append("    UID: ").append(e.uid).append('\n')
        }
        line("Message", e.message)
        line("Command", e.command)
        line("Result", e.result)
        line("Error", e.error)
        line("Details", e.details)
    }

    private fun StringBuilder.line(name: String, value: String?) {
        if (value == null) return
        val lines = value.lines()
        append("    ").append(name).append(": ").append(lines.first()).append('\n')
        for (more in lines.drop(1)) append("        ").append(more).append('\n')
    }

    fun timestamp(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date(time))

    private fun cut(value: String?, max: Int): String {
        if (value == null) return ""
        return if (value.length <= max) value else value.substring(0, max) + "..."
    }

    private fun escape(s: String): String {
        if (s.none { it == '\\' || it == '\t' || it == '\n' || it == '\r' }) return s
        val sb = StringBuilder(s.length + 8)
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\t' -> sb.append("\\t")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun unescape(s: String): String {
        if (!s.contains('\\')) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    't' -> sb.append('\t')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
