package pl.marcin.investmentmonitor.llm

import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.analysis.DeterministicScorer
import pl.marcin.investmentmonitor.analysis.Priority
import pl.marcin.investmentmonitor.persistence.LlmAnalysisRepository
import pl.marcin.investmentmonitor.testsupport.testInvestment
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
        val analyzer = OllamaInvestmentAnalyzer(client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), "test-model")

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        analysis.priority shouldBe Priority.HIGH
        analysis.reason shouldBe "Great plot and location"
    }

    @Test
    fun `falls back to a deterministic priority when the LLM response is malformed`() {
        val client = startServer(""""not valid json at all"""")
        val analyzer = OllamaInvestmentAnalyzer(client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), "test-model")

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        // Deterministic score for an investment with no fields set is 0.0 -> LOW.
        analysis.priority shouldBe Priority.LOW
        analysis.reason.contains("unusable response") shouldBe true
    }

    @Test
    fun `falls back gracefully when the LLM is unreachable`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        val analyzer = OllamaInvestmentAnalyzer(client, DeterministicScorer(), InMemoryLlmAnalysisRepository(), "test-model")

        val analysis = analyzer.analyze(testInvestment(name = "A"), locationProfile = null)

        analysis.reason.contains("LLM unavailable") shouldBe true
        analysis.investmentScore shouldBe 0.0
    }
}
