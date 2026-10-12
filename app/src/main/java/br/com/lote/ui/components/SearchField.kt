package br.com.lote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.lote.ui.theme.Corners
import br.com.lote.ui.theme.Ink
import br.com.lote.ui.theme.Ink60
import br.com.lote.ui.theme.LineControl
import br.com.lote.ui.theme.Surface
import br.com.lote.ui.theme.Touch
import br.com.lote.ui.theme.Type

// The search field is a control, so it has the control's 1dp border, 3:1 against the
// ground; the border goes to ink while the field has focus. 52dp tall at least, radius
// 14, a surface fill: the one white thing on the paper ground, so it reads as a place
// to type. The height is a minimum, so a large font scale makes it grow.

@Composable
private fun SearchBox(
    focused: Boolean,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(Corners.field)
            .background(Surface)
            .border(1.dp, if (focused) Ink else LineControl, Corners.field)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** The editable field of Explore: a lens, the text, and a clear button once there is text. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    SearchBox(focused, modifier) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Ink60, modifier = Modifier.size(24.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = Type.body,
                    color = Ink60,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocusChanged(it.isFocused)
                    }
                    .semantics { contentDescription = placeholder },
                textStyle = Type.body.copy(color = Ink),
                singleLine = true,
                cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    // The 22dp glyph, not its 48dp target, lines up with the field's padding.
                    .offset(x = 12.dp)
                    .size(Touch.target)
                    .pressClickable(onClick = onClear)
                    .semantics { contentDescription = "Limpar busca" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = Ink60, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * The same field on Home, where it is a door and not a place to type: tapping it opens
 * Explore with the keyboard up. Drawn identically so the two never look like different things.
 */
@Composable
fun SearchButton(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBox(
        focused = false,
        modifier = modifier.pressClickable(onClick = onClick, onClickLabel = "Buscar lotes", press = Press.Surface),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Ink60, modifier = Modifier.size(24.dp))
        Text(
            text = placeholder,
            style = Type.body,
            color = Ink60,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
