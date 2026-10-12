package br.com.loyfus.ui.theme

import androidx.compose.ui.unit.dp

// A 4dp base scale. Spacing is the main tool for hierarchy in this app.
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val x3 = 32.dp
    val x4 = 40.dp
    val x5 = 56.dp
    val gutter = 20.dp
}

// Interaction sizes. Material 3 asks for 48dp and the HIG for 44pt; 48dp clears
// both, so one number governs every control in the app. Controls may look smaller
// than this; the hit box may not.
object Touch {
    /** Minimum tappable box. */
    val target = 48.dp
}

// Reading measure. Past roughly this width the body copy stops being a card grid
// and starts being lines of text nobody wants to read across a tablet.
object Adaptive {
    val maxContentWidth = 640.dp

    /** Window width at which the grid goes to three and four columns. */
    private const val WIDE = 600f
    private const val WIDER = 840f

    /** Text at or above this scale no longer fits two tiles side by side. */
    private const val LARGE_TEXT = 1.3f

    /**
     * Columns of a tile grid: two on a phone, three from 600dp, four from 840dp. Large
     * text halves the count (rounded up), which puts a phone on one column: two columns
     * stop working once the reader has turned the text up, because the price loses its
     * line to the title. Fewer columns, not smaller text, is the adaptive answer.
     */
    fun gridColumns(widthDp: Float, fontScale: Float): Int {
        val byWidth = when {
            widthDp >= WIDER -> 4
            widthDp >= WIDE -> 3
            else -> 2
        }
        return if (fontScale >= LARGE_TEXT) (byWidth + 1) / 2 else byWidth
    }
}
