package br.com.lote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import br.com.lote.R

// Static instances, one file per weight, shared byte-for-byte with iOS. This is
// deliberate: a single variable file renders its *default* instance wherever the
// platform cannot pin axes, and the old Fraunces file defaulted to 9pt Black —
// so iOS drew every serif title Black at text optical size while Android pinned
// opsz 144 per weight. Same files, same result, no silent divergence.
val Bricolage = FontFamily(
    Font(R.font.bricolage_regular, FontWeight.Normal),
    Font(R.font.bricolage_medium, FontWeight.Medium),
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold),
)

// Inter Tight: neutral, slightly narrow, very legible at 12-14sp for dense metadata.
// Same static-instance rule as above: the old variable file defaulted to Regular,
// so every semibold price and label on iOS was quietly Regular.
val InterTight = FontFamily(
    Font(R.font.inter_tight_regular, FontWeight.Normal),
    Font(R.font.inter_tight_medium, FontWeight.Medium),
    Font(R.font.inter_tight_semibold, FontWeight.SemiBold),
    Font(R.font.inter_tight_bold, FontWeight.Bold),
)

// Half-leading above and below the glyphs, like a browser: the Compose default would
// hang the extra line height under the text and make every label look top-heavy
// against the reference.
private val centered = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * The type scale: seventeen roles, two families, no capitals or letter-spacing on labels (the
 * wordmark is the only capitalised text in the app). Bricolage carries identity and titles,
 * Inter Tight the interface and every number, always with tabular figures so prices and
 * countdowns never wobble. Sizes are sp, and no container below fixes a height around text.
 */
object Type {
    val display = serif(30, 34, -0.6)
    val title = serif(24, 29, -0.4)
    val titleLong = serif(20, 26, -0.3)
    val headline = serif(21, 26, -0.3)
    val section = serif(19, 24, -0.2)
    val wordmark = serif(19, 24, -0.2)

    val cardTitle = sans(FontWeight.SemiBold, 15, 20, -0.1)
    val body = sans(FontWeight.Normal, 16, 24)
    val bodyDense = sans(FontWeight.Normal, 15, 22)
    val small = sans(FontWeight.Normal, 14, 20)
    val label = sans(FontWeight.SemiBold, 14, 18)
    val caption = sans(FontWeight.Medium, 12, 16)

    val priceTile = sans(FontWeight.Bold, 18, 22, -0.3)
    val priceRow = sans(FontWeight.Bold, 17, 22)
    val priceSticky = sans(FontWeight.Bold, 21, 25, -0.4)
    val priceHero = sans(FontWeight.Bold, 34, 40, -1.0)

    val button = sans(FontWeight.SemiBold, 16, 20)
}

// The Material components that stay (navigation bar, menu, snackbar, text selection)
// read their text from the theme; this maps every slot onto the scale above so none
// of them falls back to a default Roboto style.
val LoteTypography = Typography(
    displayLarge = Type.display,
    displayMedium = Type.display,
    displaySmall = Type.display,
    headlineLarge = Type.headline,
    headlineMedium = Type.headline,
    headlineSmall = Type.section,
    titleLarge = Type.section,
    titleMedium = Type.cardTitle,
    titleSmall = Type.label,
    bodyLarge = Type.body,
    bodyMedium = Type.bodyDense,
    bodySmall = Type.small,
    labelLarge = Type.label,
    labelMedium = Type.caption,
    labelSmall = Type.caption,
)

private fun serif(size: Int, lineHeight: Int, tracking: Double) = TextStyle(
    fontFamily = Bricolage,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = centered,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private fun sans(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = InterTight,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = "tnum",
    lineHeightStyle = centered,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * The in-between sizes the component specs name outright without promoting them to roles: they
 * reuse a role's family, weight and tracking and change nothing else.
 */
fun TextStyle.sized(size: Double, lineHeight: Double, weight: FontWeight? = null): TextStyle =
    copy(fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight ?: fontWeight)
