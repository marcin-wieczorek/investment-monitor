package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component

/**
 * Explicit, reviewable registry of every configured source, grouped by
 * category. Spring collects all [InvestmentSource], [DiscoverySource] and
 * [AggregatorSource] beans automatically; this class exists so the rest of
 * the pipeline (and reporting) never has to know how sources are wired,
 * only that three well-defined categories exist (see docs/SOURCES.md).
 */
@Component
class SourceRegistry(
    private val developerSources: List<InvestmentSource>,
    private val discoverySources: List<DiscoverySource>,
    private val aggregatorSources: List<AggregatorSource>
) {
    fun developerSources(): List<InvestmentSource> = developerSources
    fun discoverySources(): List<DiscoverySource> = discoverySources
    fun aggregatorSources(): List<AggregatorSource> = aggregatorSources

    fun allSourceIds(): Set<String> =
        (developerSources.map { it.id } + discoverySources.map { it.id } + aggregatorSources.map { it.id }).toSet()
}
