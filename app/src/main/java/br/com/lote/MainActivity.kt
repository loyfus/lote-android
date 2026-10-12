package br.com.lote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.lote.data.ThemeMode
import br.com.lote.ui.LoteApp
import br.com.lote.ui.theme.LoteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = AppContainer(applicationContext)
        setContent {
            // Held above the theme: the choice made on the Settings screen repaints the
            // whole app at once, the screen it was made on included.
            var theme by remember { mutableStateOf(container.settings.theme()) }
            val dark = when (theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LoteTheme(darkTheme = dark) {
                LoteApp(
                    repository = container.repository,
                    recentSearches = container.recentSearches,
                    homeBanner = container.homeBanner,
                    theme = theme,
                    onThemeChange = { mode ->
                        container.settings.setTheme(mode)
                        theme = mode
                    },
                )
            }
        }
    }
}
