package com.EdS.DhizukuF.ui.page.settings.logs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.logging.LogEntry
import com.EdS.DhizukuF.logging.LogEvent
import com.EdS.DhizukuF.logging.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One entry of the list. [id] is its place in the loaded list: unique, so two identical entries never share a key. */
class LogRow(val id: Int, val entry: LogEntry)

data class LogsViewState(
    val loading: Boolean = true,
    val total: Int = 0,
    val rows: List<LogRow> = emptyList()
)

class LogsViewModel : ViewModel() {
    companion object {
        /** Only the newest entries are kept in memory; the file itself is never cut by this. */
        const val MAX_LOADED = 30_000
        private const val SEARCH_DELAY_MS = 200L
    }

    var state by mutableStateOf(LogsViewState())
        private set

    var query by mutableStateOf("")
        private set

    private var all: List<LogRow> = emptyList()
    private var titles: Map<LogEvent, String> = emptyMap()
    private var loadJob: Job? = null
    private var filterJob: Job? = null

    fun setTitles(value: Map<LogEvent, String>) {
        titles = value
    }

    /** Reads the journal on a background thread and shows it (newest first), filtered by the search. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val rows = LogManager.readNewest(MAX_LOADED).mapIndexed { i, e -> LogRow(i, e) }
            all = rows
            applyFilter(query)
        }
    }

    fun setQuery(value: String) {
        query = value
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            delay(SEARCH_DELAY_MS) // typing quickly does not filter on every key
            applyFilter(value)
        }
    }

    /** Every word of the search has to be found somewhere in the entry. */
    private fun applyFilter(text: String) {
        val words = text.split(' ').filter { it.isNotBlank() }
        val rows = if (words.isEmpty()) all else all.filter { row -> words.all { matches(row.entry, it) } }
        state = LogsViewState(loading = false, total = all.size, rows = rows)
    }

    // Compares the fields in place: no second copy of the text is kept for 30 000 entries (that would cost
    // tens of megabytes on a small box), and a search over all of them still takes a few milliseconds.
    private fun matches(e: LogEntry, w: String): Boolean =
        titles[e.event]?.contains(w, ignoreCase = true) == true ||
            e.event.english.contains(w, ignoreCase = true) ||
            e.category.name.contains(w, ignoreCase = true) ||
            e.level.name.contains(w, ignoreCase = true) ||
            e.appLabel?.contains(w, ignoreCase = true) == true ||
            e.packageName?.contains(w, ignoreCase = true) == true ||
            (e.uid >= 0 && e.uid.toString().contains(w)) ||
            e.message?.contains(w, ignoreCase = true) == true ||
            e.command?.contains(w, ignoreCase = true) == true ||
            e.result?.contains(w, ignoreCase = true) == true ||
            e.error?.contains(w, ignoreCase = true) == true ||
            e.details?.contains(w, ignoreCase = true) == true
}
