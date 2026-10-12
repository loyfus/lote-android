package br.com.lote

import android.content.Context
import android.content.SharedPreferences
import br.com.lote.data.LoteApiClient
import br.com.lote.data.LoteRepository
import br.com.lote.data.HomeBannerStore
import br.com.lote.data.RecentSearchesStore
import br.com.lote.data.SettingsStore
import java.util.UUID

/**
 * Manual dependency container. The graph is small and entirely stateless, so an explicit
 * container beats an annotation processor here: no codegen, no build-time cost, obvious lifetimes.
 */
class AppContainer(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("lote", Context.MODE_PRIVATE)

    private val deviceId: String = prefs.getString(DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(DEVICE_ID, it).apply()
    }

    val repository: LoteRepository by lazy { LoteRepository(LoteApiClient(deviceId)) }

    val recentSearches: RecentSearchesStore by lazy { RecentSearchesStore(prefs) }

    val homeBanner: HomeBannerStore by lazy { HomeBannerStore(prefs) }

    val settings: SettingsStore by lazy { SettingsStore(prefs) }

    private companion object {
        const val DEVICE_ID = "device_id"
    }
}
