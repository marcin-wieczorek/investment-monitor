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
