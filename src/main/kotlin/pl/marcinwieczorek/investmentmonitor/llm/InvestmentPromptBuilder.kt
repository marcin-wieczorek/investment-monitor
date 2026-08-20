package pl.marcinwieczorek.investmentmonitor.llm

import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile

/**
 * Builds a compact, structured-facts prompt for the local LLM - never raw
 * HTML, and never facts the deterministic pipeline hasn't already
 * extracted (see docs/ARCHITECTURE.md LLM role section).
 */
object InvestmentPromptBuilder {

    fun build(
        investment: Investment,
        locationProfile: LocationProfile?,
        referenceProfile: ReferenceInvestmentProfile
    ): String = buildString {
        appendLine("You are assisting a home buyer evaluating a residential real-estate investment near Poznań, Poland.")
        appendLine("Respond with ONLY a single JSON object matching exactly this schema (no prose, no markdown fences):")
        appendLine(
            """{"attractiveness":"HIGH|MEDIUM|LOW","strongestPositives":["..."],"risks":["..."],""" +
                """"locationPromising":true|false,"plotUnusuallyAttractive":true|false,""" +
                """"worthManualReview":true|false,"missingInformation":["..."],"reason":"..."}"""
        )
        appendLine()
        appendLine("Investment:")
        appendLine("  name: ${investment.name}")
        appendLine("  developer: ${investment.developer}")
        appendLine("  location: ${investment.location ?: "unknown"}")
        appendLine("  propertyType: ${investment.propertyType ?: "unknown"}")
        appendLine("  houseArea: ${investment.houseArea?.let { "${it.min ?: "?"}-${it.max ?: "?"} m2" } ?: "unknown"}")
        appendLine("  plotArea: ${investment.plotArea?.let { "${it.min ?: "?"}-${it.max ?: "?"} m2" } ?: "unknown"}")
        appendLine("  price: ${investment.price?.let { "${it.min ?: "?"}-${it.max ?: "?"} PLN" } ?: "unknown"}")
        appendLine("  units: ${investment.units ?: "unknown"}")
        appendLine()
        if (locationProfile != null) {
            appendLine("LocationProfile:")
            appendLine("  tier: ${locationProfile.tier}")
            appendLine("  growthScore: ${locationProfile.growthScore}/10")
            appendLine("  infrastructureScore: ${locationProfile.infrastructureScore}/10")
            appendLine("  transportScore: ${locationProfile.transportScore}/10")
            appendLine("  familyScore: ${locationProfile.familyScore}/10")
            appendLine()
        }
        appendLine("ReferenceInvestmentProfile (${referenceProfile.name}):")
        appendLine("  preferredPropertyTypes: ${referenceProfile.preferredPropertyTypes.joinToString()}")
        appendLine("  preferredLocationTiers: ${referenceProfile.preferredLocationTiers.joinToString()}")
        appendLine("  houseAreaRange: ${referenceProfile.houseAreaRange}")
        appendLine("  plotAreaRange: ${referenceProfile.plotAreaRange}")
        appendLine("  priceRange: ${referenceProfile.priceRange}")
        appendLine("  largePlotPreferred: ${referenceProfile.largePlotPreferred}")
    }
}
