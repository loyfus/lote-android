package br.com.loyfus.data

import android.content.SharedPreferences

/** The theme the reader picked: the phone's, or one of the two pinned regardless of it. */
enum class ThemeMode(val key: String, val label: String) {
    System("system", "Automático"),
    Light("light", "Claro"),
    Dark("dark", "Escuro"),
}

/**
 * The reader's choices on Settings. Plain preferences, like the other stores: a handful of values
 * never leave the phone. Keys and values are the same on both platforms (`UserDefaults` on iOS).
 */
class SettingsStore(private val prefs: SharedPreferences) {

    fun theme(): ThemeMode {
        val stored = prefs.getString(THEME, null)
        return ThemeMode.entries.firstOrNull { it.key == stored } ?: ThemeMode.System
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(THEME, mode.key).apply()
    }

    private companion object {
        const val THEME = "settings.theme"
    }
}
