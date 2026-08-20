package pl.marcinwieczorek.investmentmonitor.monitoring

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.analysis.DefaultInvestmentAnalyzer
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer
import pl.marcinwieczorek.investmentmonitor.analysis.ScoringResult
import pl.marcinwieczorek.investmentmonitor.archival.RawHtmlArchiver
import pl.marcinwieczorek.investmentmonitor.correlation.InvestmentCorrelator
import pl.marcinwieczorek.investmentmonitor.correlation.InvestmentDeduplicator
import pl.marcinwieczorek.investmentmonitor.detection.ChangeDetector
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.Correlation
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidate
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidateStatus
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentDuplicate
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.domain.SourceEvidence
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationRepository
import pl.marcinwieczorek.investmentmonitor.persistence.DeveloperCandidateRepository
import pl.marcinwieczorek.investmentmonitor.persistence.EvidenceRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentDuplicateRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentScoreRepository
import pl.marcinwieczorek.investmentmonitor.persistence.MonitoringRunRepository
import pl.marcinwieczorek.investmentmonitor.persistence.RunStatus
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SourceSnapshot
import pl.marcinwieczorek.investmentmonitor.persistence.SourceSnapshotRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import pl.marcinwieczorek.investmentmonitor.source.AggregatorSource
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import pl.marcinwieczorek.investmentmonitor.source.InvestmentDetailEnricher
import pl.marcinwieczorek.investmentmonitor.source.InvestmentSource
import pl.marcinwieczorek.investmentmonitor.source.SourceRegistry
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import pl.marcinwieczorek.investmentmonitor.validation.SourceValidator
import java.nio.file.Path
import java.time.Instant

private class InMemoryInvestmentRepository : InvestmentRepository {
    private val bySource = mutableMapOf<String, MutableMap<String, Investment>>()
    val aggregatorOnlyFlags = mutableMapOf<String, Boolean>()

    override fun findAllBySource(source: String): Map<String, Investment> = bySource[source] ?: emptyMap()
    override fun findAll(): List<Investment> = bySource.values.flatMap { it.values }
    override fun upsert(investment: Investment, seenAt: Instant) {
        bySource.getOrPut(investment.source) { mutableMapOf() }[investment.canonicalKey] = investment
    }
    override fun findIdByCanonicalKey(canonicalKey: String): Long? =
        findAll().indexOfFirst { it.canonicalKey == canonicalKey }.takeIf { it >= 0 }?.toLong()?.plus(1)
    override fun updateAggregatorOnlyDiscoveryFlag(canonicalKey: String, isAggregatorOnly: Boolean) {
        aggregatorOnlyFlags[canonicalKey] = isAggregatorOnly
    }
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
    override fun findAllWithLeadTime(): List<pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime> = emptyList()
}

private class InMemoryInvestmentDuplicateRepository : InvestmentDuplicateRepository {
    val saved = mutableListOf<InvestmentDuplicate>()
    override fun save(duplicate: InvestmentDuplicate) {
        if (exists(duplicate.investmentIdA, duplicate.investmentIdB)) return
        saved += duplicate
    }
    override fun findByInvestment(investmentId: Long) =
        saved.filter { it.investmentIdA == investmentId || it.investmentIdB == investmentId }
    override fun exists(investmentIdA: Long, investmentIdB: Long) = saved.any {
        (it.investmentIdA == investmentIdA && it.investmentIdB == investmentIdB) ||
            (it.investmentIdA == investmentIdB && it.investmentIdB == investmentIdA)
    }
}

private class InMemoryDeveloperCandidateRepository : DeveloperCandidateRepository {
    val saved = mutableListOf<DeveloperCandidate>()
    override fun save(candidate: DeveloperCandidate): Long {
        saved += candidate
        return saved.size.toLong()
    }
    override fun findAll(): List<DeveloperCandidate> = saved
    override fun findByName(developerName: String): DeveloperCandidate? =
        saved.firstOrNull { pl.marcinwieczorek.investmentmonitor.domain.DeveloperNameMatcher.matches(it.developerName, developerName) }
    override fun updateStatus(id: Long, status: DeveloperCandidateStatus) {
        val index = saved.indexOfFirst { it.id == id }
        if (index >= 0) saved[index] = saved[index].copy(status = status)
    }
}

