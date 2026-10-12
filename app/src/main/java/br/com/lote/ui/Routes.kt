package br.com.lote.ui

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val FAVOURITES = "favourites"
    const val DETAIL = "lot/{lotId}"
    const val SETTINGS = "settings"

    fun detail(lotId: Long) = "lot/$lotId"
}
