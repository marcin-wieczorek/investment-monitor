package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
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
 * docs/ARCHITECTURE.md LLM role section - [OllamaInvestmentAnalyzer]
 * falls back to a purely deterministic result on any failure).
 *
 * LLM analysis is enabled by default
 * (`investment-monitor.llm.enabled=true`); [probeAtStartup] runs once
 * at application startup purely to log whether Ollama is actually
 * reachable, so a missing local install is visible immediately in the
 * startup log rather than silently degrading investment-by-investment.
 */
@Component
class OllamaClient(
    @param:Value("\${investment-monitor.llm.base-url:http://localhost:11434}") private val baseUrl: String,
    @param:Value("\${investment-monitor.llm.timeout-seconds:60}") private val timeoutSeconds: Long,
    @param:Value("\${investment-monitor.llm.enabled:true}") private val enabled: Boolean = true
) {
    private val mapper = jacksonObjectMapper()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    @PostConstruct
    fun probeAtStartup() {
        if (!enabled) {
            logger.info("LLM analysis disabled (investment-monitor.llm.enabled=false); using deterministic scoring only.")
            return
        }
        if (isAvailable()) {
            logger.info("Ollama reachable at {} - LLM-enhanced analysis active.", baseUrl)
        } else {
            logger.warn(
                "LLM analysis is enabled but Ollama is not reachable at {} - " +
                    "falling back to deterministic-only analysis for every investment. " +
                    "Install/start Ollama (see docs/LLM.md) or set investment-monitor.llm.enabled=false to silence this warning.",
                baseUrl
            )
        }
    }

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
