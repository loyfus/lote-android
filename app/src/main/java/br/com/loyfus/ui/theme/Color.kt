package br.com.loyfus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// "Pregão": a night-blue ground that carries the brand (the hero of Home, the icon),
// cool neutral paper so the photographs keep their own colour, and one amber accent
// for "act now". Pine marks what is alive and positive. No other hue reaches text.
//
// Amber comes in two strengths because a fill is not a text colour: `amber` is the
// fill (the CTA, the urgent time chip, the badges) and always carries `onAmber`;
// `amberText` is the amber dark enough to write on paper (an urgent price or time,
// the heart, the last-day bar). The fill never writes on paper: it measures 1.88:1.
//
// Every token is a role, not a colour, and resolves through the active theme. The
// light constants below are the reference palette; DarkRoles restates each one
// for dark appearances rather than inverting it, because a dark theme is not an
// inversion. Reading these as top-level composable getters keeps call sites
// unchanged and makes it impossible to draw light-mode ink on a dark screen.

@Immutable
data class Roles(
    val ink: Color,
    val ink80: Color,
    val ink60: Color,
    val ink40: Color,
    val paper: Color,
    val paperDim: Color,
    val surface: Color,
    val line: Color,
    val lineControl: Color,
    val amber: Color,
    val onAmber: Color,
    val amberText: Color,
    val amberSoft: Color,
    val amberDeep: Color,
    val pine: Color,
    val pineSoft: Color,
    val slate: Color,
    val slateSoft: Color,
    val scrim: Color,
    val heartOn: Color,
    // The hero of Home: night blue in both themes, the only gradient in the app, and
    // the ground of the fixed bar above it. Only onHero and onHero80 write on it.
    val heroTop: Color,
    val heroBottom: Color,
    val onHero: Color,
    val onHero80: Color,
    // The search pill's border on the hero: 3:1 against both ends of the gradient
    // and against the pill's own fill (.tools/ui_audit.py).
    val heroControl: Color,
    /** Frosted panel over the hero: the shortcuts and the notice. */
    val glass: Color,
    val glassLine: Color,
)

// Reference palette. Light values were solved against WCAG 2.2 AA rather than
// picked by eye; see .tools/ui_audit.py for the pair-by-pair numbers.
internal val LightRoles = Roles(
    ink = Color(0xFF0E1726),
    ink80 = Color(0xFF2F3A4B),
    ink60 = Color(0xFF586375),
    ink40 = Color(0xFF5E6879),
    paper = Color(0xFFF6F7F9),
    paperDim = Color(0xFFECEEF2),
    surface = Color(0xFFFFFFFF),
    line = Color(0xFFCDD3DC),
    lineControl = Color(0xFF7D8698),
    amber = Color(0xFFF5A70A),
    onAmber = Color(0xFF1A1200),
    amberText = Color(0xFFB54708),
    amberSoft = Color(0xFFFDEFD3),
    amberDeep = Color(0xFF7A3D00),
    pine = Color(0xFF13705A),
    pineSoft = Color(0xFFDCEFE8),
    slate = Color(0xFF4A5568),
    slateSoft = Color(0xFFE8EBF0),
    // 64% of the ink: white text on it stays above 5.3:1 even over a pure white photo.
    scrim = Color(0xA30E1726),
    heartOn = Color(0xFFB54708),
    heroTop = Color(0xFF14325A),
    heroBottom = Color(0xFF0C1E38),
    onHero = Color(0xFFF3F6FA),
    onHero80 = Color(0xFFC9D3E1),
    heroControl = Color(0xFF7D8698),
    // White at 9% / 16%: the audit composites the panel over both ends of the gradient.
    glass = Color(0x17FFFFFF),
    glassLine = Color(0x29FFFFFF),
)

// Dark is a separate palette: the same night blue a shade deeper, near-black cool
// grounds, the amber lifted until it can write on them by itself.
// Every value verified by .tools/ui_audit.py.
internal val DarkRoles = Roles(
    ink = Color(0xFFEEF2F8),
    ink80 = Color(0xFFCBD3DF),
    ink60 = Color(0xFFA3ADBC),
    ink40 = Color(0xFF98A2B1),
    paper = Color(0xFF0A0E15),
    paperDim = Color(0xFF1E2531),
    surface = Color(0xFF131925),
    line = Color(0xFF333C4B),
    lineControl = Color(0xFF6B778B),
    amber = Color(0xFFF7B733),
    onAmber = Color(0xFF241800),
    amberText = Color(0xFFF7B733),
    amberSoft = Color(0xFF4A3408),
    amberDeep = Color(0xFFFFE2A1),
    pine = Color(0xFF6FCBAE),
    pineSoft = Color(0xFF103D31),
    slate = Color(0xFFA7B0C0),
    slateSoft = Color(0xFF2B3038),
    scrim = Color(0xAD000000),
    // Fixed in both themes: the heart always sits on a white disc, and the dark
    // theme's amber would wash out there.
    heartOn = Color(0xFFB54708),
    heroTop = Color(0xFF112A4D),
    heroBottom = Color(0xFF0A1729),
    onHero = Color(0xFFF3F6FA),
    onHero80 = Color(0xFFC9D3E1),
    heroControl = Color(0xFF8C97A8),
    glass = Color(0x17FFFFFF),
    glassLine = Color(0x29FFFFFF),
)

val LocalRoles = staticCompositionLocalOf { LightRoles }

val Ink: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.ink
val Ink80: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.ink80
val Ink60: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.ink60
val Ink40: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.ink40
val Paper: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.paper
val PaperDim: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.paperDim
val Surface: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.surface
val Line: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.line
val LineControl: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.lineControl
val Amber: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.amber
val OnAmber: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.onAmber
val AmberText: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.amberText
val AmberSoft: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.amberSoft
val AmberDeep: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.amberDeep
val Pine: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.pine
val PineSoft: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.pineSoft
val Slate: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.slate
val SlateSoft: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.slateSoft
val Scrim: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.scrim
val HeartOn: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.heartOn
val HeroTop: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.heroTop
val HeroBottom: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.heroBottom
val OnHero: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.onHero
val OnHero80: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.onHero80
val HeroControl: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.heroControl
val Glass: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.glass
val GlassLine: Color
    @Composable @ReadOnlyComposable get() = LocalRoles.current.glassLine
