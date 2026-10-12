package br.com.loyfus.data

import android.content.SharedPreferences

/**
 * Whether the reader has dismissed Home's "Como funciona" notice. Once dismissed it
 * never comes back: a notice that can be dismissed but returns is not dismissable, it is
 * a nagging element wearing the costume of one. Plain preferences, because one boolean
 * is not worth a database and never leaves the phone.
 */
class HomeBannerStore(private val prefs: SharedPreferences) {

    fun isDismissed(): Boolean = prefs.getBoolean(KEY, false)

    fun dismiss() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        /** The key the specification names, shared by both platforms' local storage. */
        const val KEY = "home.banner.dismissed"
    }
}
