package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.AreaRange

/**
 * Parses Polish-language area phrases such as:
 * - "87,43 m2"          -> single value (min == max)
 * - "do 363 m2"         -> upper bound only
 * - "od 75 do 79 m2"    -> explicit range
 */
object PolishAreaFormat {

    private val RANGE = Regex("od\\s+([0-9]+(?:[.,][0-9]+)?)\\s+do\\s+([0-9]+(?:[.,][0-9]+)?)")
    private val UPPER_BOUND = Regex("do\\s+([0-9]+(?:[.,][0-9]+)?)")
    private val SINGLE_VALUE = Regex("([0-9]+(?:[.,][0-9]+)?)")

    fun parse(text: String): AreaRange? {
        RANGE.find(text)?.let { match ->
            val (min, max) = match.destructured
            return AreaRange(toDouble(min), toDouble(max))
        }
        UPPER_BOUND.find(text)?.let { match ->
            return AreaRange(min = null, max = toDouble(match.groupValues[1]))
        }
        SINGLE_VALUE.find(text)?.let { match ->
            val value = toDouble(match.groupValues[1])
            return AreaRange(value, value)
        }
        return null
    }

    private fun toDouble(value: String): Double = value.replace(',', '.').toDouble()
}

/**
 * Builds an [AreaRange] from a list of unit-level area values (min/max of
 * the observed values), or `null` if the list is empty. Extracted from
 * three identical private copies in [NickelParser], [PWDParser] and
 * [JaksBudParser] (see docs review - "toAreaRange duplicated in 3 parsers"
 * finding) - those sources aggregate area from per-unit table rows rather
 * than a single published range string, so they don't go through
 * [PolishAreaFormat.parse].
 */
fun List<Double>.toAreaRange(): AreaRange? =
    if (isEmpty()) null else AreaRange(minOrNull(), maxOrNull())

/**
 * Extracts a URL from an inline CSS `background: url(...)` style
 * attribute value, for sites that render their card image as a background
 * rather than an `<img>` tag. Extracted from three byte-identical private
 * `IMAGE_URL` regex copies in [ChronosParser], [MJParser] and
 * [VastbouwParser] (see docs review - "CSS background-image URL regex
 * duplicated in 5 parsers" finding - the other two, `DudaParser` and
 * `SagarisParser`/`AtanerParser`, use subtly different, non-identical
 * regexes and are left as-is to avoid changing their matching behavior).
 */
object CssBackgroundImage {
    private val URL_IN_STYLE = Regex("url\\(['\"]?([^'\"()]+)['\"]?\\)")

    fun extractUrl(style: String): String? =
        URL_IN_STYLE.find(style)?.groupValues?.get(1)?.takeIf(String::isNotBlank)
}
