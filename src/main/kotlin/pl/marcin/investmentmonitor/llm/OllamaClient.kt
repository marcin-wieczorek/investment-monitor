package pl.marcin.investmentmonitor.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val format: String = "json"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaGenerateResponse(val response: String? = null)

/**
 * Thin client for a local Ollama runtime (see docs/LLM.md).
 *
 * Uses the JDK's built-in [HttpClient] rather than pulling in a full
 * Spring MVC/WebFlux stack for a single one-shot HTTP call, keeping this a
 * local-first CLI tool rather than an embedded web server (see
 * docs/ADR-001-local-first.md).
 *
 * Every call is defensive: network errors, timeouts and malformed
 * responses are all caught and surfaced as `null`, never thrown, so a
 * missing/misconfigured local LLM never breaks a deterministic scan (see
 * docs/ARCHITECTURE.md LLM role section - the analyzer that uses this
 * client falls back to [pl.marcin.investmentmonitor.analysis.DefaultInvestmentAnalyzer]
 * behaviour on any failure).
 */
@Component
class OllamaClient(
    @param:Value("\${investment-monitor.llm.base-url:http://localhost:11434}") private val baseUrl: String,
    @param:Value("\${investment-monitor.llm.timeout-seconds:60}") private val timeoutSeconds: Long
) {
    private val mapper = jacksonObjectMapper()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /** Returns the model's raw JSON text response, or null on any failure (network, timeout, malformed body). */
    fun generate(model: String, prompt: String): String? = runCatching {
        val requestBody = mapper.writeValueAsString(OllamaGenerateRequest(model = model, prompt = prompt))
        val request = HttpRequest.newBuilder(URI("$baseUrl/api/generate"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return@runCatching null

        mapper.readValue<OllamaGenerateResponse>(response.body()).response
    }.onFailure { error ->
        logger.warn("Ollama call failed: {}", error.message)
    }.getOrNull()

    /** Best-effort liveness check; never throws. */
    fun isAvailable(): Boolean = runCatching {
        val request = HttpRequest.newBuilder(URI("$baseUrl/api/tags"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        response.statusCode() in 200..299
    }.getOrDefault(false)

    private companion object {
        val logger = LoggerFactory.getLogger(OllamaClient::class.java)
    }
}
