package br.com.loyfus.core

import java.util.Locale
import java.util.regex.Pattern

/** A lot title cleaned for display, and what the source wrote after the first " | ". */
data class TidyTitle(val headline: String, val detail: String?)

// The catalogue title comes from the auctioneer: 76% in capitals, 28% with a
// "(Ref.: 53137)" tail, 5% with " | " between title and address. The backend keeps
// delivering the raw text (it stays the source of truth); presenting it is the
// client's job, and this is a line-for-line port of docs/design-v3/tidy_title.py,
// checked against the 1,566 real titles of tidy_title_golden.tsv.
//
// Python's `str` regexes are Unicode-aware where Java's are not, so every `\s`, `\w`,
// `\d` and `\b` of the reference is spelled out as the class it stands for.

/** Portuguese connectives: lower case, except as the first word. */
private val CONNECTORS = setOf(
    "de", "da", "do", "das", "dos", "e", "em", "no", "na", "nos", "nas", "com", "para",
    "por", "as", "os", "um", "uma", "ao", "aos", "à", "á", "sem", "sob", "ou", "que",
    "até", "pelo", "pela", "entre", "sobre",
)

/**
 * Short words (up to three letters) that must become "Kit", "Aço", "Ano"; the rest of
 * the short tokens in the catalogue are codes (ELX, FIV, GL, LED, ID) and stay in capitals.
 */
private val WORDS3 = setOf(
    "ano", "kit", "cor", "aço", "aco", "não", "uso", "par", "max", "baú", "pá", "fio", "luz",
    "ar", "gol", "air", "sul", "new", "som", "aro", "uno", "yo", "gás", "mãe", "pés", "pó",
    "po", "ser", "fit", "pro", "mil", "box", "pão", "mão", "óleo", "cal", "sal", "rua",
    "ovo", "boi", "tia", "sol", "mar", "rio", "bar", "som", "lar", "pia", "cão", "gato",
    "kia", "imp", "ref", "gir", "dia", "vez", "três", "dez", "bem", "mal", "bom", "sim", "ter", "lei", "mês", "fim",
    "alt", "lado", "pé", "pés", "rei", "tom", "top", "seu", "sua", "meu", "uma", "tubo",
)

/**
 * Short acronyms that cannot become "Suv" or "Abs". Tokens of up to three letters with
 * no vowel (VW, GM, LS, GL, KG, HP, CV...) already stay in capitals by the general rule.
 */
private val ACRONYMS = setOf(
    "SUV", "ABS", "GPS", "LED", "LCD", "USB", "TV", "PC", "CD", "DVD", "HD", "SSD", "RAM",
    "ZF", "AT", "MT", "TSI", "GLS", "GLX", "EX", "EXL", "XEI", "CVT", "DSG", "AWD", "KVA",
    "CNH", "CNPJ", "CPF", "IPVA", "IPTU", "RG", "ME", "EPP", "UTI", "MDF", "PVC", "EPI",
    "NF", "DUT", "ID", "II", "III", "IV", "VI", "VII", "VIII", "IX", "XI", "XII",
)

/** State codes: "Rosário Do Sul-RS" and "(SP)" cannot become "Rs" and "Sp". */
private val UFS = setOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
    "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
)

private const val VOWELS = "AEIOUÁÀÂÃÉÊÍÓÔÕÚ"
private const val TRIM = " ,-–"
private val ROMAN = Regex("^[IVX]+$")

// Python's `\s` on a str: every Unicode space, plus the C0 separators and NEL.
private const val SPACE = "\\t\\n\\x0B\\f\\r \\x1C-\\x1F\\x85\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000"

// Python's `\w` and `\d`: letters, numbers and underscore; decimal digits.
private const val WORD = "[\\p{L}\\p{N}_]"
private const val NOT_AFTER_WORD = "(?<!$WORD)"
private const val NOT_BEFORE_WORD = "(?!$WORD)"

private val CASELESS = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE

// "(Ref.: 53137)" and its leading separators.
private val REF = Pattern.compile("[,$SPACE\\-]*\\([$SPACE]*ref\\.?[$SPACE]*:?[^)]*\\)", CASELESS)

// Codes that only mean something to the auctioneer: "ID: #3614548", "ID #3753852/EL".
private val SOURCE_ID = Pattern.compile(
    "[$SPACE,\\-]*${NOT_AFTER_WORD}ID:?[$SPACE]*#$WORD+(?:/$WORD+)?[$SPACE]*$",
    CASELESS,
)

// "(#175MAGO)".
private val HASH_CODE = Pattern.compile("[$SPACE]*\\([$SPACE]*#[^)]*\\)")

// A dangling "CH.:" (chassis) label with nothing after it.
private val CH = Pattern.compile(",?[$SPACE]*CH\\.?:?[$SPACE]*(?=$|,)", CASELESS)

// "2010 2011" -> "2010/2011".
private val YEARS = Pattern.compile(
    "$NOT_AFTER_WORD((?:19|20)\\p{Nd}\\p{Nd}) ((?:19|20)\\p{Nd}\\p{Nd})$NOT_BEFORE_WORD",
)

