package pl.marcin.investmentmonitor.monitoring

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.analysis.NoOpInvestmentAnalyzer
import pl.marcin.investmentmonitor.archival.RawHtmlArchiver
import pl.marcin.investmentmonitor.correlation.InvestmentCorrelator
import pl.marcin.investmentmonitor.detection.ChangeDetector
import pl.marcin.investmentmonitor.domain.Correlation
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.domain.SourceEvidence
import pl.marcin.investmentmonitor.persistence.CorrelationRepository
import pl.marcin.investmentmonitor.persistence.EvidenceRepository
import pl.marcin.investmentmonitor.persistence.InvestmentRepository
import pl.marcin.investmentmonitor.persistence.MonitoringRunRepository
import pl.marcin.investmentmonitor.persistence.RunStatus
import pl.marcin.investmentmonitor.persistence.SignalRepository
import pl.marcin.investmentmonitor.persistence.SourceSnapshot
import pl.marcin.investmentmonitor.persistence.SourceSnapshotRepository
import pl.marcin.investmentmonitor.source.AggregatorSource
import pl.marcin.investmentmonitor.source.DiscoverySource
import pl.marcin.investmentmonitor.source.InvestmentDetailEnricher
import pl.marcin.investmentmonitor.source.InvestmentSource
import pl.marcin.investmentmonitor.source.SourceRegistry
import pl.marcin.investmentmonitor.testsupport.testInvestment
import pl.marcin.investmentmonitor.testsupport.testSignal
import pl.marcin.investmentmonitor.validation.SourceValidator
import java.nio.file.Path
import java.time.Instant

private class InMemoryInvestmentRepository : InvestmentRepository {
    private val bySource = mutableMapOf<String, MutableMap<String, Investment>>()

    override fun findAllBySource(source: String): Map<String, Investment> = bySource[source] ?: emptyMap()
    override fun findAll(): List<Investment> = bySource.values.flatMap { it.values }
    override fun upsert(investment: Investment, seenAt: Instant) {
        bySource.getOrPut(investment.source) { mutableMapOf() }[investment.canonicalKey] = investment
    }
    override fun findIdByCanonicalKey(canonicalKey: String): Long? =
        findAll().indexOfFirst { it.canonicalKey == canonicalKey }.takeIf { it >= 0 }?.toLong()?.plus(1)
}

private class InMemorySourceSnapshotRepository : SourceSnapshotRepository {
    private val snapshots = mutableMapOf<String, SourceSnapshot>()
    override fun find(source: String): SourceSnapshot? = snapshots[source]
    override fun save(snapshot: SourceSnapshot) { snapshots[snapshot.source] = snapshot }
}

private class InMemoryMonitoringRunRepository : MonitoringRunRepository {
    var lastStatus: RunStatus? = null
    override fun start(startedAt: Instant): Long = 1L
    override fun finish(id: Long, finishedAt: Instant, status: RunStatus, sourcesChecked: Int, sourcesFailed: Int, newInvestments: Int) {
        lastStatus = status
    }
}

private class InMemorySignalRepository : SignalRepository {
    private val bySource = mutableMapOf<String, MutableMap<String, InvestmentSignal>>()
    override fun findAllBySource(source: String): Map<String, InvestmentSignal> = bySource[source] ?: emptyMap()
    override fun findAll(): List<InvestmentSignal> = bySource.values.flatMap { it.values }
    override fun upsert(signal: InvestmentSignal, seenAt: Instant) {
        bySource.getOrPut(signal.source) { mutableMapOf() }[signal.canonicalKey] = signal
    }
    override fun findIdByCanonicalKey(canonicalKey: String): Long? =
        findAll().indexOfFirst { it.canonicalKey == canonicalKey }.takeIf { it >= 0 }?.toLong()?.plus(1)
}

private class InMemoryEvidenceRepository : EvidenceRepository {
    val saved = mutableListOf<SourceEvidence>()
    override fun save(evidence: SourceEvidence) { saved += evidence }
    override fun findByInvestment(investmentId: Long) = saved.filter { it.investmentId == investmentId }
    override fun findBySignal(signalId: Long) = saved.filter { it.signalId == signalId }
}

private class InMemoryCorrelationRepository : CorrelationRepository {
    val saved = mutableListOf<Correlation>()
    override fun save(correlation: Correlation) { saved += correlation }
    override fun findByInvestment(investmentId: Long) = saved.filter { it.investmentId == investmentId }
    override fun exists(investmentId: Long, signalId: Long) = saved.any { it.investmentId == investmentId && it.signalId == signalId }
}

