package pl.marcinwieczorek.investmentmonitor.monitoring

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.analysis.LocationActivityCollector
import pl.marcinwieczorek.investmentmonitor.domain.Correlation
import pl.marcinwieczorek.investmentmonitor.domain.HotspotSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.llm.LocationSynthesisAnalyzer
import pl.marcinwieczorek.investmentmonitor.llm.OllamaClient
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.LlmAnalysisRepository
import pl.marcinwieczorek.investmentmonitor.persistence.LocationSynthesisRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import java.net.URI
import java.time.Instant

private class SynthesisFakeInvestmentRepository(private val investments: List<Investment>) : InvestmentRepository {
    override fun findAllBySource(source: SourceId): Map<String, Investment> = emptyMap()
    override fun findAll(): List<Investment> = investments
    override fun upsert(investment: Investment, seenAt: Instant) {}
    override fun findIdByCanonicalKey(canonicalKey: String): Long? = null
    override fun updateAggregatorOnlyDiscoveryFlag(canonicalKey: String, isAggregatorOnly: Boolean) {}
}

private class SynthesisFakeSignalRepository(private val signals: List<InvestmentSignal>) : SignalRepository {
    override fun findAllBySource(source: SourceId): Map<String, InvestmentSignal> = emptyMap()
    override fun findAll(): List<InvestmentSignal> = signals
    override fun upsert(signal: InvestmentSignal, seenAt: Instant) {}
    override fun findIdByCanonicalKey(canonicalKey: String): Long? = null
}

private class SynthesisFakeCorrelationRepository : CorrelationRepository {
    override fun save(correlation: Correlation) {}
    override fun findByInvestment(investmentId: Long): List<Correlation> = emptyList()
    override fun exists(investmentId: Long, signalId: Long): Boolean = false
    override fun findAllWithLeadTime(): List<CorrelationLeadTime> = emptyList()
}

private class SynthesisFakeLocationSynthesisRepository : LocationSynthesisRepository {
    val savedLocations = mutableListOf<LocationSynthesis>()
    var savedHotspot: HotspotSynthesis? = null
    override fun upsertLocation(synthesis: LocationSynthesis) {
        savedLocations += synthesis
    }
    override fun findByLocation(location: String): LocationSynthesis? = savedLocations.find { it.location == location }
    override fun findAllLocations(): List<LocationSynthesis> = savedLocations
    override fun saveHotspot(synthesis: HotspotSynthesis) {
        savedHotspot = synthesis
    }
    override fun findLatestHotspot(): HotspotSynthesis? = savedHotspot
}

private class SynthesisFakeUserPreferencesRepository : UserPreferencesRepository {
    override fun findScoringProfile(): ReferenceInvestmentProfile? = null
    override fun saveScoringProfile(profile: ReferenceInvestmentProfile) {}
}

private class SynthesisFakeLlmAnalysisRepository : LlmAnalysisRepository {
    override fun findCached(investmentCanonicalKey: String, model: String, promptHash: String): String? = null
    override fun save(investmentCanonicalKey: String, model: String, promptHash: String, responseJson: String) {}
}

class LocationSynthesisServiceTest {

    private fun buildService(investments: List<Investment>, signals: List<InvestmentSignal>): LocationSynthesisService {
        val collector = LocationActivityCollector(
            SynthesisFakeInvestmentRepository(investments),
            SynthesisFakeSignalRepository(signals),
            SynthesisFakeCorrelationRepository(),
            activityPeriodDays = 365
        )
        val analyzer = LocationSynthesisAnalyzer(
            OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 1),
            SynthesisFakeLlmAnalysisRepository(),
            model = "test-model",
            enabled = false
        )
        return LocationSynthesisService(
            collector, analyzer, SynthesisFakeLocationSynthesisRepository(), SynthesisFakeUserPreferencesRepository(),
            minSignalsForSynthesis = 2, maxLocationsPerScan = 20, hotspotTopN = 10
        )
    }

    @Test
    fun `synthesizes and persists every active location plus one hotspot ranking`() {
        val signals = listOf(
            testSignal(url = URI("https://example.com/1"), location = "Kruszewnia", detectedAt = Instant.parse("2026-01-01T00:00:00Z")),
            testSignal(url = URI("https://example.com/2"), location = "Kruszewnia", detectedAt = Instant.parse("2026-02-01T00:00:00Z"))
        )
        val service = buildService(emptyList(), signals)

        val result = service.synthesize()

        result.locationSyntheses shouldHaveSize 1
        result.locationSyntheses.first().location shouldBe "Kruszewnia"
        result.hotspotSynthesis shouldNotBe null
    }

    @Test
    fun `produces no syntheses or hotspot when nothing is active`() {
        val service = buildService(emptyList(), emptyList())

        val result = service.synthesize()

        result.locationSyntheses shouldHaveSize 0
        result.hotspotSynthesis shouldBe null
    }

    @Test
    fun `respects maxLocationsPerScan even when more locations are active`() {
        val signals = listOf("Kruszewnia", "Jasin", "Gruszczyn").flatMapIndexed { index, location ->
            listOf(
                testSignal(url = URI("https://example.com/$location-1"), location = location, detectedAt = Instant.parse("2026-01-01T00:00:00Z")),
                testSignal(url = URI("https://example.com/$location-2"), location = location, detectedAt = Instant.parse("2026-02-01T00:00:00Z"))
            )
        }
        val collector = LocationActivityCollector(
            SynthesisFakeInvestmentRepository(emptyList()),
            SynthesisFakeSignalRepository(signals),
            SynthesisFakeCorrelationRepository(),
            activityPeriodDays = 365
        )
        val analyzer = LocationSynthesisAnalyzer(
            OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 1),
            SynthesisFakeLlmAnalysisRepository(),
            model = "test-model",
            enabled = false
        )
        val service = LocationSynthesisService(
            collector, analyzer, SynthesisFakeLocationSynthesisRepository(), SynthesisFakeUserPreferencesRepository(),
            minSignalsForSynthesis = 2, maxLocationsPerScan = 2, hotspotTopN = 10
        )

        val result = service.synthesize()

        result.locationSyntheses shouldHaveSize 2
    }
}
