package pl.marcinwieczorek.investmentmonitor.llm

import pl.marcinwieczorek.investmentmonitor.domain.LocationActivity
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile

/**
 * Builds a prompt asking the local LLM to compare development activity
 * *across* multiple locations and identify the most dynamically developing
 * ones - the region-wide counterpart to [LocationSynthesisPromptBuilder],
 * which only ever looks at one location at a time. This comparative
 * ranking is exactly the part a per-location synthesis structurally cannot
 * do (see docs/ARCHITECTURE.md phase 12).
 *
 * Same language split as [LocationSynthesisPromptBuilder]: English system
 * instruction, Polish response.
 */
object HotspotSynthesisPromptBuilder {

    fun build(activities: List<LocationActivity>, referenceProfile: ReferenceInvestmentProfile): String = buildString {
        appendLine(
            "You are analyzing residential development activity across multiple locations " +
                "in the Poznań metropolitan area, Poland. Identify the most dynamically developing " +
                "areas and their relevance to the buyer's reference profile below. Respond in Polish."
        )
        appendLine("Respond with ONLY a single JSON object matching exactly this schema (no prose, no markdown fences):")
        appendLine(
            """{"hotspots":[{"location":"...","activityLevel":"HIGH|MEDIUM|LOW",""" +
                """"trend":"ACCELERATING|STABLE|SLOWING|MINIMAL","reason":"...",""" +
                """"relevanceToProfile":"HIGH|MEDIUM|LOW"}],""" +
                """"emergingAreas":["..."],"summary":"...","recommendation":"..."}"""
        )
        appendLine()

        appendLine("=== BUYER REFERENCE PROFILE (${referenceProfile.name}) ===")
        appendLine("preferredPropertyTypes: ${referenceProfile.preferredPropertyTypes.joinToString()}")
        appendLine("preferredLocationTiers: ${referenceProfile.preferredLocationTiers.joinToString()}")
        appendLine("houseAreaRange: ${referenceProfile.houseAreaRange}")
        appendLine("plotAreaRange: ${referenceProfile.plotAreaRange}")
        appendLine("priceRange: ${referenceProfile.priceRange}")
        appendLine("largePlotPreferred: ${referenceProfile.largePlotPreferred}")
        appendLine()

        appendLine("=== LOCATION ACTIVITY SUMMARY (top ${activities.size} by signal count) ===")
        activities.sortedByDescending { it.signalCount }.forEachIndexed { index, activity ->
            appendLine("${index + 1}. ${describeActivity(activity)}")
        }
    }

    private fun describeActivity(activity: LocationActivity): String {
        val municipality = activity.municipality?.let { " ($it" } ?: ""
        val tier = activity.locationProfile?.tier?.let { if (municipality.isNotEmpty()) ", Tier $it)" else " (Tier $it)" }
            ?: if (municipality.isNotEmpty()) ")" else ""
        val leadTime = activity.averageLeadTimeDays?.let { ", avg lead %+.0f days".format(it) } ?: ""
        val developers = if (activity.activeDevelopers.isEmpty()) "none known" else activity.activeDevelopers.joinToString()
        val dominant = activity.dominantSignalTypes.firstOrNull()?.toString() ?: "none"

        return "${activity.location}$municipality$tier: ${activity.signalCount} signals, " +
            "${activity.investmentCount} investments, ${activity.correlationCount} correlations$leadTime | " +
            "Developers: $developers | Dominant: $dominant"
    }
}
