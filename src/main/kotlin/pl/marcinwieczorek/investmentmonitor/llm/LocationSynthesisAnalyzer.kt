package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.ActivityLevel
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTrend
import pl.marcinwieczorek.investmentmonitor.domain.HotspotEntry
import pl.marcinwieczorek.investmentmonitor.domain.HotspotSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.LocationActivity
import pl.marcinwieczorek.investmentmonitor.domain.LocationSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.RecommendedAction
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.persistence.LlmAnalysisRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

/**
 * LLM-assisted (or fully deterministic, when disabled/unavailable)
 * interpretation of the location-level and region-wide activity snapshots
 * built by
 * [pl.marcinwieczorek.investmentmonitor.analysis.LocationActivityCollector].
 *
 * Same contract as [OllamaInvestmentAnalyzer]: the LLM only ever supplies
 * qualitative interpretation (trend labels, free-text summaries,
 * opportunities/risks) on top of facts already assembled deterministically
 * - it never invents a signal or investment count, and every failure mode
 * (disabled, unreachable, malformed JSON) falls back to a result derived
 * purely from the counts/aggregates already present on [LocationActivity].
 * Fallback and LLM-derived text are both always in Polish, since this is
 * user-facing interpretive content, not a system identifier.
 */
@Component
class LocationSynthesisAnalyzer(
    private val ollamaClient: OllamaClient,
    private val llmAnalysisRepository: LlmAnalysisRepository,
    @param:Value("\${investment-monitor.llm.model:qwen2.5:7b}") private val model: String,
    @param:Value("\${investment-monitor.llm.enabled:true}") private val enabled: Boolean = true,
    private val clock: Clock = Clock.systemUTC()
) {

    private val mapper = jacksonObjectMapper()

    fun synthesizeLocation(activity: LocationActivity, referenceProfile: ReferenceInvestmentProfile): LocationSynthesis {
        val now = Instant.now(clock)
        val fallbackTrend = deterministicTrend(activity)

        if (!enabled) {
            return deterministicLocationSynthesis(activity, fallbackTrend, now)
        }

        val interpretation = interpretLocation(activity, referenceProfile)
            ?: return deterministicLocationSynthesis(activity, fallbackTrend, now)

        return LocationSynthesis(
            location = activity.location,
            municipality = activity.municipality,
            developmentTrend = trendFrom(interpretation.developmentTrend) ?: fallbackTrend,
            summary = interpretation.summary ?: deterministicSummary(activity, fallbackTrend),
            estimatedTimeline = interpretation.estimatedNewInvestmentsTimeline,
            keyDevelopers = interpretation.keyDevelopers.ifEmpty { activity.activeDevelopers },
            opportunities = interpretation.opportunities,
            risks = interpretation.risks,
            recommendedAction = actionFrom(interpretation.recommendedAction) ?: deterministicAction(activity, fallbackTrend),
            reason = interpretation.reason ?: "Synteza deterministyczna (brak odpowiedzi LLM dla wybranych pól).",
            signalCount = activity.signalCount,
            investmentCount = activity.investmentCount,
            averageLeadTimeDays = activity.averageLeadTimeDays,
            synthesizedAt = now
        )
    }

    fun synthesizeHotspots(activities: List<LocationActivity>, referenceProfile: ReferenceInvestmentProfile, topN: Int): HotspotSynthesis {
        val now = Instant.now(clock)
        val ranked = activities.sortedByDescending { it.signalCount }.take(topN)

        if (!enabled) {
            return deterministicHotspotSynthesis(ranked, referenceProfile, now)
        }

        val interpretation = interpretHotspots(ranked, referenceProfile)
            ?: return deterministicHotspotSynthesis(ranked, referenceProfile, now)

        val hotspots = interpretation.hotspots.mapNotNull { entry ->
            val location = entry.location ?: return@mapNotNull null
            HotspotEntry(
                location = location,
                activityLevel = activityLevelFrom(entry.activityLevel) ?: ActivityLevel.LOW,
                trend = trendFrom(entry.trend) ?: DevelopmentTrend.MINIMAL,
                reason = entry.reason ?: "",
                relevanceToProfile = activityLevelFrom(entry.relevanceToProfile) ?: ActivityLevel.LOW
            )
        }.ifEmpty { deterministicHotspotEntries(ranked, referenceProfile) }

        return HotspotSynthesis(
            hotspots = hotspots,
            emergingAreas = interpretation.emergingAreas,
            summary = interpretation.summary ?: deterministicHotspotSummary(ranked),
            recommendation = interpretation.recommendation ?: "Obserwuj lokalizacje z najwyższą liczbą sygnałów.",
            synthesizedAt = now
        )
    }

    private fun interpretLocation(activity: LocationActivity, referenceProfile: ReferenceInvestmentProfile): LlmLocationSynthesis? {
        val prompt = LocationSynthesisPromptBuilder.build(activity, referenceProfile)
        return callAndParse("location:${activity.location}", prompt)
    }

    private fun interpretHotspots(activities: List<LocationActivity>, referenceProfile: ReferenceInvestmentProfile): LlmHotspotSynthesis? {
        val prompt = HotspotSynthesisPromptBuilder.build(activities, referenceProfile)
        return callAndParse("hotspot:global", prompt)
    }

    private inline fun <reified T> callAndParse(cacheKey: String, prompt: String): T? {
        val promptHash = sha256(prompt)
        val cached = llmAnalysisRepository.findCached(cacheKey, model, promptHash)
        val raw = cached ?: ollamaClient.generate(model, prompt) ?: return null

        val parsed = runCatching { mapper.readValue<T>(raw) }
            .onFailure { error -> logger.warn("Could not parse LLM location-synthesis response as JSON: {}", error.message) }
            .getOrNull() ?: return null

        if (cached == null) {
            llmAnalysisRepository.save(cacheKey, model, promptHash, raw)
        }
        return parsed
    }

    // --- Deterministic fallbacks ---

    private fun deterministicTrend(activity: LocationActivity): DevelopmentTrend = when {
        activity.signalCount >= 3 -> DevelopmentTrend.ACCELERATING
        activity.signalCount in 1..2 -> DevelopmentTrend.STABLE
        else -> DevelopmentTrend.MINIMAL
    }

    private fun deterministicAction(activity: LocationActivity, trend: DevelopmentTrend): RecommendedAction = when {
        trend == DevelopmentTrend.MINIMAL && activity.investmentCount == 0 -> RecommendedAction.LOW_PRIORITY
        activity.signalCount >= 2 || activity.investmentCount >= 1 -> RecommendedAction.WATCH_CLOSELY
        else -> RecommendedAction.MONITOR
    }

    private fun deterministicSummary(activity: LocationActivity, trend: DevelopmentTrend): String {
        val developers = if (activity.activeDevelopers.isEmpty()) {
            ""
        } else {
            " Aktywni deweloperzy: ${activity.activeDevelopers.joinToString()}."
        }
        return "W lokalizacji ${activity.location} wykryto ${activity.signalCount} sygnałów " +
            "i ${activity.investmentCount} inwestycji w ostatnim okresie aktywności " +
            "(trend: ${trend.name}).$developers"
    }

    private fun deterministicLocationSynthesis(activity: LocationActivity, trend: DevelopmentTrend, now: Instant): LocationSynthesis =
        LocationSynthesis(
            location = activity.location,
            municipality = activity.municipality,
            developmentTrend = trend,
            summary = deterministicSummary(activity, trend),
            estimatedTimeline = null,
            keyDevelopers = activity.activeDevelopers,
            opportunities = emptyList(),
            risks = emptyList(),
            recommendedAction = deterministicAction(activity, trend),
            reason = "Synteza deterministyczna (LLM wyłączony lub niedostępny) na podstawie liczby sygnałów i inwestycji.",
            signalCount = activity.signalCount,
            investmentCount = activity.investmentCount,
            averageLeadTimeDays = activity.averageLeadTimeDays,
            synthesizedAt = now
        )

    private fun deterministicHotspotEntries(activities: List<LocationActivity>, referenceProfile: ReferenceInvestmentProfile): List<HotspotEntry> =
        activities.mapIndexed { index, activity ->
            val trend = deterministicTrend(activity)
            HotspotEntry(
                location = activity.location,
                activityLevel = activityLevelByRank(index, activities.size),
                trend = trend,
                reason = "Liczba sygnałów: ${activity.signalCount}, inwestycji: ${activity.investmentCount}.",
                relevanceToProfile = relevanceToProfile(activity, referenceProfile)
            )
        }

    private fun deterministicHotspotSummary(activities: List<LocationActivity>): String {
        if (activities.isEmpty()) return "Brak aktywnych lokalizacji w bieżącym okresie."
        val top = activities.first()
        return "Największa aktywność deweloperska odnotowana w lokalizacji ${top.location} " +
            "(${top.signalCount} sygnałów, ${top.investmentCount} inwestycji)."
    }

    private fun deterministicHotspotSynthesis(
        activities: List<LocationActivity>,
        referenceProfile: ReferenceInvestmentProfile,
        now: Instant
    ): HotspotSynthesis = HotspotSynthesis(
        hotspots = deterministicHotspotEntries(activities, referenceProfile),
        emergingAreas = emptyList(),
        summary = deterministicHotspotSummary(activities),
        recommendation = "Obserwuj lokalizacje z najwyższą liczbą sygnałów (synteza deterministyczna, LLM wyłączony lub niedostępny).",
        synthesizedAt = now
    )

    private fun activityLevelByRank(index: Int, total: Int): ActivityLevel {
        if (total == 0) return ActivityLevel.LOW
        val fraction = index.toDouble() / total
        return when {
            fraction < 1.0 / 3 -> ActivityLevel.HIGH
            fraction < 2.0 / 3 -> ActivityLevel.MEDIUM
            else -> ActivityLevel.LOW
        }
    }

    private fun relevanceToProfile(activity: LocationActivity, referenceProfile: ReferenceInvestmentProfile): ActivityLevel {
        val tierMatches = activity.locationProfile?.tier?.let { it in referenceProfile.preferredLocationTiers } ?: false
        val propertyTypeMatches = activity.investments.any { it.propertyType != null && it.propertyType in referenceProfile.preferredPropertyTypes }
        return when {
            tierMatches && propertyTypeMatches -> ActivityLevel.HIGH
            tierMatches || propertyTypeMatches -> ActivityLevel.MEDIUM
            else -> ActivityLevel.LOW
        }
    }

    private fun trendFrom(value: String?): DevelopmentTrend? = runCatching {
        value?.uppercase()?.let { DevelopmentTrend.valueOf(it) }
    }.getOrNull()

    private fun actionFrom(value: String?): RecommendedAction? = runCatching {
        value?.uppercase()?.let { RecommendedAction.valueOf(it) }
    }.getOrNull()

    private fun activityLevelFrom(value: String?): ActivityLevel? = runCatching {
        value?.uppercase()?.let { ActivityLevel.valueOf(it) }
    }.getOrNull()

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(LocationSynthesisAnalyzer::class.java)
    }
}
