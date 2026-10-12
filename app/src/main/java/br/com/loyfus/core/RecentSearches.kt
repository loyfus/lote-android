package br.com.loyfus.core

import java.util.Locale

/** How many searches the app remembers. */
const val RECENT_SEARCH_LIMIT = 8

/**
 * The list of recent searches after one more: newest first, no repeats (case and
 * surrounding space do not make a search different), at most [limit] kept. A blank
 * search is not worth remembering and leaves the list as it was.
 */
fun List<String>.withRecent(query: String, limit: Int = RECENT_SEARCH_LIMIT): List<String> {
    val clean = query.trim().replace(Regex("\\s+"), " ")
    if (clean.isEmpty()) return this
    val key = clean.lowercase(Locale.ROOT)
    return (listOf(clean) + filter { it.lowercase(Locale.ROOT) != key }).take(limit)
}
