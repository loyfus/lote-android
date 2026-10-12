package br.com.lote.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

// The strings a reader actually sees, written once and tested against the same
// vectors as the iOS client (docs/DESIGN_SYSTEM.md, section 4). Hand-rolled instead
// of delegating to the platform's number and date formatters: those differ between
// the JVM the tests run on and the ICU on the device, and a price must read the same
// in both.

private const val NBSP = ' '

/** Brazilian auctions seal the opening bid on some lots; showing "R$ 0" would be a lie. */
const val PRICE_ON_REQUEST = "Sob consulta"

/**
 * Full price: `R$ 7.637,80`, `R$ 81.000`. Cents only when they are not zero. The gap
 * after the symbol is a no-break space, so a price can never wrap away from its currency.
 */
fun price(value: Double?): String {
    val exact = exactDecimal(value) ?: return PRICE_ON_REQUEST
    return "R$$NBSP" + brazilian(exact.setScale(2, RoundingMode.HALF_UP))
}

/** `price` below a million; from there on `R$ 12,5 mi`, with the `,0` dropped. */
fun priceShort(value: Double?): String {
    val exact = exactDecimal(value) ?: return PRICE_ON_REQUEST
    if (exact < MILLION) return price(value)
    val millions = exact.movePointLeft(6).setScale(1, RoundingMode.HALF_UP)
    return "R$$NBSP" + brazilian(millions) + " mi"
}

/** Whole-number count with thousands separators: `4.691`. */
fun groupedCount(value: Long): String = group(value.toString())

/** The same grouping for a string of digits that is still being typed: `50000` is `50.000`. */
fun groupedDigits(digits: String): String = group(digits)

/** Time left, as much precision as it is worth: `35d`, `4d 15h`, `5h 20min`, `42min`. */
fun timeLeft(seconds: Long): String {
    if (seconds <= 0) return "Encerrado"
    val days = seconds / 86_400
    val hours = seconds % 86_400 / 3_600
    val minutes = seconds % 3_600 / 60
    return when {
        days >= 10 -> "${days}d"
        days >= 1 -> "${days}d ${hours}h"
        hours >= 1 -> "${hours}h ${minutes}min"
        minutes >= 1 -> "${minutes}min"
        else -> "<1min"
    }
}

/** `timeLeft` for the screen that ticks: seconds appear once they matter. */
fun timeLeftLive(seconds: Long): String = when {
    seconds <= 0 -> "Encerrado"
    seconds < 60 -> "${seconds}s"
    seconds < 600 -> "${seconds / 60}min ${seconds % 60}s"
    else -> timeLeft(seconds)
}

/**
 * Wall-clock end in the device's zone: `qui, 15 out · 14:12`. The year only appears
 * when it is not the current one. Abbreviations carry no dots.
 */
fun endLabel(
    iso: String?,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
): String? {
    val moment = parseInstant(iso)?.atZone(zone) ?: return null
    val year = if (moment.year != now.atZone(zone).year) " ${moment.year}" else ""
    return "${WEEKDAYS[moment.dayOfWeek.value - 1]}, ${moment.dayOfMonth} " +
        "${MONTHS[moment.monthValue - 1]}$year · ${clock(moment.hour, moment.minute)}"
}

/** `HH:mm` of an instant in the device's zone: when a value was last read. */
fun clockTime(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String? {
    val moment = parseInstant(iso)?.atZone(zone) ?: return null
    return clock(moment.hour, moment.minute)
}

fun bids(count: Int): String = when (count) {
    0 -> "Sem lances"
    1 -> "1 lance"
    else -> "$count lances"
}

fun participants(count: Int): String =
    if (count == 1) "1 participante" else "$count participantes"

/** How long ago: `agora`, `há 12 min`, `há 3 h`, `há 2 d`. A timestamp in the future reads as now. */
fun ago(iso: String?, now: Instant = Instant.now()): String? {
    val then = parseInstant(iso) ?: return null
    val seconds = Duration.between(then, now).seconds
    return when {
        seconds < 60 -> "agora"
        seconds < 3_600 -> "há ${seconds / 60} min"
        seconds < 86_400 -> "há ${seconds / 3_600} h"
        else -> "há ${seconds / 86_400} d"
    }
}

/** Minutes since a timestamp, for the freshness dot; `null` when it cannot be read. */
fun minutesSince(iso: String?, now: Instant = Instant.now()): Long? =
    parseInstant(iso)?.let { Duration.between(it, now).toMinutes() }

/**
 * A money amount for a label with room for a few characters: `1 mil`, `50 mil`, `1,5 mi`. The
 * price tag and the shortcuts write the symbol once for the whole range.
 */
fun compactAmount(value: Long): String = when {
    value >= 1_000_000 -> decimalCompact(BigDecimal(value).movePointLeft(6)) + " mi"
    value >= 1_000 -> decimalCompact(BigDecimal(value).movePointLeft(3)) + " mil"
    else -> value.toString()
}

/**
 * An amount the reader typed, as a tag: compact when the compact form is exact, grouped digits
 * otherwise. A tag that rounds the reader's own number would say something they did not ask for.
 */
fun filterAmount(value: Long): String = when {
    value >= 1_000_000 && value % 100_000 == 0L -> compactAmount(value)
    value in 1_000..999_999 && value % 100 == 0L -> compactAmount(value)
    else -> groupedCount(value)
}

/** The price filter as a tag: `R$ 1 mil – 50 mil`, `Até R$ 1 mil`, `Acima de R$ 200 mil`. */
fun priceRangeLabel(min: Long?, max: Long?): String? = when {
    min != null && max != null -> "R\$$NBSP${filterAmount(min)} – ${filterAmount(max)}"
    max != null -> "Até R\$$NBSP${filterAmount(max)}"
    min != null -> "Acima de R\$$NBSP${filterAmount(min)}"
    else -> null
}

private fun decimalCompact(value: BigDecimal): String =
    value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',').removeSuffix(",0")

private val MILLION = BigDecimal(1_000_000)

private val WEEKDAYS = arrayOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
private val MONTHS = arrayOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

private fun clock(hour: Int, minute: Int) =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

/** The shortest decimal that round-trips the double, so 7637.8 is 7637.8 and not 7637.79999…. */
private fun exactDecimal(value: Double?): BigDecimal? =
    if (value == null || !value.isFinite()) null else BigDecimal.valueOf(value)

/** Brazilian grouping and decimal comma; a fraction that is all zeros is dropped. */
private fun brazilian(value: BigDecimal): String {
    val plain = value.abs().toPlainString()
    val integer = plain.substringBefore('.')
    val fraction = plain.substringAfter('.', "")
    val sign = if (value.signum() < 0) "-" else ""
    return if (fraction.any { it != '0' }) "$sign${group(integer)},$fraction" else "$sign${group(integer)}"
}

private fun group(digits: String): String {
    val out = StringBuilder()
    digits.forEachIndexed { index, digit ->
        if (index > 0 && (digits.length - index) % 3 == 0) out.append('.')
        out.append(digit)
    }
    return out.toString()
}

private fun parseInstant(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(iso).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
}
