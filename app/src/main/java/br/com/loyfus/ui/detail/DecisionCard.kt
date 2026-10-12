package br.com.loyfus.ui.detail

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import br.com.loyfus.core.HOT_WINDOW_SECONDS
import br.com.loyfus.core.LotKind
import br.com.loyfus.core.Urgency
import br.com.loyfus.core.bids
import br.com.loyfus.core.endLabel
import br.com.loyfus.core.participants
import br.com.loyfus.core.price
import br.com.loyfus.core.priceLabel
import br.com.loyfus.core.timeLeft
import br.com.loyfus.core.timeLeftLive
import br.com.loyfus.core.urgencyOf
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.components.touchOverflow
import br.com.loyfus.ui.theme.AmberText
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.PaperDim
import br.com.loyfus.ui.theme.Surface
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized
import kotlinx.coroutines.delay

/**
 * Seconds left on the deadline, from the moment the lot was read, ticking while the screen is on
 * and unpaused. The device clock's time, not a counter decremented by timers, so a screen that
 * was in the background shows the real remaining time at once.
 */
@Composable
internal fun rememberLiveSeconds(initial: Long, readAtElapsed: Long, paused: Boolean): Long {
    fun remaining() = initial - (SystemClock.elapsedRealtime() - readAtElapsed) / 1_000
    var seconds by remember(initial, readAtElapsed) { mutableLongStateOf(if (initial > 0) remaining() else initial) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(paused, initial, readAtElapsed) {
        if (paused || initial <= 0) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                seconds = remaining()
                if (seconds <= 0) break
                delay(untilNextChange(seconds, SystemClock.elapsedRealtime() - readAtElapsed))
            }
        }
    }
    return seconds
}

/**
 * Milliseconds until the text for [seconds] left would read differently: every second inside the
 * last ten minutes, every minute before that. A countdown with hours to go has no reason to
 * redraw sixty times a minute.
 */
internal fun untilNextChange(seconds: Long, sinceReadMillis: Long): Long {
    val intoSecond = sinceReadMillis % 1_000
    val wholeSeconds = if (seconds < 600) 1L else seconds % 60 + 1
    return (wholeSeconds * 1_000 - intoSecond).coerceAtLeast(250)
}

/**
 * The decision card: what the lot costs, how contested it is, how long is left. The loudest block
 * on the screen and the only place amber appears at size, and only under a day to the deadline.
 *
 * The countdown ticks, so it can be paused (WCAG 2.2.2); pausing hides nothing, since the
 * absolute end date is always on the card. There is deliberately no live region: announcing every
 * second would make the screen unusable with a screen reader.
 */
@Composable
fun DecisionCard(
    model: DetailModel,
    seconds: Long,
    paused: Boolean,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lot = model.lot
    val urgency = urgencyOf(model.kind, seconds, hasDeadline = lot.endsAt != null)
    val hot = urgency == Urgency.Hot
    val accent = if (hot) AmberText else Ink

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Corners.decision)
            .background(Surface)
            .border(1.dp, Line, Corners.decision)
            .padding(18.dp),
    ) {
        Text(
            text = priceLabel(model.kind, urgency, lot.bidCount),
            style = Type.small.sized(13.0, 18.0, FontWeight.Medium),
            color = Ink60,
        )
        HeroPrice(text = price(lot.currentPrice), color = if (lot.currentPrice == null) Ink60 else accent)
        Text(
            text = when (model.kind) {
                LotKind.Auction -> "${bids(lot.bidCount)} · ${participants(lot.bidderCount)}"
                LotKind.DirectSale -> "Compra direta, sem disputa de lances"
            },
            style = Type.small,
            color = Ink60,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (urgency != Urgency.None) {
            Box(Modifier.padding(vertical = 16.dp).fillMaxWidth().height(1.dp).background(Line))
            val running = seconds > 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Ink60, modifier = Modifier.size(22.dp))
                Text(
                    text = when {
                        !running -> "Encerrado"
                        model.kind == LotKind.DirectSale -> "Disponível por ${timeLeft(seconds)}"
                        else -> "Encerra em ${timeLeftLive(seconds)}"
                    },
                    style = Type.button.sized(18.0, 24.0),
                    color = if (hot) AmberText else if (running) Ink else Ink60,
                    modifier = Modifier.weight(1f),
                )
                if (running) {
                    Box(
                        modifier = Modifier
                            // The glyph, not its 48dp target, lines up with the card's padding.
                            .offset(x = 12.dp)
                            .touchOverflow(40.dp)
                            .size(Touch.target)
                            .pressClickable(onClick = onTogglePause)
                            .semantics {
                                contentDescription =
                                    if (paused) "Retomar contagem regressiva" else "Pausar contagem regressiva"
                                stateDescription = if (paused) "pausada" else "em contagem"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Crossfade(targetState = paused, animationSpec = Motion.fast(), label = "pause") { isPaused ->
                            Icon(
                                imageVector = if (isPaused) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                                contentDescription = null,
                                tint = Ink60,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
            endLabel(lot.endsAt)?.let {
                Text(
                    text = it,
                    style = Type.small,
                    color = Ink60,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp),
                )
            }
            if (hot) {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(PaperDim),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((seconds.toFloat() / HOT_WINDOW_SECONDS).coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(AmberText),
                    )
                }
            }
        }
    }
}

/**
 * The price, on one line at any text size: at the reader's font scale the figure can be wider than
 * the card, so it shrinks until it fits rather than wrapping under its own symbol. Never above the
 * specified 34sp, never below 20sp.
 */
@Composable
private fun HeroPrice(text: String, color: Color) {
    BasicText(
        text = text,
        style = Type.priceHero.copy(color = color, lineHeight = TextUnit.Unspecified),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 34.sp, stepSize = 0.5.sp),
        modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
    )
}
