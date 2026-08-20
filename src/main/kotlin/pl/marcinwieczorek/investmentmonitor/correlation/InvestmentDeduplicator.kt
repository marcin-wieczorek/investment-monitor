package pl.marcinwieczorek.investmentmonitor.correlation

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperNameMatcher
import pl.marcinwieczorek.investmentmonitor.domain.DuplicateConfidence
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog

/**
 * A candidate link between two investments from different sources, before
 * either side's database id is known. The caller resolves both ids (via
 * [pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository.findIdByCanonicalKey])
 * and persists the result as a [pl.marcinwieczorek.investmentmonitor.domain.InvestmentDuplicate].
 */
data class DuplicateCandidate(
    val investmentA: Investment,
    val investmentB: Investment,
    val confidence: DuplicateConfidence,
    val matchedFeatures: List<String>,
    val reason: String
)

/**
 * Deterministically finds investments from *different* sources that likely
 * describe the same real-world project - e.g. "Tercja" published by
 * Chronos's own developer site and "Osiedle Tercja | Chronos" listed on the
 * RynekPierwotny aggregator. [Investment.canonicalKey] is `source:url`, so
 * it never merges these on its own - without this step the same project
 * shows up as unrelated duplicate rows everywhere in the frontend (see
 * docs/ARCHITECTURE.md cross-source deduplication section).
 *
 * Matching never spans two investments from the *same* source: within a
 * single source, canonical-key identity is already ground truth, so two
 * different rows from the same source really are two different projects.
 *
 * Purely feature-based, never LLM-driven, same rationale as
 * [InvestmentCorrelator]:
 *
 * - a shared recognized [LocationCatalog] location is required for *any*
 *   match at all - two investments with no recognized or differing
 *   location are never compared
 * - matching developer name (via [DeveloperNameMatcher], ignoring
 *   aggregator placeholder names like "Unknown (RynekPierwotny)") *and*
 *   strong investment-name token overlap -> [DuplicateConfidence.HIGH]
 * - matching developer name alone, or strong name overlap alone ->
 *   [DuplicateConfidence.MEDIUM]
 * - weak name overlap only (different/unknown developer) ->
 *   [DuplicateConfidence.LOW]
 */
@Component
class InvestmentDeduplicator {

    fun findDuplicates(investments: List<Investment>): List<DuplicateCandidate> =
        groupByLocation(investments).values.flatMap(::pairwiseCandidates)

    private fun groupByLocation(investments: List<Investment>): Map<String, List<Investment>> =
        investments
            .mapNotNull { investment -> investment.location?.let(LocationCatalog::findIn)?.let { it to investment } }
            .groupBy({ it.first }, { it.second })

    private fun pairwiseCandidates(group: List<Investment>): List<DuplicateCandidate> = buildList {
        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                val a = group[i]
                val b = group[j]
                if (a.source == b.source) continue
                candidateFor(a, b)?.let(::add)
            }
        }
    }

    private fun candidateFor(a: Investment, b: Investment): DuplicateCandidate? {
        val location = a.location?.let(LocationCatalog::findIn) ?: return null
        val developerMatch = isKnownDeveloperName(a) && isKnownDeveloperName(b) &&
            DeveloperNameMatcher.matches(a.developer, b.developer)
        val similarity = nameSimilarity(a.name, b.name)

        val confidence = when {
            developerMatch && similarity >= NAME_OVERLAP_STRONG -> DuplicateConfidence.HIGH
            developerMatch || similarity >= NAME_OVERLAP_STRONG -> DuplicateConfidence.MEDIUM
            similarity >= NAME_OVERLAP_WEAK -> DuplicateConfidence.LOW
            else -> return null
        }

        val matchedFeatures = buildList {
            add("location:$location")
            if (developerMatch) add("developer:${a.developer}")
            if (similarity > 0.0) add("nameOverlap:${"%.2f".format(similarity)}")
        }
        val reason = buildString {
            append("Same location ($location)")
            if (developerMatch) append(", same developer")
            if (similarity > 0.0) append(", name overlap ${(similarity * 100).toInt()}%")
        }

        return DuplicateCandidate(a, b, confidence, matchedFeatures, reason)
    }

    /** An aggregator's placeholder developer name (e.g. "Unknown (RynekPierwotny)") has no discriminating power. */
    private fun isKnownDeveloperName(investment: Investment): Boolean =
        !investment.developer.contains("unknown", ignoreCase = true)

    private fun nameSimilarity(a: String, b: String): Double {
        val tokensA = nameTokens(a)
        val tokensB = nameTokens(b)
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size
        return intersection.toDouble() / union
    }

    private fun nameTokens(name: String): Set<String> =
        NON_WORD.replace(name.lowercase(), " ")
            .split(" ")
            .filter { it.isNotBlank() && it !in STOPWORDS }
            .toSet()

    private companion object {
        const val NAME_OVERLAP_STRONG = 0.5
        const val NAME_OVERLAP_WEAK = 0.25
        val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        val STOPWORDS = setOf(
            "osiedle", "inwestycja", "by", "domy", "dom", "mieszkania", "mieszkanie",
            "apartamenty", "etap", "i", "ii", "iii", "iv", "v", "z", "w", "the"
        )
    }
}
