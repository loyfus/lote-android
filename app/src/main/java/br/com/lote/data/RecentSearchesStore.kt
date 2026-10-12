package br.com.lote.data

import android.content.SharedPreferences
import br.com.lote.core.withRecent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * The reader's last searches, kept on the device and nowhere else. Plain preferences:
 * eight short strings are not worth a database, and nothing here ever leaves the phone.
 */
class RecentSearchesStore(private val prefs: SharedPreferences) {

    private val serializer = ListSerializer(String.serializer())

    fun load(): List<String> {
        val stored = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { LoteApiClient.parser.decodeFromString(serializer, stored) }
            .getOrDefault(emptyList())
    }

    /** Remembers [query] as the latest search and returns the list as it now stands. */
    fun add(query: String): List<String> {
        val updated = load().withRecent(query)
        save(updated)
        return updated
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun save(list: List<String>) {
        prefs.edit().putString(KEY, LoteApiClient.parser.encodeToString(serializer, list)).apply()
    }

    private companion object {
        const val KEY = "recent_searches"
    }
}
