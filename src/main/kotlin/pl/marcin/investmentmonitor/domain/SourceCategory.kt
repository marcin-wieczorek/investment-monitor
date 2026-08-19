package pl.marcin.investmentmonitor.domain

/**
 * Which of the three source categories produced a fact.
 *
 * Factual authority follows [SourceCategory.DEVELOPER] > [SourceCategory.DISCOVERY] >
 * [SourceCategory.AGGREGATOR]: a developer's own site is the ground truth for its
 * investments, official municipal sources are the ground truth for early planning
 * signals, and aggregators are only a completeness/cross-check layer (see
 * docs/ARCHITECTURE.md).
 */
enum class SourceCategory { DEVELOPER, DISCOVERY, AGGREGATOR }
