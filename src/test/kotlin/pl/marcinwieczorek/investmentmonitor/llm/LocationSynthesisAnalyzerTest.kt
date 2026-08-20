package pl.marcinwieczorek.investmentmonitor.llm

import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.ActivityLevel
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTrend
import pl.marcinwieczorek.investmentmonitor.domain.LocationActivity
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.persistence.LlmAnalysisRepository
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private class InMemorySynthesisLlmAnalysisRepository : LlmAnalysisRepository {
    private val store = mutableMapOf<String, String>()
    override fun findCached(investmentCanonicalKey: String, model: String, promptHash: String): String? =
        store["$investmentCanonicalKey|$model|$promptHash"]
    override fun save(investmentCanonicalKey: String, model: String, promptHash: String, responseJson: String) {
        store["$investmentCanonicalKey|$model|$promptHash"] = responseJson
    }
}

private fun testReferenceProfile(): ReferenceInvestmentProfile = ReferenceInvestmentProfile(
    name = "test-profile",
    preferredPropertyTypes = setOf(PropertyType.TERRACED),
    preferredLocationTiers = setOf(DevelopmentTier.A),
    houseAreaRange = AreaRange(100.0, 150.0),
    plotAreaRange = AreaRange(400.0, 800.0),
    priceRange = PriceRange(500_000, 900_000),
    largePlotPreferred = true,
    maxDistanceFromPoznanKm = 20
)

private val fixedClock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)

class LocationSynthesisAnalyzerTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    private fun startServer(responseJson: String): OllamaClient {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/api/generate") { exchange ->
            val body = """{"response":${responseJson}}"""
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.start()
        server = httpServer
        return OllamaClient(baseUrl = "http://127.0.0.1:${httpServer.address.port}", timeoutSeconds = 5)
    }

    private fun busyActivity(): LocationActivity = LocationActivity(
        location = "Kruszewnia",
        municipality = "Swarzędz",
        locationProfile = LocationProfile("Kruszewnia", DevelopmentTier.A, 8, 6, 6, 8),
        investments = listOf(testInvestment(name = "A", developer = "Chronos", propertyType = PropertyType.TERRACED)),
        signals = listOf(
            testSignal(url = URI("https://example.com/1"), detectedAt = Instant.parse("2026-01-01T00:00:00Z")),
            testSignal(url = URI("https://example.com/2"), detectedAt = Instant.parse("2026-02-01T00:00:00Z")),
            testSignal(url = URI("https://example.com/3"), detectedAt = Instant.parse("2026-03-01T00:00:00Z"))
        ),
        correlations = emptyList()
    )

    @Test
    fun `uses the LLM's synthesis when the response is well-formed`() {
        val json = """{\"developmentTrend\":\"ACCELERATING\",\"summary\":\"Duza aktywnosc.\",""" +
            """\"recommendedAction\":\"WATCH_CLOSELY\",\"reason\":\"Wiele sygnalow.\"}"""
        val client = startServer(""""$json"""")
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = true, clock = fixedClock)

        val synthesis = analyzer.synthesizeLocation(busyActivity(), testReferenceProfile())

        synthesis.developmentTrend shouldBe DevelopmentTrend.ACCELERATING
        synthesis.summary shouldBe "Duza aktywnosc."
        synthesis.recommendedAction shouldBe pl.marcinwieczorek.investmentmonitor.domain.RecommendedAction.WATCH_CLOSELY
    }

    @Test
    fun `falls back to a deterministic synthesis when the LLM response is malformed`() {
        val client = startServer(""""not valid json"""")
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = true, clock = fixedClock)

        val synthesis = analyzer.synthesizeLocation(busyActivity(), testReferenceProfile())

        synthesis.developmentTrend shouldBe DevelopmentTrend.ACCELERATING // 3 signals -> deterministic ACCELERATING
        synthesis.reason.contains("deterministyczna") shouldBe true
        synthesis.signalCount shouldBe 3
        synthesis.investmentCount shouldBe 1
    }

    @Test
    fun `falls back gracefully when the LLM is unreachable`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = true, clock = fixedClock)

        val synthesis = analyzer.synthesizeLocation(busyActivity(), testReferenceProfile())

        synthesis shouldNotBe null
        synthesis.summary.isNotBlank() shouldBe true
    }

    @Test
    fun `skips the Ollama call entirely and returns a deterministic result when disabled`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = false, clock = fixedClock)

        val synthesis = analyzer.synthesizeLocation(busyActivity(), testReferenceProfile())

        synthesis.reason.contains("wyłączony") shouldBe true
    }

    @Test
    fun `deterministic trend is MINIMAL for a location with no recent signals`() {
        val quiet = LocationActivity(
            location = "Jasin", municipality = "Swarzędz", locationProfile = null,
            investments = emptyList(), signals = emptyList(), correlations = emptyList()
        )
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = false, clock = fixedClock)

        val synthesis = analyzer.synthesizeLocation(quiet, testReferenceProfile())

        synthesis.developmentTrend shouldBe DevelopmentTrend.MINIMAL
        synthesis.recommendedAction shouldBe pl.marcinwieczorek.investmentmonitor.domain.RecommendedAction.LOW_PRIORITY
    }

    @Test
    fun `deterministic hotspot synthesis ranks locations by signal count and computes profile relevance`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = false, clock = fixedClock)
        val quiet = LocationActivity(
            location = "Jasin", municipality = "Swarzędz", locationProfile = null,
            investments = emptyList(), signals = listOf(testSignal(url = URI("https://example.com/quiet"))),
            correlations = emptyList()
        )

        val hotspot = analyzer.synthesizeHotspots(listOf(quiet, busyActivity()), testReferenceProfile(), topN = 10)

        hotspot.hotspots.first().location shouldBe "Kruszewnia"
        hotspot.hotspots.first().relevanceToProfile shouldBe ActivityLevel.HIGH // Tier A + TERRACED both match
    }

    @Test
    fun `deterministic hotspot synthesis handles an empty activity list`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = LocationSynthesisAnalyzer(client, InMemorySynthesisLlmAnalysisRepository(), "test-model", enabled = false, clock = fixedClock)

        val hotspot = analyzer.synthesizeHotspots(emptyList(), testReferenceProfile(), topN = 10)

        hotspot.hotspots shouldBe emptyList()
    }
}
