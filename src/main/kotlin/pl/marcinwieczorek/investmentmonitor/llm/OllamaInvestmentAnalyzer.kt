package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicAnalysisSupport
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer
import pl.marcinwieczorek.investmentmonitor.analysis.InvestmentAnalysis
import pl.marcinwieczorek.investmentmonitor.analysis.InvestmentAnalyzer
import pl.marcinwieczorek.investmentmonitor.analysis.Priority
import pl.marcinwieczorek.investmentmonitor.analysis.ScoringResult
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.persistence.LlmAnalysisRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import java.security.MessageDigest

/**
 * The sole [InvestmentAnalyzer] implementation. Enabled by default
 * (`investment-monitor.llm.enabled=true`, see docs/LLM.md for local Ollama
 * setup) - a fresh checkout with no Ollama installed still produces a
 * fully valid, fully deterministic [InvestmentAnalysis] for every
 * investment, because every LLM call path degrades gracefully to the
 * same deterministic result (see below). There is deliberately no
 * separate "LLM off" analyzer bean: the deterministic path *is* this
 * class's fallback, not a different implementation, so the two can never
 * drift apart (previously two classes - `DefaultInvestmentAnalyzer` and
 * this one - duplicated the same deterministic logic; see
 * docs/ADR-006-ollama-integration.md for why they were merged).
 *
 * The LLM never decides identity, deduplication or any fact a
 * deterministic parser already extracted (see
 * docs/ARCHITECTURE.md LLM role section): [DeterministicScorer] always
 * computes [InvestmentAnalysis.investmentScore] and
 * [InvestmentAnalysis.referenceProfileScore], against the same
 * user-configurable reference profile ([UserPreferencesRepository]). The
 * LLM only supplies [InvestmentAnalysis.priority] and
 * [InvestmentAnalysis.reason] - pure interpretation/ranking - and only
 * when it returns a well-formed response; any failure (disabled,
 * unreachable, timeout, malformed JSON) falls back to a deterministic
 * priority/reason derived purely from the numeric score, so a missing or
 * disabled local LLM never breaks a scan.
 */
@Component
class OllamaInvestmentAnalyzer(
    private val ollamaClient: OllamaClient,
    private val scorer: DeterministicScorer,
    private val llmAnalysisRepository: LlmAnalysisRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:Value("\${investment-monitor.llm.model:qwen2.5:7b}") private val model: String,
    @param:Value("\${investment-monitor.llm.enabled:true}") private val enabled: Boolean = true
) : InvestmentAnalyzer {

    private val mapper = jacksonObjectMapper()

    override fun analyze(investment: Investment, locationProfile: LocationProfile?): InvestmentAnalysis {
        val referenceProfile = userPreferencesRepository.effectiveScoringProfile()
        val scoring = scorer.score(investment, locationProfile, referenceProfile)

        if (!enabled) {
            return InvestmentAnalysis(
                investmentScore = scoring.overallScore,
                locationScore = DeterministicAnalysisSupport.locationScore(locationProfile),
                referenceProfileScore = scoring.overallScore,
                priority = DeterministicAnalysisSupport.priorityFrom(scoring),
                reason = DeterministicAnalysisSupport.describeScore(scoring)
            )
        }

        val interpretation = interpret(investment, locationProfile, referenceProfile)
            ?: return fallback(scoring, "LLM unavailable or returned an unusable response; using deterministic score only.")

        return InvestmentAnalysis(
            investmentScore = scoring.overallScore,
            locationScore = DeterministicAnalysisSupport.locationScore(locationProfile),
            referenceProfileScore = scoring.overallScore,
            priority = priorityFrom(interpretation.attractiveness) ?: DeterministicAnalysisSupport.priorityFrom(scoring),
            reason = interpretation.reason ?: DeterministicAnalysisSupport.describeScore(scoring)
        )
    }

    private fun interpret(
        investment: Investment,
        locationProfile: LocationProfile?,
        referenceProfile: pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
    ): LlmInvestmentInterpretation? {
        val prompt = InvestmentPromptBuilder.build(investment, locationProfile, referenceProfile)
        val promptHash = sha256(prompt)

        val cached = llmAnalysisRepository.findCached(investment.canonicalKey, model, promptHash)
        val raw = cached ?: ollamaClient.generate(model, prompt) ?: return null

        val interpretation = runCatching { mapper.readValue<LlmInvestmentInterpretation>(raw) }
            .onFailure { error -> logger.warn("Could not parse LLM response as JSON: {}", error.message) }
            .getOrNull() ?: return null

        if (cached == null) {
            llmAnalysisRepository.save(investment.canonicalKey, model, promptHash, raw)
        }
        return interpretation
    }

    private fun priorityFrom(attractiveness: String?): Priority? = when (attractiveness?.uppercase()) {
        "HIGH" -> Priority.HIGH
        "MEDIUM" -> Priority.MEDIUM
        "LOW" -> Priority.LOW
        else -> null
    }

    private fun fallback(scoring: ScoringResult, reason: String): InvestmentAnalysis = InvestmentAnalysis(
        investmentScore = scoring.overallScore,
        locationScore = null,
        referenceProfileScore = scoring.overallScore,
        priority = DeterministicAnalysisSupport.priorityFrom(scoring),
        reason = reason
    )

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(OllamaInvestmentAnalyzer::class.java)
    }
}
