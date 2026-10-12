package br.com.loyfus.core

import java.text.Collator
import java.util.Locale

/** What kind of offer a lot is: an auction with bids, or a direct sale with a price. */
enum class LotKind { Auction, DirectSale }

/** How close a lot is to its deadline. Only an auction can be [Hot]. */
enum class Urgency { Normal, Hot, Ended, None }

/** Seconds in the last day: from there on an auction wears the accent. */
const val HOT_WINDOW_SECONDS = 86_400L

/** Seconds in a week: the Saved list's "this week" group ends here. */
const val WEEK_SECONDS = 7 * 86_400L

fun kindOf(modality: String?): LotKind =
    if (modality == "AFTER_MARKET") LotKind.DirectSale else LotKind.Auction

/**
 * The deadline state of a lot. The API sends -1 for no deadline and 0 once it has passed, so
 * "ended" needs a deadline to have passed *from*: a lot with no `endsAt` never ends.
 */
fun urgencyOf(kind: LotKind, secondsToClose: Long, hasDeadline: Boolean): Urgency = when {
    secondsToClose > 0 ->
        if (kind == LotKind.Auction && secondsToClose <= HOT_WINDOW_SECONDS) Urgency.Hot else Urgency.Normal
    hasDeadline -> Urgency.Ended
    else -> Urgency.None
}

/** The groups of the Saved list, in the order they are shown. */
enum class SavedGroup(val title: String) {
    Today("Encerram hoje"),
    Week("Esta semana"),
    Later("Mais tarde"),
    Ended("Encerrados"),
}

/**
 * Where a saved lot belongs: an auction in its last day is "today", one within the week "this
 * week", an ended one "ended", everything else "later". A direct sale has no clock to watch.
 */
fun savedGroupOf(kind: LotKind, urgency: Urgency, secondsToClose: Long): SavedGroup = when {
    urgency == Urgency.Ended -> SavedGroup.Ended
    kind == LotKind.DirectSale || urgency == Urgency.None -> SavedGroup.Later
    urgency == Urgency.Hot -> SavedGroup.Today
    secondsToClose <= WEEK_SECONDS -> SavedGroup.Week
    else -> SavedGroup.Later
}

/** The label above a price: what the number is. */
fun priceLabel(kind: LotKind, urgency: Urgency, bidCount: Int): String = when {
    kind == LotKind.DirectSale -> "Preço"
    urgency == Urgency.Ended -> "Lance final"
    bidCount > 0 -> "Lance atual"
    else -> "Lance inicial"
}

/**
 * The same label for the sticky bar's caption, where it is lower case and a running
 * auction adds its time: "lance atual · 4d 15h", "preço".
 */
fun stickyCaption(kind: LotKind, urgency: Urgency, bidCount: Int, secondsToClose: Long): String {
    val label = priceLabel(kind, urgency, bidCount).lowercase(Locale.ROOT)
    val running = kind == LotKind.Auction && secondsToClose > 0
    return if (running) "$label · ${timeLeft(secondsToClose)}" else label
}

// The entries the specification fixes at the head of every ficha técnica. The API
// sends them in no meaningful order, and the two clients used to disagree.
private val SPEC_ORDER = listOf(
    "Marca", "Modelo", "Ano Modelo", "Ano Fabricação", "Cor", "Placa", "Combustível",
    "Câmbio", "Quilometragem", "Valor Tabela FIPE", "Deságio", "Área do Terreno",
    "Área Construída", "Quartos", "Banheiros", "Vagas", "Endereço", "Número da Matrícula",
    "Subtipo", "Condições de Pagamento",
)

/** The ficha técnica in reading order: the fixed list first, then the rest alphabetically (pt-BR). */
fun specOrder(specs: Map<String, String>): List<Pair<String, String>> {
    val collator = Collator.getInstance(Locale.Builder().setLanguage("pt").setRegion("BR").build())
    val known = SPEC_ORDER.mapNotNull { key -> specs[key]?.let { key to it } }
    val rest = specs.entries
        .filter { it.key !in SPEC_ORDER }
        .sortedWith { a, b -> collator.compare(a.key, b.key) }
        .map { it.key to it.value }
    return known + rest
}

/** The commission among the purchase conditions, as a label and a value ("Comissão do leilão", "5%"). */
fun commissionOf(conditions: List<String>): Pair<String, String>? {
    val line = conditions.firstOrNull { it.startsWith("Comiss", ignoreCase = true) } ?: return null
    val label = line.substringBefore(':').trim()
    val value = line.substringAfter(':', "").trim()
    return if (value.isEmpty()) "Comissão" to line else label to value
}

/** `commercialCondition` split on `|`, one item per line. */
fun purchaseConditions(commercialCondition: String?): List<String> =
    commercialCondition.orEmpty().split('|').map { it.trim() }.filter { it.isNotEmpty() }

// Short category labels from the specification's table; a name with no entry shows
// as the API wrote it. Matched after trim(): the API keeps stray spaces in some names
// ("Sucatas , Materiais & Resíduos", "Partes e Peças ").
private val SHORT_LABELS = mapOf(
    "Caminhões & Ônibus" to "Caminhões",
    "Máquinas Pesadas & Agrícolas" to "Máquinas pesadas",
    "Movimentação & Transporte" to "Movimentação",
    "Industrial, Máquinas & Equipamentos" to "Indústria",
    "Móveis e Decoração" to "Móveis",
    "Bolsas, Canetas, Joias e Relógios" to "Joias e relógios",
    "Sucatas , Materiais & Resíduos" to "Sucatas",
    "Embarcações & Aeronaves" to "Barcos e aviões",
    "Eletrodomésticos" to "Eletro",
    "Materiais para Construção Civil" to "Construção",
    "Cozinhas e Restaurantes" to "Cozinhas",
    "Alimentos e Bebidas" to "Alimentos",
    "Artes, Decoração & Colecionismo" to "Arte e coleção",
)

fun categoryShortLabel(name: String): String = SHORT_LABELS[name.trim()] ?: name.trim()