private class FakeInvestmentSource(override val id: String, private val investments: List<Investment>) : InvestmentSource {
    override fun fetch(): List<Investment> = investments
}

private class FakeDiscoverySource(
    override val id: String,
    override val municipality: String,
    private val signals: List<InvestmentSignal>
) : DiscoverySource {
    override fun fetch(): List<InvestmentSignal> = signals
}

private class FakeAggregatorSource(override val id: String, private val investments: List<Investment>) : AggregatorSource {
    override fun fetch(): List<Investment> = investments
}

class MonitoringServiceTest {

    private fun buildService(
        developerSources: List<InvestmentSource> = emptyList(),
        discoverySources: List<DiscoverySource> = emptyList(),
        aggregatorSources: List<AggregatorSource> = emptyList(),
        evidenceRepository: InMemoryEvidenceRepository = InMemoryEvidenceRepository(),
        correlationRepository: InMemoryCorrelationRepository = InMemoryCorrelationRepository()
    ): MonitoringService = MonitoringService(
        sourceRegistry = SourceRegistry(developerSources, discoverySources, aggregatorSources),
        sourceValidator = SourceValidator(),
        changeDetector = ChangeDetector(),
        detailEnricher = InvestmentDetailEnricher(emptyList()) { _ -> "" },
        investmentAnalyzer = NoOpInvestmentAnalyzer(),
        investmentRepository = InMemoryInvestmentRepository(),
        sourceSnapshotRepository = InMemorySourceSnapshotRepository(),
        monitoringRunRepository = InMemoryMonitoringRunRepository(),
        signalRepository = InMemorySignalRepository(),
        evidenceRepository = evidenceRepository,
        correlationRepository = correlationRepository,
        correlator = InvestmentCorrelator(),
        rawHtmlArchiver = RawHtmlArchiver(enabled = false, basePath = "unused", retentionDays = 1)
    )

    @Test
    fun `a scan with no sources produces a report with nothing new`() {
        val report = buildService().scan()

        report.newInvestmentCount shouldBe 0
        report.newDiscoverySignalCount shouldBe 0
        report.changedInvestmentCount shouldBe 0
    }

    @Test
    fun `detects a new investment from a developer source`() {
        val source = FakeInvestmentSource("chronos", listOf(testInvestment(name = "Aura")))
        val report = buildService(developerSources = listOf(source)).scan()

        report.newInvestmentCount shouldBe 1
        report.developerReports shouldHaveSize 1
    }

    @Test
    fun `detects a new discovery signal`() {
        val source = FakeDiscoverySource("swarzedz-wz", "Swarzędz", listOf(testSignal()))
        val report = buildService(discoverySources = listOf(source)).scan()

        report.newDiscoverySignalCount shouldBe 1
    }

    @Test
    fun `correlates a matching investment and signal by location`() {
        val investment = testInvestment(name = "OsiedleX", location = "Kruszewnia")
        val signal = testSignal(location = "Kruszewnia", title = "budowa budynku mieszkalnego jednorodzinnego")

        val developerSource = FakeInvestmentSource("chronos", listOf(investment))
        val discoverySource = FakeDiscoverySource("swarzedz-wz", "Swarzędz", listOf(signal))

        val report = buildService(
            developerSources = listOf(developerSource),
            discoverySources = listOf(discoverySource)
        ).scan()

        report.correlations shouldHaveSize 1
    }

    @Test
    fun `records evidence for a newly committed investment`() {
        val evidenceRepository = InMemoryEvidenceRepository()
        val source = FakeInvestmentSource("chronos", listOf(testInvestment(name = "Aura")))
        buildService(developerSources = listOf(source), evidenceRepository = evidenceRepository).scan()

        evidenceRepository.saved.isNotEmpty() shouldBe true
    }

    @Test
    fun `a fetch failure does not overwrite trusted state and is reported as a failure`() {
        val failingSource = object : InvestmentSource {
            override val id = "broken"
            override fun fetch(): List<Investment> = error("simulated failure")
        }
        val report = buildService(developerSources = listOf(failingSource)).scan()

        report.developerReports.single().fetchSucceeded shouldBe false
        report.sourcesFailed shouldBe 1
    }

    @Test
    fun `flags an aggregator-only investment with no matching developer location`() {
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "AggregatorOnly", location = "Mosina"))
        )
        val report = buildService(aggregatorSources = listOf(aggregatorSource)).scan()

        report.aggregatorOnlyDiscoveries shouldHaveSize 1
    }
}
