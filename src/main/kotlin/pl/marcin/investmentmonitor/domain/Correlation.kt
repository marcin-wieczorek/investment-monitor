package pl.marcin.investmentmonitor.domain

import java.time.Instant

/** How confident the deterministic correlator is that a signal and an investment refer to the same project. */
enum class CorrelationConfidence { HIGH, MEDIUM, LOW }

/**
 * A deterministic link between a discovery [InvestmentSignal] and an
 * [Investment] that likely describe the same underlying project - e.g. a
 * municipal "warunki zabudowy" decision for 150 terraced houses in
 * Kruszewnia later published by a developer as "Osiedle X".
 *
 * Matching is purely feature-based (location, municipality, developer
 * name overlap) - never LLM-driven, so correlation stays reproducible
 * and explainable (see docs/ARCHITECTURE.md cross-source correlation
 * section).
 */
data class Correlation(
    val id: Long? = null,
    val investmentId: Long,
    val signalId: Long,
    val confidence: CorrelationConfidence,
    val matchedFeatures: List<String>,
    val reason: String,
    val createdAt: Instant
)
