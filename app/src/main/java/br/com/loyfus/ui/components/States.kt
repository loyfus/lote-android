package br.com.loyfus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.PaperDim
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Type

/**
 * A screen with nothing to show: a quiet disc and a glyph saying what is missing, a title, a line
 * of guidance and the action that gets out of it. Empty and failed never share a screen:
 * [ErrorState] says what went wrong and offers a retry, this says there is nothing and offers a
 * way to find something.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x3, vertical = Space.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(PaperDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Ink60, modifier = Modifier.size(28.dp))
        }
        Text(
            text = title,
            style = Type.headline,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.lg).semantics { heading() },
        )
        Text(
            text = message,
            style = Type.body,
            color = Ink60,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.sm).widthIn(max = 320.dp),
        )
        if (action != null) {
            Box(Modifier.padding(top = Space.xl)) { action() }
        }
    }
}

/** The same screen for a request that failed: what happened, and a way to try again. */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Não foi possível carregar",
) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        title = title,
        message = message,
        modifier = modifier,
        action = { SecondaryButton(label = "Tentar novamente", onClick = onRetry, icon = Icons.Filled.Refresh) },
    )
}
