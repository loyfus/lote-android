package br.com.lote.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// One radius per kind of object, so two things of the same kind never differ by a
// couple of dp: rectangular tags and badges 8, controls and row/grid photos 12,
// fields and buttons 14, vitrine photos and cards 16, the decision card 18, the top
// of a sheet 24, and the pill for chips.
object Radius {
    val tag = 8.dp
    val control = 12.dp
    val field = 14.dp
    val card = 16.dp
    val decision = 18.dp
    val sheet = 24.dp
}

object Corners {
    val tag = RoundedCornerShape(Radius.tag)
    val control = RoundedCornerShape(Radius.control)
    val field = RoundedCornerShape(Radius.field)
    val card = RoundedCornerShape(Radius.card)
    val decision = RoundedCornerShape(Radius.decision)
    val sheet = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
    val pill = CircleShape
}

// The Material components that stay (menu, snackbar, sheet) pick their corners from
// the theme; this keeps them on the same ladder.
val LoteShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.tag),
    small = RoundedCornerShape(Radius.control),
    medium = RoundedCornerShape(Radius.field),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.sheet),
)