private class InMemoryInvestmentScoreRepository : InvestmentScoreRepository {
    val saved = mutableMapOf<String, ScoringResult>()
    override fun save(investmentCanonicalKey: String, scoring: ScoringResult, scoredAt: Instant) {
        saved[investmentCanonicalKey] = scoring
    }
    override fun find(investmentCanonicalKey: String): ScoringResult? = saved[investmentCanonicalKey]
}

private class InMemoryUserPreferencesRepository(
    private var scoringProfile: ReferenceInvestmentProfile? = null
) : UserPreferencesRepository {
    override fun findScoringProfile(): ReferenceInvestmentProfile? = scoringProfile
    override fun saveScoringProfile(profile: ReferenceInvestmentProfile) {
        scoringProfile = profile
    }
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
        correlationRepository: InMemoryCorrelationRepository = InMemoryCorrelationRepository(),
        duplicateRepository: InMemoryInvestmentDuplicateRepository = InMemoryInvestmentDuplicateRepository(),
        developerCandidateRepository: InMemoryDeveloperCandidateRepository = InMemoryDeveloperCandidateRepository(),
        investmentScoreRepository: InMemoryInvestmentScoreRepository = InMemoryInvestmentScoreRepository(),
        investmentRepository: InMemoryInvestmentRepository = InMemoryInvestmentRepository()
    ): MonitoringService = MonitoringService(
        sourceRegistry = SourceRegistry(developerSources, discoverySources, aggregatorSources),
        sourceValidator = SourceValidator(),
        changeDetector = ChangeDetector(),
        detailEnricher = InvestmentDetailEnricher(emptyList()) { _ -> "" },
        investmentAnalyzer = DefaultInvestmentAnalyzer(DeterministicScorer(), InMemoryUserPreferencesRepository()),
        investmentRepository = investmentRepository,
        sourceSnapshotRepository = InMemorySourceSnapshotRepository(),
        monitoringRunRepository = InMemoryMonitoringRunRepository(),
        signalRepository = InMemorySignalRepository(),
        evidenceRepository = evidenceRepository,
        correlationRepository = correlationRepository,
        correlator = InvestmentCorrelator(),
        duplicateRepository = duplicateRepository,
        deduplicator = InvestmentDeduplicator(),
        rawHtmlArchiver = RawHtmlArchiver(enabled = false, basePath = "unused", retentionDays = 1),
        developerCandidateRepository = developerCandidateRepository,
        scorer = DeterministicScorer(),
        investmentScoreRepository = investmentScoreRepository,
        userPreferencesRepository = InMemoryUserPreferencesRepository()
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
    fun `records one evidence row per published fact, not one placeholder row`() {
        val evidenceRepository = InMemoryEvidenceRepository()
        val investment = testInvestment(
            name = "Tercja",
            location = "Rabowice",
            propertyType = PropertyType.TERRACED,
            houseArea = AreaRange(120.0, 140.0),
            price = PriceRange(800_000, 900_000)
        )
        val source = FakeInvestmentSource("chronos", listOf(investment))
        buildService(developerSources = listOf(source), evidenceRepository = evidenceRepository).scan()

        val fields = evidenceRepository.saved.map { it.fieldName }
        fields shouldContainAll listOf("name", "location", "propertyType", "houseArea", "price")
        evidenceRepository.saved.none { it.fieldName == "investment" } shouldBe true
    }

    @Test
    fun `does not fabricate evidence for unpublished fields`() {
        val evidenceRepository = InMemoryEvidenceRepository()
        val investment = testInvestment(name = "Bare")
        val source = FakeInvestmentSource("chronos", listOf(investment))
        buildService(developerSources = listOf(source), evidenceRepository = evidenceRepository).scan()

        evidenceRepository.saved.map { it.fieldName } shouldBe listOf("name")
    }

    @Test
    fun `records one evidence row per published signal fact`() {
        val evidenceRepository = InMemoryEvidenceRepository()
        val signal = testSignal(location = "Kruszewnia", reference = "WAU.6730.1.2026")
        val source = FakeDiscoverySource("swarzedz-wz", "Swarzędz", listOf(signal))
        buildService(discoverySources = listOf(source), evidenceRepository = evidenceRepository).scan()

        val fields = evidenceRepository.saved.map { it.fieldName }
        fields shouldContainAll listOf("title", "signalType", "detectedAt", "location", "reference")
        evidenceRepository.saved.none { it.fieldName == "signal" } shouldBe true
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

    @Test
    fun `persists the aggregator-only flag for every current aggregator investment, not just new ones`() {
        val investmentRepository = InMemoryInvestmentRepository()
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "AggregatorOnly", source = "rynekpierwotny", location = "Mosina"))
        )
        buildService(aggregatorSources = listOf(aggregatorSource), investmentRepository = investmentRepository).scan()

        investmentRepository.aggregatorOnlyFlags.values shouldContainAll listOf(true)
    }

    @Test
    fun `does not flag an aggregator investment whose location is already covered by a developer source`() {
        val investmentRepository = InMemoryInvestmentRepository()
        val developerSource = FakeInvestmentSource("chronos", listOf(testInvestment(name = "Aura", location = "Mosina")))
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "Covered", source = "rynekpierwotny", location = "Mosina"))
        )
        buildService(
            developerSources = listOf(developerSource),
            aggregatorSources = listOf(aggregatorSource),
            investmentRepository = investmentRepository
        ).scan()

        investmentRepository.aggregatorOnlyFlags.values shouldContainAll listOf(false)
    }

    @Test
    fun `records a developer candidate for an aggregator-only investment from an unknown developer`() {
        val candidateRepository = InMemoryDeveloperCandidateRepository()
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "AggregatorOnly", developer = "Totally Unknown Developer Sp. z o.o.", location = "Mosina"))
        )
        buildService(aggregatorSources = listOf(aggregatorSource), developerCandidateRepository = candidateRepository).scan()

        candidateRepository.saved shouldHaveSize 1
        candidateRepository.saved.single().developerName shouldBe "Totally Unknown Developer Sp. z o.o."
    }

    @Test
    fun `does not record a developer candidate for a developer already in the registry`() {
        val candidateRepository = InMemoryDeveloperCandidateRepository()
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "AggregatorOnly", developer = "Chronos Development", location = "Mosina"))
        )
        buildService(aggregatorSources = listOf(aggregatorSource), developerCandidateRepository = candidateRepository).scan()

        candidateRepository.saved shouldHaveSize 0
    }

    @Test
    fun `does not record a duplicate candidate for the same developer under a slightly different legal name`() {
        val candidateRepository = InMemoryDeveloperCandidateRepository()
        candidateRepository.save(
            DeveloperCandidate(
                developerName = "Totally Unknown Developer Sp. z o.o.",
                discoveredUrl = java.net.URI("https://example.com/already-known"),
                municipality = "Mosina",
                discoveredFromSource = "rynekpierwotny",
                discoveredAt = java.time.Instant.EPOCH
            )
        )
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "AggregatorOnly", developer = "Totally Unknown Developer", location = "Mosina"))
        )
        buildService(aggregatorSources = listOf(aggregatorSource), developerCandidateRepository = candidateRepository).scan()

        candidateRepository.saved shouldHaveSize 1
    }

    @Test
    fun `persists a deterministic score for a newly detected investment even without an LLM configured`() {
        val scoreRepository = InMemoryInvestmentScoreRepository()
        val investment = testInvestment(name = "Aura", location = "Kruszewnia")
        val source = FakeInvestmentSource("chronos", listOf(investment))

        buildService(developerSources = listOf(source), investmentScoreRepository = scoreRepository).scan()

        val score = scoreRepository.saved[investment.canonicalKey]
        score shouldNotBe null
    }

    @Test
    fun `flags a cross-source duplicate between a developer investment and an aggregator listing of the same project`() {
        val duplicateRepository = InMemoryInvestmentDuplicateRepository()
        val developerSource = FakeInvestmentSource(
            "chronos",
            listOf(testInvestment(name = "Tercja", source = "chronos", developer = "Chronos Development", location = "Kruszewnia"))
        )
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "Osiedle Tercja", source = "rynekpierwotny", developer = "Chronos Development", location = "Kruszewnia", url = java.net.URI("https://rynekpierwotny.pl/osiedle-tercja")))
        )

        val report = buildService(
            developerSources = listOf(developerSource),
            aggregatorSources = listOf(aggregatorSource),
            duplicateRepository = duplicateRepository
        ).scan()

        report.duplicates shouldHaveSize 1
        duplicateRepository.saved shouldHaveSize 1
    }

    @Test
    fun `does not flag two unrelated investments from different sources as duplicates`() {
        val duplicateRepository = InMemoryInvestmentDuplicateRepository()
        val developerSource = FakeInvestmentSource(
            "chronos",
            listOf(testInvestment(name = "Aura", source = "chronos", developer = "Chronos Development", location = "Kruszewnia"))
        )
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(testInvestment(name = "Zielona Dolina", source = "rynekpierwotny", developer = "Unknown (RynekPierwotny)", location = "Mosina", url = java.net.URI("https://rynekpierwotny.pl/zielona-dolina")))
        )

        val report = buildService(
            developerSources = listOf(developerSource),
            aggregatorSources = listOf(aggregatorSource),
            duplicateRepository = duplicateRepository
        ).scan()

        report.duplicates.isEmpty() shouldBe true
        duplicateRepository.saved.isEmpty() shouldBe true
    }

    @Test
    fun `enriches a developer investment's missing price from a HIGH-confidence duplicate`() {
        val investmentRepository = InMemoryInvestmentRepository()
        val developerSource = FakeInvestmentSource(
            "chronos",
            listOf(testInvestment(name = "Tercja", source = "chronos", developer = "Chronos Development", location = "Kruszewnia"))
        )
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(
                testInvestment(
                    name = "Osiedle Tercja",
                    source = "rynekpierwotny",
                    developer = "Chronos Development",
                    location = "Kruszewnia",
                    url = java.net.URI("https://rynekpierwotny.pl/osiedle-tercja"),
                    price = PriceRange(800_000, 900_000)
                )
            )
        )

        buildService(
            developerSources = listOf(developerSource),
            aggregatorSources = listOf(aggregatorSource),
            investmentRepository = investmentRepository
        ).scan()

        val chronosTercja = investmentRepository.findAll().single { it.source == "chronos" }
        chronosTercja.price shouldBe PriceRange(800_000, 900_000)
    }

    @Test
    fun `never overwrites a field a developer already published via cross-source enrichment`() {
        val investmentRepository = InMemoryInvestmentRepository()
        val developerSource = FakeInvestmentSource(
            "chronos",
            listOf(
                testInvestment(
                    name = "Tercja",
                    source = "chronos",
                    developer = "Chronos Development",
                    location = "Kruszewnia",
                    price = PriceRange(700_000, 750_000)
                )
            )
        )
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(
                testInvestment(
                    name = "Osiedle Tercja",
                    source = "rynekpierwotny",
                    developer = "Chronos Development",
                    location = "Kruszewnia",
                    url = java.net.URI("https://rynekpierwotny.pl/osiedle-tercja"),
                    price = PriceRange(800_000, 900_000)
                )
            )
        )

        buildService(
            developerSources = listOf(developerSource),
            aggregatorSources = listOf(aggregatorSource),
            investmentRepository = investmentRepository
        ).scan()

        val chronosTercja = investmentRepository.findAll().single { it.source == "chronos" }
        chronosTercja.price shouldBe PriceRange(700_000, 750_000)
    }

    @Test
    fun `does not enrich from a MEDIUM-confidence duplicate`() {
        val investmentRepository = InMemoryInvestmentRepository()
        val developerSource = FakeInvestmentSource(
            "chronos",
            listOf(testInvestment(name = "AuraEtap1", source = "chronos", developer = "Chronos Development Sp. z o.o.", location = "Kruszewnia"))
        )
        val aggregatorSource = FakeAggregatorSource(
            "rynekpierwotny",
            listOf(
                testInvestment(
                    name = "AuraEtap2",
                    source = "rynekpierwotny",
                    developer = "Chronos Development",
                    location = "Kruszewnia",
                    url = java.net.URI("https://rynekpierwotny.pl/aura-etap-2"),
                    price = PriceRange(800_000, 900_000)
                )
            )
        )

        val report = buildService(
            developerSources = listOf(developerSource),
            aggregatorSources = listOf(aggregatorSource),
            investmentRepository = investmentRepository
        ).scan()

        report.duplicates.single().confidence shouldBe pl.marcinwieczorek.investmentmonitor.domain.DuplicateConfidence.MEDIUM
        val chronosAura = investmentRepository.findAll().single { it.source == "chronos" }
        chronosAura.price shouldBe null
    }
}
