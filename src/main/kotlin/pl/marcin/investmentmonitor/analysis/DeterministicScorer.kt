package pl.marcin.investmentmonitor.analysis

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.LocationProfile
import pl.marcin.investmentmonitor.domain.PriceRange
import pl.marcin.investmentmonitor.domain.ReferenceInvestmentProfile

/**
 * Deterministic, numeric comparison of an investment against a
 * [ReferenceInvestmentProfile] and (optionally) a [LocationProfile].
 *
 * This is the single source of truth for "how similar is this investment
 * to what I'm looking for" - an LLM may add qualitative interpretation on
 * top (see [pl.marcin.investmentmonitor.analysis.InvestmentAnalyzer]) but
 * must never replace this numeric comparison (see
 * docs/ARCHITECTURE.md reference-profile section).
 *
 * Large plots are explicitly rewarded, never penalized: a plot larger than
 * the reference profile's preferred range earns [ScoringResult.largePlotBonus]
 * instead of a lower score (see docs/ARCHITECTURE.md large plots section).
 */
@Component
class DeterministicScorer {

    fun score(
        investment: Investment,
        locationProfile: LocationProfile?,
        referenceProfile: ReferenceInvestmentProfile
    ): ScoringResult {
        val propertyTypeMatch = investment.propertyType != null &&
            investment.propertyType in referenceProfile.preferredPropertyTypes

        val locationTierMatch = locationProfile?.let { it.tier in referenceProfile.preferredLocationTiers }

        val houseAreaScore = representativeValue(investment.houseArea)
            ?.let { value -> referenceProfile.houseAreaRange?.let { range -> rangeScore(value, range) } }

        val plotValue = representativeValue(investment.plotArea)
        val plotAreaScore = plotValue
            ?.let { value -> referenceProfile.plotAreaRange?.let { range -> rangeScore(value, range) } }

        val largePlotBonus = referenceProfile.largePlotPreferred &&
            plotValue != null &&
            referenceProfile.plotAreaRange?.max != null &&
            plotValue > referenceProfile.plotAreaRange.max

        val priceScore = representativePrice(investment.price)
            ?.let { value -> referenceProfile.priceRange?.let { range -> priceRangeScore(value, range) } }

        val plotToHouseRatio = if (plotValue != null) {
            representativeValue(investment.houseArea)?.takeIf { it > 0 }?.let { house -> plotValue / house }
        } else {
            null
        }

        val overallScore = weightedAverage(
            WeightedComponent(if (investment.propertyType != null) if (propertyTypeMatch) 1.0 else 0.0 else null, 0.25),
            WeightedComponent(locationTierMatch?.let { if (it) 1.0 else 0.0 }, 0.15),
            WeightedComponent(plotAreaScore, 0.25),
            WeightedComponent(houseAreaScore, 0.20),
            WeightedComponent(priceScore, 0.15)
        ) + if (largePlotBonus) LARGE_PLOT_BONUS else 0.0

        return ScoringResult(
            propertyTypeMatch = propertyTypeMatch,
            locationTierMatch = locationTierMatch,
            houseAreaScore = houseAreaScore,
            plotAreaScore = plotAreaScore,
            priceScore = priceScore,
            largePlotBonus = largePlotBonus,
            plotToHouseRatio = plotToHouseRatio,
            overallScore = overallScore.coerceIn(0.0, 1.0)
        )
    }

    private fun representativeValue(range: AreaRange?): Double? {
        if (range == null) return null
        val min = range.min
        val max = range.max
        return when {
            min != null && max != null -> (min + max) / 2.0
            min != null -> min
            max != null -> max
            else -> null
        }
    }

    private fun representativePrice(range: PriceRange?): Double? {
        if (range == null) return null
        val min = range.min?.toDouble()
        val max = range.max?.toDouble()
        return when {
            min != null && max != null -> (min + max) / 2.0
            min != null -> min
            max != null -> max
            else -> null
        }
    }

    /** 1.0 inside the range, decaying linearly to 0.0 over one range-width outside it. */
    private fun rangeScore(value: Double, range: AreaRange): Double {
        val min = range.min ?: range.max ?: return 0.0
        val max = range.max ?: range.min ?: return 0.0
        if (value in min..max) return 1.0
        val distance = if (value < min) min - value else value - max
        val span = (max - min).takeIf { it > 0.0 } ?: min.coerceAtLeast(1.0)
        return (1.0 - distance / span).coerceIn(0.0, 1.0)
    }

    private fun priceRangeScore(value: Double, range: PriceRange): Double {
        val min = (range.min ?: range.max ?: return 0.0).toDouble()
        val max = (range.max ?: range.min ?: return 0.0).toDouble()
        if (value in min..max) return 1.0
        val distance = if (value < min) min - value else value - max
        val span = (max - min).takeIf { it > 0.0 } ?: min.coerceAtLeast(1.0)
        return (1.0 - distance / span).coerceIn(0.0, 1.0)
    }

    private data class WeightedComponent(val value: Double?, val weight: Double)

    private fun weightedAverage(vararg components: WeightedComponent): Double {
        val present = components.filter { it.value != null }
        if (present.isEmpty()) return 0.0
        val totalWeight = present.sumOf { it.weight }
        return present.sumOf { it.value!! * it.weight } / totalWeight
    }

    private companion object {
        const val LARGE_PLOT_BONUS = 0.10
    }
}

/**
 * Result of comparing an investment against a reference profile. All
 * component scores are 0.0-1.0 and null when the underlying fact was
 * unavailable (not fabricated).
 */
data class ScoringResult(
    val propertyTypeMatch: Boolean,
    val locationTierMatch: Boolean?,
    val houseAreaScore: Double?,
    val plotAreaScore: Double?,
    val priceScore: Double?,
    val largePlotBonus: Boolean,
    val plotToHouseRatio: Double?,
    val overallScore: Double
)
