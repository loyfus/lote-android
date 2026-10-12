package br.com.loyfus.ui.explore

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.loyfus.core.groupedDigits
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.LineControl
import br.com.loyfus.ui.theme.Surface
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized

private const val CURRENCY = "R$ "

/** The most digits a price field takes: nine, a hundred million reais, past anything in the catalogue. */
private const val MAX_DIGITS = 9

/**
 * Shows typed digits as a price while they are typed: `50000` reads `R$ 50.000`. The field's own
 * text stays digits only, so the API never receives a formatted string.
 */
internal object MoneyTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val shown = CURRENCY + groupedDigits(digits)
        return TransformedText(AnnotatedString(shown), Mapping(digits.length, shown))
    }

    private class Mapping(private val length: Int, private val shown: String) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val dots = (1..minOf(offset, length - 1)).count { (length - it) % 3 == 0 }
            return CURRENCY.length + offset + dots
        }

        override fun transformedToOriginal(offset: Int): Int {
            val typed = (offset - CURRENCY.length).coerceIn(0, shown.length - CURRENCY.length)
            return shown.substring(CURRENCY.length).take(typed).count { it != '.' }
        }
    }
}

/**
 * A bound of the price range: a label above, the amount below, digits only. A control, so
 * it has the control's border; the box is a minimum height, so it grows with the text.
 */
@Composable
fun PriceField(
    label: String,
    hint: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(Corners.control)
            .background(Surface)
            .border(1.dp, if (focused) Ink else LineControl, Corners.control)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text = label, style = Type.caption.sized(12.0, 14.0), color = Ink60)
        Box {
            if (digits.isEmpty()) {
                Text(text = hint, style = Type.body.sized(16.0, 22.0, FontWeight.SemiBold), color = Ink60)
            }
            BasicTextField(
                value = digits,
                onValueChange = { typed -> onDigitsChange(typed.filter(Char::isDigit).take(MAX_DIGITS)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label },
                textStyle = Type.body.sized(16.0, 22.0, FontWeight.SemiBold).copy(color = Ink),
                singleLine = true,
                cursorBrush = SolidColor(Ink),
                visualTransformation = MoneyTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
        }
    }
}
