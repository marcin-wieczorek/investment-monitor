package pl.marcinwieczorek.investmentmonitor.domain

import java.time.Instant

/** How confident the deterministic deduplicator is that two investments from different sources describe the same project. */
enum class DuplicateConfidence { HIGH, MEDIUM, LOW }

/**
 * A deterministic link between two [Investment]s from *different* sources
 * that likely describe the same real-world project - e.g. "Tercja"
 * published on Chronos's own site and "Osiedle Tercja | Chronos" listed on
 * an aggregator portal. Without this, the two coexist as unrelated rows
 * everywhere in the frontend even though they are the same investment (see
 * docs/ARCHITECTURE.md cross-source deduplication section).
 *
 * [investmentIdA] is always the smaller database id of the pair (enforced
 * by [pl.marcinwieczorek.investmentmonitor.persistence.InvestmentDuplicateRepository.save]),
 * so the same pair is never stored twice in reversed order.
 *
 * Matching is purely feature-based (location, developer name, investment
 * name overlap) - never LLM-driven, same rationale as
 * [Correlation] - so it stays reproducible and explainable.
 */
data class InvestmentDuplicate(
    val id: Long? = null,
    val investmentIdA: Long,
    val investmentIdB: Long,
    val confidence: DuplicateConfidence,
    val matchedFeatures: List<String>,
    val reason: String,
    val createdAt: Instant
)
