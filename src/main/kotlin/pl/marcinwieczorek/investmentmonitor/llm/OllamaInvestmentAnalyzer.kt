package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
 * Local-LLM-backed [InvestmentAnalyzer], active only when
 * `investment-monitor.llm.enabled=true` (see docs/LLM.md for local Ollama
 * setup).
 *
 * The LLM never decides identity, deduplication or any fact a
 * deterministic parser already extracted (see
 * docs/ARCHITECTURE.md LLM role section): [DeterministicScorer] always
 * computes [InvestmentAnalysis.investmentScore] and
 * [InvestmentAnalysis.referenceProfileScore], against the same
 * user-configurable reference profile ([UserPreferencesRepository]) that
 * [pl.marcinwieczorek.investmentmonitor.analysis.DefaultInvestmentAnalyzer]
 * uses - so changing the scoring profile in Settings affects both analyzer
 * implementations identically, regardless of whether the LLM is enabled.
 * The LLM only supplies [InvestmentAnalysis.priority] and
 * [InvestmentAnalysis.reason] - pure interpretation/ranking - and only
 * when it returns a well-formed response; any failure (unreachable,
 * timeout, malformed JSON) falls back to a deterministic priority/reason
 * derived purely from the numeric score, so a missing local LLM never
 * breaks a scan.
 */
@Component
@ConditionalOnProperty(prefix = "investment-monitor.llm", name = ["enabled"], havingValue = "true")
class OllamaInvestmentAnalyzer(
    private val ollamaClient: OllamaClient,
    private val scorer: DeterministicScorer,
    private val llmAnalysisRepository: LlmAnalysisRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:Value("\${investment-monitor.llm.model:qwen2.5:7b}") private val model: String
) : InvestmentAnalyzer {

    private val mapper = jacksonObjectMapper()

    override fun analyze(investment: Investment, locationProfile: LocationProfile?): InvestmentAnalysis {
        val referenceProfile = userPreferencesRepository.effectiveScoringProfile()
        val scoring = scorer.score(investment, locationProfile, referenceProfile)

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

    private fun locationScore(locationProfile: LocationProfile?): Double? =
        DeterministicAnalysisSupport.locationScore(locationProfile)

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
