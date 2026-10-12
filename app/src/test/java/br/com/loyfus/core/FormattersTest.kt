package br.com.loyfus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * The strings a reader actually sees: every vector of docs/DESIGN_SYSTEM.md section 4,
 * which the iOS client tests too, plus the edges between them. Each one was wrong at
 * some point, and "R$ 0,00" for a lot the auctioneer never priced is worse than an
 * honest blank.
 */
class FormattersTest {

    private val nbsp = " "
    private fun brl(value: String) = "R\$$nbsp$value"

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    // ---- price ----

    @Test
    fun `price vectors of the specification`() {
        assertEquals("Sob consulta", price(null))
        assertEquals(brl("7.637,80"), price(7637.8))
        assertEquals(brl("81.000"), price(81000.0))
        assertEquals(brl("0,30"), price(0.3))
        assertEquals(brl("12.510.000"), price(12510000.0))
    }

    @Test
    fun `price shows cents only when they are not zero`() {
        assertEquals(brl("330.947,11"), price(330947.11))
        assertEquals(brl("6.200,50"), price(6200.5))
        assertEquals(brl("0,01"), price(0.01))
        assertEquals(brl("1.000"), price(1000.0))
        assertEquals(brl("999,99"), price(999.99))
        // Rounded to the cent first: 0.999 is a whole real, not "R$ 0,99".
        assertEquals(brl("1"), price(0.999))
    }

    @Test
    fun `the gap after the currency symbol never breaks`() {
        assertEquals(' ', price(1.0)[2])
        assertEquals(' ', priceShort(2_000_000.0)[2])
    }

