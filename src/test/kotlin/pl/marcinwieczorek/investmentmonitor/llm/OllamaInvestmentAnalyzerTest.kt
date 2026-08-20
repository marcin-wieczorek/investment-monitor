package pl.marcinwieczorek.investmentmonitor.llm

import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer
import pl.marcinwieczorek.investmentmonitor.analysis.Priority
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.persistence.LlmAnalysisRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

private class InMemoryLlmAnalysisRepository : LlmAnalysisRepository {
    private val store = mutableMapOf<String, String>()

    override fun findCached(investmentCanonicalKey: String, model: String, promptHash: String): String? =
        store["$investmentCanonicalKey|$model|$promptHash"]

    override fun save(investmentCanonicalKey: String, model: String, promptHash: String, responseJson: String) {
        store["$investmentCanonicalKey|$model|$promptHash"] = responseJson
    }
}

private class FakeUserPreferencesRepository(
    private val profile: ReferenceInvestmentProfile? = null
) : UserPreferencesRepository {
    override fun findScoringProfile(): ReferenceInvestmentProfile? = profile
    override fun saveScoringProfile(profile: ReferenceInvestmentProfile) {}
}

class OllamaInvestmentAnalyzerTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    private fun startServer(ollamaResponseField: String): OllamaClient {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/api/generate") { exchange ->
            val body = """{"response":${ollamaResponseField}}"""
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.start()
        server = httpServer
        return OllamaClient(baseUrl = "http://127.0.0.1:${httpServer.address.port}", timeoutSeconds = 5)
    }

    @Test
    fun `uses the LLM's priority and reason when the response is well-formed`() {
        val json = """{\"attractiveness\":\"HIGH\",\"reason\":\"Great plot and location\"}"""
        val client = startServer(""""$json"""")
        val analyzer = OllamaInvestmentAnalyzer(client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(), "test-model")

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        analysis.priority shouldBe Priority.HIGH
        analysis.reason shouldBe "Great plot and location"
    }

    @Test
    fun `falls back to a deterministic priority when the LLM response is malformed`() {
        val client = startServer(""""not valid json at all"""")
        val analyzer = OllamaInvestmentAnalyzer(client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(), "test-model")

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        // Deterministic score for an investment with no fields set is 0.0 -> LOW.
        analysis.priority shouldBe Priority.LOW
        analysis.reason.contains("unusable response") shouldBe true
    }

    @Test
    fun `falls back gracefully when the LLM is unreachable`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = OllamaInvestmentAnalyzer(client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(), "test-model")

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        analysis.reason.contains("LLM unavailable") shouldBe true
        analysis.investmentScore shouldBe 0.0
    }

    @Test
    fun `scores against the user-configured reference profile, not the hardcoded default`() {
        // Regression test for the review finding that this analyzer used
        // ReferenceProfiles.DEFAULT hardcoded, ignoring UserPreferencesRepository -
        // meaning a user's Settings changes silently had no effect when the LLM was enabled.
        val client = startServer(""""not valid json at all"""")
        val customProfile = ReferenceInvestmentProfile(
            name = "custom",
            preferredPropertyTypes = setOf(PropertyType.APARTMENT),
            preferredLocationTiers = setOf(DevelopmentTier.S),
            houseAreaRange = AreaRange(30.0, 60.0),
            plotAreaRange = null,
            priceRange = PriceRange(300_000, 500_000),
            largePlotPreferred = false,
            maxDistanceFromPoznanKm = 10
        )
        val analyzer = OllamaInvestmentAnalyzer(
            client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(customProfile), "test-model"
        )

        // An APARTMENT investment matches the custom profile's preferred type, so
        // propertyTypeMatch (and thus investmentScore) differs from the DEFAULT
        // profile (which prefers TERRACED/SEMI_DETACHED/DETACHED houses instead).
        val analysis = analyzer.analyze(testInvestment(name = "A", propertyType = PropertyType.APARTMENT), locationProfile = null)

        analysis.investmentScore shouldBe 1.0
    }

    @Test
    fun `skips the Ollama call entirely and returns a deterministic result when disabled`() {
        // enabled=false must not even attempt an HTTP call - baseUrl points
        // nowhere reachable, so any attempt would time out and fail the test.
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = OllamaInvestmentAnalyzer(
            client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(),
            "test-model", enabled = false
        )
        val investment = testInvestment(
            name = "Tercja",
            propertyType = PropertyType.TERRACED,
            houseArea = AreaRange(120.0, 120.0),
            plotArea = AreaRange(500.0, 500.0),
            price = PriceRange(800_000, 800_000)
        )

        val analysis = analyzer.analyze(investment)

        analysis.investmentScore shouldNotBe null
        analysis.referenceProfileScore shouldNotBe null
        analysis.priority shouldNotBe Priority.UNKNOWN
    }

    @Test
    fun `derives HIGH priority from a strongly matching investment when disabled`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = OllamaInvestmentAnalyzer(
            client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(),
            "test-model", enabled = false
        )
        val investment = testInvestment(
            name = "Tercja",
            propertyType = PropertyType.TERRACED,
            houseArea = AreaRange(120.0, 120.0),
            plotArea = AreaRange(500.0, 500.0),
            price = PriceRange(800_000, 800_000)
        )
        val locationProfile = LocationProfile("Rabowice", DevelopmentTier.S, 9, 9, 8, 9)

        val analysis = analyzer.analyze(investment, locationProfile)

        analysis.priority shouldBe Priority.HIGH
        analysis.locationScore shouldNotBe null
    }

    @Test
    fun `derives LOW priority from a poorly matching investment when disabled`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = OllamaInvestmentAnalyzer(
            client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(),
            "test-model", enabled = false
        )

        val analysis = analyzer.analyze(testInvestment(name = "ApartamentyXYZ", propertyType = PropertyType.APARTMENT))

        analysis.priority shouldBe Priority.LOW
    }

    @Test
    fun `never fabricates a location score when no location profile is available and disabled`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = OllamaInvestmentAnalyzer(
            client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), FakeUserPreferencesRepository(),
            "test-model", enabled = false
        )

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        analysis.locationScore shouldBe null
    }
}
