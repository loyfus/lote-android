package br.com.loyfus.data

import android.content.SharedPreferences

/**
 * Whether the reader dismissed Home's "Como funciona" notice. Once dismissed it never returns: a
 * notice that comes back is not dismissable, it is nagging in costume. Plain preferences, because
 * one boolean never leaves the phone.
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
