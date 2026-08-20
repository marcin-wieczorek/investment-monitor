package pl.marcinwieczorek.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.marcinwieczorek.investmentmonitor.detection.ChangeType
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidate
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.persistence.DeveloperCandidateRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.registry.DeveloperRegistry
import pl.marcinwieczorek.investmentmonitor.reporting.SourceReport
import pl.marcinwieczorek.investmentmonitor.source.SourceRegistry
import java.time.Clock
import java.time.Instant

/**
 * Detects investments that only an aggregator source knows about (no
 * developer source covers their location yet) and the unknown-developer
 * feedback loop that follows from it (AGENTS.md sections 6/33): when an
 * aggregator publishes an investment from a developer the system does not
 * yet know about, record a [DeveloperCandidate] for later human review
 * rather than silently ignoring the discovery or auto-trusting the
 * developer.
 *
 * Split out of [MonitoringService] for independent testability.
 */
@Service
class AggregatorDiscoveryService(
    private val investmentRepository: InvestmentRepository,
    private val sourceRegistry: SourceRegistry,
    private val developerCandidateRepository: DeveloperCandidateRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findAggregatorOnlyDiscoveries(aggregatorReports: List<SourceReport>): List<Investment> {
        val newAggregatorInvestments = aggregatorReports
            .flatMap { it.changes }
            .filter { it.change.type == ChangeType.NEW }
            .mapNotNull { it.change.current }
        if (newAggregatorInvestments.isEmpty()) return emptyList()

        val developerLocations = developerLocations()

        return newAggregatorInvestments.filter { investment ->
            val location = investment.location?.let(LocationCatalog::findIn)
            location == null || location !in developerLocations
        }
    }

    fun recordUnknownDeveloperCandidates(aggregatorOnlyDiscoveries: List<Investment>) {
        aggregatorOnlyDiscoveries
            .filter { investment -> DeveloperRegistry.findByName(investment.developer) == null }
            .filter { investment -> developerCandidateRepository.findByName(investment.developer) == null }
            .forEach { investment ->
                developerCandidateRepository.save(
                    DeveloperCandidate(
                        developerName = investment.developer,
                        discoveredUrl = investment.url,
                        municipality = investment.location?.let(LocationCatalog::findIn),
                        discoveredFromSource = investment.source.value,
                        discoveredAt = Instant.now(clock)
                    )
                )
                logger.info("Recorded new developer candidate '{}' from source '{}'", investment.developer, investment.source)
            }
    }

    /**
     * Persists, for every currently known aggregator investment (not just
     * this run's new ones - unlike [findAggregatorOnlyDiscoveries], which
     * only feeds the per-scan console report), whether it currently has no
     * matching developer source covering its location. Lets the frontend
     * filter on `investment.aggregator_only_discovery` directly instead of
     * re-deriving [LocationCatalog] matching in SQL/JS.
     */
    fun updateAggregatorOnlyDiscoveryFlags() {
        val developerLocations = developerLocations()

        sourceRegistry.aggregatorSources()
            .flatMap { investmentRepository.findAllBySource(SourceId(it.id)).values }
            .forEach { investment ->
                val location = investment.location?.let(LocationCatalog::findIn)
                val isAggregatorOnly = location == null || location !in developerLocations
                investmentRepository.updateAggregatorOnlyDiscoveryFlag(investment.canonicalKey, isAggregatorOnly)
            }
    }

    private fun developerLocations(): Set<String> =
        sourceRegistry.developerSources()
            .flatMap { investmentRepository.findAllBySource(SourceId(it.id)).values }
            .mapNotNull { it.location?.let(LocationCatalog::findIn) }
            .toSet()

    private companion object {
        val logger = LoggerFactory.getLogger(AggregatorDiscoveryService::class.java)
    }
}
