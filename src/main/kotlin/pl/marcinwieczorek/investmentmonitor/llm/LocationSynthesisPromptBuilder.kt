package pl.marcinwieczorek.investmentmonitor.llm

import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationActivity
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime

/**
 * Builds a compact, structured-facts prompt asking the local LLM to
 * synthesize everything currently known about a single location - never
 * raw HTML, and never a fact
 * [pl.marcinwieczorek.investmentmonitor.analysis.LocationActivityCollector]
 * hasn't already assembled deterministically (see docs/ARCHITECTURE.md LLM
 * role section).
 *
 * The system instruction is deliberately in English (better instruction-
 * following for most local models), but the response itself is required to
 * be in Polish - this is user-facing interpretive text about Polish
 * municipal planning data, not a system identifier.
 */
object LocationSynthesisPromptBuilder {

    fun build(activity: LocationActivity, referenceProfile: ReferenceInvestmentProfile): String = buildString {
        appendLine(
            "You are assisting a home buyer monitoring residential development activity " +
                "in the Poznań metropolitan area, Poland. Analyze the data below for ONE specific " +
                "location and provide a structured synthesis. Respond in Polish."
        )
        appendLine("Respond with ONLY a single JSON object matching exactly this schema (no prose, no markdown fences):")
        appendLine(
            """{"developmentTrend":"ACCELERATING|STABLE|SLOWING|MINIMAL",""" +
                """"summary":"...","estimatedNewInvestmentsTimeline":"..." or null,""" +
                """"keyDevelopers":["..."],"opportunities":["..."],"risks":["..."],""" +
                """"recommendedAction":"WATCH_CLOSELY|MONITOR|LOW_PRIORITY","reason":"..."}"""
        )
        appendLine()

        appendLine("=== LOCATION ===")
        appendLine("name: ${activity.location}")
        appendLine("municipality: ${activity.municipality ?: "unknown"}")
        activity.locationProfile?.let { profile ->
            appendLine("tier: ${profile.tier}")
            appendLine("growthScore: ${profile.growthScore}/10")
            appendLine("infrastructureScore: ${profile.infrastructureScore}/10")
            appendLine("transportScore: ${profile.transportScore}/10")
            appendLine("familyScore: ${profile.familyScore}/10")
        }
        appendLine()

        appendReferenceProfile(referenceProfile)

        appendLine("=== DISCOVERY SIGNALS (last activity window, ${activity.signalCount} total) ===")
        if (activity.signals.isEmpty()) {
            appendLine("(none)")
        } else {
            activity.signals.sortedByDescending { it.detectedAt }.forEachIndexed { index, signal ->
                appendLine("${index + 1}. ${describeSignal(signal)}")
            }
        }
        appendLine()

        appendLine("=== KNOWN INVESTMENTS IN THIS LOCATION (${activity.investmentCount} total) ===")
        if (activity.investments.isEmpty()) {
            appendLine("(none)")
        } else {
            activity.investments.forEachIndexed { index, investment ->
                appendLine("${index + 1}. ${describeInvestment(investment)}")
            }
        }
        appendLine()

        appendLine("=== CORRELATIONS (${activity.correlationCount} total) ===")
        if (activity.correlations.isEmpty()) {
            appendLine("(none)")
        } else {
            activity.correlations.forEachIndexed { index, correlation ->
                appendLine("${index + 1}. ${describeCorrelation(correlation)}")
            }
        }
    }

    private fun StringBuilder.appendReferenceProfile(referenceProfile: ReferenceInvestmentProfile) {
        appendLine("=== BUYER REFERENCE PROFILE (${referenceProfile.name}) ===")
        appendLine("preferredPropertyTypes: ${referenceProfile.preferredPropertyTypes.joinToString()}")
        appendLine("preferredLocationTiers: ${referenceProfile.preferredLocationTiers.joinToString()}")
        appendLine("houseAreaRange: ${referenceProfile.houseAreaRange}")
        appendLine("plotAreaRange: ${referenceProfile.plotAreaRange}")
        appendLine("priceRange: ${referenceProfile.priceRange}")
        appendLine("largePlotPreferred: ${referenceProfile.largePlotPreferred}")
        appendLine()
    }

    private fun describeSignal(signal: InvestmentSignal): String {
        val date = signal.detectedAt.toString().substringBefore("T")
        val ref = signal.reference?.let { " (ref: $it)" } ?: ""
        return "[$date] ${signal.signalType}: \"${signal.title}\"$ref"
    }

    private fun describeInvestment(investment: Investment): String {
        val propertyType = investment.propertyType?.toString() ?: "unknown type"
        val houseArea = investment.houseArea?.let { "${it.min ?: "?"}-${it.max ?: "?"} m2" } ?: "area unknown"
        val plotArea = investment.plotArea?.let { ", plot ${it.min ?: "?"}-${it.max ?: "?"} m2" } ?: ""
        val price = investment.price?.let { ", ${it.min ?: "?"}-${it.max ?: "?"} PLN" } ?: ", price unknown"
        return "\"${investment.name}\" by ${investment.developer} | $propertyType | $houseArea$plotArea$price"
    }

    private fun describeCorrelation(correlation: CorrelationLeadTime): String {
        val leadTime = correlation.leadTimeDays?.let { "lead time: ${if (it >= 0) "+" else ""}$it days" } ?: "lead time unknown"
        return "Signal \"${correlation.signalTitle}\" -> Investment \"${correlation.investmentName}\" | $leadTime"
    }
}