private val ALLCAPS_WORD = Pattern.compile("$NOT_AFTER_WORD[A-ZÁÀÂÃÉÊÍÓÔÕÚÇ]{4,}$NOT_BEFORE_WORD")
private val SPACE_RUN = Pattern.compile("[$SPACE]+")
private val MOJIBAKE_DASH = Pattern.compile("[$SPACE]¿[$SPACE]")

/** `str.isspace()` of the reference. */
private fun isSpace(ch: Char) =
    ch.isWhitespace() || Character.isSpaceChar(ch) || ch in '\u001C'..'\u001F' || ch == '\u0085'

/** `str.isdigit()` of the reference: decimal digits plus the superscript and subscript ones. */
private fun isDigit(ch: Char) =
    Character.isDigit(ch) || ch in "²³¹⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉"

private fun isSeparator(ch: Char) = isSpace(ch) || ch in "/-.,;:()+&|"

private fun isMostlyUpper(text: String): Boolean {
    var letters = 0
    var upper = 0
    var index = 0
    while (index < text.length) {
        val cp = text.codePointAt(index)
        if (Character.isLetter(cp)) {
            letters++
            if (Character.isUpperCase(cp)) upper++
        }
        index += Character.charCount(cp)
    }
    return letters >= 4 && upper.toDouble() / letters >= 0.7
}

private fun capitalized(word: String): String =
    word.substring(0, 1).uppercase(Locale.ROOT) + word.substring(1).lowercase(Locale.ROOT)

/** One word, without surrounding punctuation, of a title written in capitals. */
private fun fixWord(word: String, first: Boolean): String {
    if (word.isEmpty()) return word
    // 1.0MT, 4X4, 16VH, XEI20FLEX: model codes stay as they are.
    if (word.any(::isDigit)) return word
    val upper = word.uppercase(Locale.ROOT)
    val lower = word.lowercase(Locale.ROOT)
    return when {
        lower in CONNECTORS -> if (first) capitalized(word) else lower
        upper in ACRONYMS || upper in UFS || ROMAN.matches(upper) -> upper
        lower in WORDS3 -> capitalized(word)
        // VW, GM, GL, ELX, FIV, LED, KG: a code, not a word.
        word.codePointCount(0, word.length) <= 3 -> upper
        // CSLC, KLTS: with no vowel it is not a word either.
        upper.none { it in VOWELS } -> upper
        else -> capitalized(word)
    }
}

private fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var first = true
    var index = 0
    while (index < text.length) {
        if (isSeparator(text[index])) {
            out.append(text[index])
            index++
            continue
        }
        var end = index
        while (end < text.length && !isSeparator(text[end])) end++
        out.append(fixWord(text.substring(index, end), first))
        first = false
        index = end
    }
    return out.toString()
}

/** A mixed-case title ("Máquina Antiga OLIVETTI LINEA 98"): only the shouted words change. */
private fun softenCapsWords(text: String): String {
    val matcher = ALLCAPS_WORD.matcher(text)
    val out = StringBuilder()
    var last = 0
    while (matcher.find()) {
        out.append(text, last, matcher.start())
        out.append(fixWord(matcher.group(), first = false))
        last = matcher.end()
    }
    return out.append(text, last, text.length).toString()
}

private fun normalise(piece: String): String {
    val joined = YEARS.matcher(piece).replaceAll("$1/$2")
    return if (isMostlyUpper(joined)) titleCase(joined) else softenCapsWords(joined)
}

/**
 * The title to show and the detail the source put after the first " | ". Display only:
 * the model keeps the raw text.
 */
fun tidyTitle(raw: String): TidyTitle {
    var text = raw.replace(' ', ' ')
    // The source's encoding turned the dash into "¿".
    text = MOJIBAKE_DASH.matcher(text).replaceAll(" – ").replace("¿", "")
    text = REF.matcher(text).replaceAll("")
    text = CH.matcher(text).replaceAll("")
    text = HASH_CODE.matcher(text).replaceAll("")
    text = SOURCE_ID.matcher(text).replaceAll("")
    text = SPACE_RUN.matcher(text).replaceAll(" ").trim { it in TRIM }

    val cut = text.indexOf(" | ")
    val head = if (cut < 0) text else text.substring(0, cut)
    val rest = if (cut < 0) "" else text.substring(cut + 3)
    val detail = rest.trim().takeIf { it.isNotEmpty() }?.let { normalise(it).replace(" | ", " · ") }
    return TidyTitle(normalise(head).trim { it in TRIM }, detail)
}

/**
 * The real place of a lot, when its title carries one (" | Cidade-UF"): the first piece
 * of the title detail that is a city and a state code. The catalogue's own city and state
 * fields are the auction event's address, not the item's, so they are never used for this.
 */
fun place(detail: String?): String? {
    if (detail == null) return null
    return detail.split(" · ").firstNotNullOfOrNull { PLACE.matchEntire(it)?.groupValues?.get(1) }
}

private val PLACE = Regex("^(.+?-[A-Z]{2})$")
