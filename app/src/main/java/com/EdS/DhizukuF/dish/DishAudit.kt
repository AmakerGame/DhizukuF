package com.EdS.DhizukuF.dish

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * In-memory log of the commands that dish ran (newest last). Shown by `dish log`. Only the first
 * words of a command are kept, so values such as setting contents are never stored.
 */
object DishAudit {
    private const val MAX = 200
    private val lines = ArrayDeque<String>()
    private val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT)

    @Synchronized
    fun record(uid: Int, pkg: String, args: List<String>, code: Int) {
        val shown = args.take(3).joinToString(" ") + if (args.size > 3) " ..." else ""
        lines.addLast("${format.format(Date())}  uid=$uid $pkg  exit=$code  $shown")
        while (lines.size > MAX) lines.removeFirst()
    }

    @Synchronized
    fun last(n: Int): List<String> = lines.toList().takeLast(n)

    @Synchronized
    fun clear() {
        lines.clear()
    }
}