    @Test
    fun `an unreadable number is a price on request, not a crash`() {
        assertEquals("Sob consulta", price(Double.NaN))
        assertEquals("Sob consulta", priceShort(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `priceShort vectors of the specification`() {
        assertEquals("Sob consulta", priceShort(null))
        assertEquals(price(7637.8), priceShort(7637.8))
        assertEquals(price(81000.0), priceShort(81000.0))
        assertEquals(price(999_999.99), priceShort(999_999.99))
        assertEquals(brl("12,5 mi"), priceShort(12510000.0))
        assertEquals(brl("1,6 mi"), priceShort(1573818.2))
        assertEquals(brl("2 mi"), priceShort(2000000.0))
    }

    @Test
    fun `priceShort rounds the million half up and drops a zero decimal`() {
        assertEquals(brl("1 mi"), priceShort(1_000_000.0))
        assertEquals(brl("1 mi"), priceShort(1_049_999.0))
        assertEquals(brl("1,1 mi"), priceShort(1_050_000.0))
        assertEquals(brl("2 mi"), priceShort(1_950_000.0))
        assertEquals(brl("42,5 mi"), priceShort(42_548_477.32))
    }

    @Test
    fun `counts are grouped like the prices beside them`() {
        assertEquals("4.691", groupedCount(4691))
        assertEquals("999", groupedCount(999))
        assertEquals("1.298.000", groupedCount(1_298_000))
        assertEquals("0", groupedCount(0))
    }

    @Test
    fun `compact amounts for the price tag`() {
        assertEquals("500", compactAmount(500))
        assertEquals("1 mil", compactAmount(1_000))
        assertEquals("1,5 mil", compactAmount(1_500))
        assertEquals("50 mil", compactAmount(50_000))
        assertEquals("200 mil", compactAmount(200_000))
        assertEquals("1 mi", compactAmount(1_000_000))
    }

    // ---- time ----

    private fun secs(days: Int = 0, hours: Int = 0, minutes: Int = 0, seconds: Int = 0): Long =
        days * 86_400L + hours * 3_600L + minutes * 60L + seconds

    @Test
    fun `timeLeft vectors of the specification`() {
        assertEquals("Encerrado", timeLeft(0))
        assertEquals("Encerrado", timeLeft(-1))
        assertEquals("35d", timeLeft(secs(days = 35, hours = 7)))
        assertEquals("4d 15h", timeLeft(secs(days = 4, hours = 15, minutes = 20)))
        assertEquals("5h 20min", timeLeft(secs(hours = 5, minutes = 20)))
        assertEquals("42min", timeLeft(secs(minutes = 42)))
        assertEquals("<1min", timeLeft(48))
    }

    @Test
    fun `timeLeft changes unit exactly at the boundaries`() {
        assertEquals("10d", timeLeft(secs(days = 10)))
        assertEquals("9d 23h", timeLeft(secs(days = 9, hours = 23, minutes = 59, seconds = 59)))
        assertEquals("1d 0h", timeLeft(secs(days = 1)))
        assertEquals("23h 59min", timeLeft(secs(days = 1) - 1))
        assertEquals("1h 0min", timeLeft(secs(hours = 1)))
        assertEquals("59min", timeLeft(secs(hours = 1) - 1))
        assertEquals("1min", timeLeft(60))
        assertEquals("<1min", timeLeft(59))
        assertEquals("<1min", timeLeft(1))
    }

    @Test
    fun `timeLeftLive vectors of the specification`() {
        assertEquals("Encerrado", timeLeftLive(0))
        assertEquals("3min 12s", timeLeftLive(secs(minutes = 3, seconds = 12)))
        assertEquals("48s", timeLeftLive(48))
        // From ten minutes on it reads like timeLeft.
        assertEquals("10min", timeLeftLive(secs(minutes = 10)))
        assertEquals("9min 59s", timeLeftLive(secs(minutes = 9, seconds = 59)))
        assertEquals("5h 20min", timeLeftLive(secs(hours = 5, minutes = 20)))
        assertEquals("4d 15h", timeLeftLive(secs(days = 4, hours = 15)))
        assertEquals("1s", timeLeftLive(1))
        assertEquals("1min 0s", timeLeftLive(60))
    }

    @Test
    fun `endLabel vectors of the specification`() {
        val now = Instant.parse("2026-10-10T12:00:00Z")
        // Thursday, in the device's zone, no dots, the year only when it is not this one.
        assertEquals("qui, 15 out · 14:12", endLabel("2026-10-15T17:12:30Z", saoPaulo, now))
        assertEquals("sex, 15 out 2027 · 14:12", endLabel("2027-10-15T17:12:30Z", saoPaulo, now))
    }

    @Test
    fun `endLabel follows the zone of the device`() {
        val now = Instant.parse("2026-10-10T12:00:00Z")
        val iso = "2026-10-15T01:30:00Z"
        assertEquals("qua, 14 out · 22:30", endLabel(iso, saoPaulo, now))
        assertEquals("qui, 15 out · 01:30", endLabel(iso, ZoneId.of("UTC"), now))
    }

    @Test
    fun `endLabel names every weekday and month without a dot`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val utc = ZoneId.of("UTC")
        assertEquals("seg, 5 jan · 09:05", endLabel("2026-01-05T09:05:00Z", utc, now))
        assertEquals("dom, 11 jan · 00:00", endLabel("2026-01-11T00:00:00Z", utc, now))
        assertEquals("sáb, 28 fev · 23:59", endLabel("2026-02-28T23:59:00Z", utc, now))
        val months = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
        months.forEachIndexed { index, name ->
            val month = (index + 1).toString().padStart(2, '0')
            val label = endLabel("2026-$month-15T12:00:00Z", utc, now)!!
            assertTrue(label, label.contains(" 15 $name "))
            assertFalse(label, label.contains('.'))
        }
    }

    @Test
    fun `endLabel and clockTime say nothing for a missing or broken date`() {
        assertNull(endLabel(null))
        assertNull(endLabel(""))
        assertNull(endLabel("amanhã"))
        assertNull(clockTime(null))
        assertEquals("14:12", clockTime("2026-10-15T17:12:30Z", saoPaulo))
    }

    @Test
    fun `ago vectors of the specification`() {
        val now = Instant.parse("2026-10-10T12:00:00Z")
        assertEquals("agora", ago("2026-10-10T11:59:30Z", now))
        assertEquals("há 12 min", ago("2026-10-10T11:48:00Z", now))
        assertEquals("há 3 h", ago("2026-10-10T09:00:00Z", now))
        assertEquals("há 2 d", ago("2026-10-08T12:00:00Z", now))
    }

    @Test
    fun `ago changes unit exactly at the boundaries`() {
        val now = Instant.parse("2026-10-10T12:00:00Z")
        fun back(seconds: Long) = now.minusSeconds(seconds).toString()
        assertEquals("agora", ago(back(0), now))
        assertEquals("agora", ago(back(59), now))
        assertEquals("há 1 min", ago(back(60), now))
        assertEquals("há 59 min", ago(back(3_599), now))
        assertEquals("há 1 h", ago(back(3_600), now))
        assertEquals("há 23 h", ago(back(86_399), now))
        assertEquals("há 1 d", ago(back(86_400), now))
    }

    @Test
    fun `ago never reports the future and reads the API's microseconds`() {
        val now = Instant.parse("2026-10-10T12:00:00Z")
        assertEquals("agora", ago("2026-10-10T12:05:00Z", now))
        assertEquals("há 4 h", ago("2026-10-10T07:59:59.442815Z", now))
        assertEquals("há 4 h", ago("2026-10-10T05:00:00-03:00", now))
        assertNull(ago(null, now))
        assertNull(ago("ontem", now))
    }

    // ---- counts ----

    @Test
    fun `bids and participants agree in number`() {
        assertEquals("Sem lances", bids(0))
        assertEquals("1 lance", bids(1))
        assertEquals("104 lances", bids(104))
        assertEquals("0 participantes", participants(0))
        assertEquals("1 participante", participants(1))
        assertEquals("2 participantes", participants(2))
    }
}
