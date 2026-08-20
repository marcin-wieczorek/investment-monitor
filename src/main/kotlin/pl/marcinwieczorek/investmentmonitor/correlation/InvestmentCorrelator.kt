package pl.marcinwieczorek.investmentmonitor.correlation

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.CorrelationConfidence
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog

/**
 * A candidate link between a discovery signal and an investment, before
 * either side's database id is known. The caller resolves both ids (via
 * the investment/signal repositories) and persists the result as a
 * [pl.marcinwieczorek.investmentmonitor.domain.Correlation].
 */
data class CorrelationCandidate(
    val investment: Investment,
    val signal: InvestmentSignal,
    val confidence: CorrelationConfidence,
    val matchedFeatures: List<String>,
    val reason: String
)

/**
 * Deterministically links discovery [InvestmentSignal]s to [Investment]s
 * that likely describe the same underlying project - e.g. a municipal
 * zoning-conditions decision for houses in Kruszewnia later published by a
 * developer as "Osiedle X".
 *
 * Matching is purely feature-based (never LLM-driven, see
 * docs/ARCHITECTURE.md cross-source correlation section):
 *
 * - same normalized location name -> a candidate match ([CorrelationConfidence.MEDIUM])
 * - developer name also mentioned inside the signal's title/reference text -> [CorrelationConfidence.HIGH]
 *
 * A signal or investment with no recognized location in [LocationCatalog]
 * is never matched - there is nothing deterministic to compare, so it is
 * left uncorrelated rather than guessed. A location equal to the signal's
 * own municipality (e.g. "Swarzędz" on a source whose scope already *is*
 * Gmina Swarzędz) is deliberately not treated as a match either: it has no
 * discriminating power and would otherwise correlate nearly every signal
 * in the register to every investment in the same town. Signals whose
 * title does not mention residential construction (a house/building
 * keyword) are also excluded - the register also carries plenty of
 * unrelated permits (retaining walls, transformer stations, wind
 * micro-installations, ...) that would otherwise flood the correlation
 * with noise unrelated to housing.
 */
@Component
class InvestmentCorrelator {

    fun correlate(investments: List<Investment>, signals: List<InvestmentSignal>): List<CorrelationCandidate> =
        signals.filter(::mentionsResidentialConstruction).flatMap { signal -> matchesFor(signal, investments) }

    private fun mentionsResidentialConstruction(signal: InvestmentSignal): Boolean =
        RESIDENTIAL_KEYWORDS.any { keyword -> signal.title.contains(keyword, ignoreCase = true) }

    private fun matchesFor(signal: InvestmentSignal, investments: List<Investment>): List<CorrelationCandidate> {
        val signalLocation = signal.location ?: return emptyList()
        if (signalLocation.equals(signal.municipality, ignoreCase = true)) return emptyList()

        return investments.mapNotNull { investment ->
            val investmentLocation = investment.location?.let(LocationCatalog::findIn) ?: return@mapNotNull null
            if (!investmentLocation.equals(signalLocation, ignoreCase = true)) return@mapNotNull null

            val developerMentioned = mentionsDeveloper(signal, investment)
            val matchedFeatures = buildList {
                add("location:$signalLocation")
                if (developerMentioned) add("developer:${investment.developer}")
            }
            val confidence = if (developerMentioned) CorrelationConfidence.HIGH else CorrelationConfidence.MEDIUM
            val reason = buildString {
                append("Same location ($signalLocation)")
                if (developerMentioned) append(", developer name found in signal text")
            }

            CorrelationCandidate(investment, signal, confidence, matchedFeatures, reason)
        }
    }

    private fun mentionsDeveloper(signal: InvestmentSignal, investment: Investment): Boolean {
        val developerWords = investment.developer.split(" ").filter { it.length > 3 }
        if (developerWords.isEmpty()) return false
        val haystack = "${signal.title} ${signal.reference.orEmpty()}"
        return developerWords.any { word -> containsWholeWord(haystack, word) }
    }

    /**
     * Whole-word match rather than plain [String.contains] - a bare
     * substring check would let a developer word like "Development" or
     * "Invest" false-positive-match inside an unrelated longer word (see
     * docs review - "mentionsDeveloper word matching could false-positive
     * on common words" finding). Uses the same explicit Unicode
     * letter/digit lookaround as [LocationCatalog.findIn] rather than
     * `\b`, since developer names commonly start/end with a Polish
     * diacritic letter.
     */
    private fun containsWholeWord(haystack: String, word: String): Boolean =
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(word)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
            .containsMatchIn(haystack)

    private companion object {
        val RESIDENTIAL_KEYWORDS = listOf(
            "budynk\u00f3w mieszkal", "budynku mieszkal", "budynki mieszkal", "domów jednorodzin",
            "domu jednorodzin", "zabudowie szeregowej", "zabudowie bli\u017aniaczej", "zabudowy jednorodzinnej",
            "budynek mieszkalny"
        )
    }
}
