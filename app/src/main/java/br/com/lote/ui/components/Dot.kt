package br.com.lote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** A coloured dot: the freshness of Home, the colour key of a Saved group. Decorative. */
@Composable
fun Dot(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}
