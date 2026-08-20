package pl.marcinwieczorek.investmentmonitor.analysis

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.Correlation
import pl.marcinwieczorek.investmentmonitor.domain.CorrelationConfidence
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private class InMemoryInvestmentRepository(private val investments: List<Investment>) : InvestmentRepository {
    override fun findAllBySource(source: SourceId): Map<String, Investment> =
        investments.filter { it.source == source }.associateBy { it.canonicalKey }
    override fun findAll(): List<Investment> = investments
    override fun upsert(investment: Investment, seenAt: Instant) {}
    override fun findIdByCanonicalKey(canonicalKey: String): Long? = null
    override fun updateAggregatorOnlyDiscoveryFlag(canonicalKey: String, isAggregatorOnly: Boolean) {}
}

private class InMemorySignalRepository(private val signals: List<InvestmentSignal>) : SignalRepository {
    override fun findAllBySource(source: SourceId): Map<String, InvestmentSignal> =
        signals.filter { it.source == source }.associateBy { it.canonicalKey }
    override fun findAll(): List<InvestmentSignal> = signals
    override fun upsert(signal: InvestmentSignal, seenAt: Instant) {}
    override fun findIdByCanonicalKey(canonicalKey: String): Long? = null
}

private class InMemoryCorrelationRepository(private val leadTimes: List<CorrelationLeadTime>) : CorrelationRepository {
    override fun save(correlation: Correlation) {}
    override fun findByInvestment(investmentId: Long): List<Correlation> = emptyList()
    override fun exists(investmentId: Long, signalId: Long): Boolean = false
    override fun findAllWithLeadTime(): List<CorrelationLeadTime> = leadTimes
}

class LocationActivityCollectorTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `groups a village signal under its parent municipality`() {
        val signal = testSignal(location = "Jasin", municipality = "Swarzędz", detectedAt = Instant.parse("2026-01-01T00:00:00Z"))
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(emptyList()),
            InMemorySignalRepository(listOf(signal)),
            InMemoryCorrelationRepository(emptyList()),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val activity = collector.collectForLocation("Jasin")

        activity.municipality shouldBe "Swarzędz"
        activity.signals shouldContainExactly listOf(signal)
    }

    @Test
    fun `excludes signals older than the activity period`() {
        val recent = testSignal(
            url = java.net.URI("https://example.com/recent"),
            location = "Kruszewnia",
            detectedAt = Instant.parse("2026-05-01T00:00:00Z")
        )
        val stale = testSignal(
            url = java.net.URI("https://example.com/stale"),
            location = "Kruszewnia",
            detectedAt = Instant.parse("2020-01-01T00:00:00Z")
        )
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(emptyList()),
            InMemorySignalRepository(listOf(recent, stale)),
            InMemoryCorrelationRepository(emptyList()),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val activity = collector.collectForLocation("Kruszewnia")

        activity.signals shouldContainExactly listOf(recent)
    }

    @Test
    fun `includes investments regardless of age`() {
        val investment = testInvestment(name = "OldInvestment", location = "Kruszewnia")
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(listOf(investment)),
            InMemorySignalRepository(emptyList()),
            InMemoryCorrelationRepository(emptyList()),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val activity = collector.collectForLocation("Kruszewnia")

        activity.investments shouldContainExactly listOf(investment)
    }

    @Test
    fun `computes dominant signal types and active developers`() {
        val investment = testInvestment(name = "A", location = "Kruszewnia", developer = "Chronos")
        val wz1 = testSignal(url = java.net.URI("https://example.com/1"), location = "Kruszewnia", signalType = SignalType.WZ_DECISION)
        val wz2 = testSignal(url = java.net.URI("https://example.com/2"), location = "Kruszewnia", signalType = SignalType.WZ_DECISION)
        val other = testSignal(url = java.net.URI("https://example.com/3"), location = "Kruszewnia", signalType = SignalType.OTHER)
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(listOf(investment)),
            InMemorySignalRepository(listOf(wz1, wz2, other)),
            InMemoryCorrelationRepository(emptyList()),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val activity = collector.collectForLocation("Kruszewnia")

        activity.dominantSignalTypes.first() shouldBe SignalType.WZ_DECISION
        activity.activeDevelopers shouldContain "Chronos"
    }

    @Test
    fun `computes average lead time from matching correlations only`() {
        val leadTime = CorrelationLeadTime(
            investmentName = "A", signalTitle = "S", leadTimeDays = 30, investmentLocation = "Kruszewnia"
        )
        val elsewhere = CorrelationLeadTime(
            investmentName = "B", signalTitle = "T", leadTimeDays = 999, investmentLocation = "Elsewhere"
        )
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(emptyList()),
            InMemorySignalRepository(emptyList()),
            InMemoryCorrelationRepository(listOf(leadTime, elsewhere)),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val activity = collector.collectForLocation("Kruszewnia")

        activity.averageLeadTimeDays shouldBe 30.0
    }

    @Test
    fun `collectAll discovers every location with any investment or signal`() {
        val investment = testInvestment(name = "A", location = "Kruszewnia")
        val signal = testSignal(location = "Jasin", detectedAt = Instant.parse("2026-01-01T00:00:00Z"))
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(listOf(investment)),
            InMemorySignalRepository(listOf(signal)),
            InMemoryCorrelationRepository(emptyList()),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val locations = collector.collectAll().map { it.location }

        locations shouldContain "Kruszewnia"
        locations shouldContain "Jasin"
    }

    @Test
    fun `collectActive filters out locations below the minimum signal threshold with no investments`() {
        val quiet = testSignal(
            url = java.net.URI("https://example.com/quiet"),
            location = "Jasin",
            detectedAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        val busy1 = testSignal(
            url = java.net.URI("https://example.com/busy1"),
            location = "Kruszewnia",
            detectedAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        val busy2 = testSignal(
            url = java.net.URI("https://example.com/busy2"),
            location = "Kruszewnia",
            detectedAt = Instant.parse("2026-02-01T00:00:00Z")
        )
        val collector = LocationActivityCollector(
            InMemoryInvestmentRepository(emptyList()),
            InMemorySignalRepository(listOf(quiet, busy1, busy2)),
            InMemoryCorrelationRepository(emptyList()),
            activityPeriodDays = 365,
            clock = fixedClock
        )

        val active = collector.collectActive(minSignals = 2).map { it.location }

        active shouldContain "Kruszewnia"
        active shouldHaveSize 1
    }
}
