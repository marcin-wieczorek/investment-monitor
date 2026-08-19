package pl.marcin.investmentmonitor.llm

import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class OllamaClientTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    private fun startServer(path: String, status: Int, body: String): Int {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext(path) { exchange ->
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.start()
        server = httpServer
        return httpServer.address.port
    }

    @Test
    fun `parses a well-formed Ollama response`() {
        val port = startServer("/api/generate", 200, """{"response":"{\"attractiveness\":\"HIGH\"}"}""")
        val client = OllamaClient(baseUrl = "http://127.0.0.1:$port", timeoutSeconds = 5)

        client.generate("qwen2.5:7b", "prompt") shouldBe """{"attractiveness":"HIGH"}"""
    }

    @Test
    fun `returns null when the server responds with an error status`() {
        val port = startServer("/api/generate", 500, "internal error")
        val client = OllamaClient(baseUrl = "http://127.0.0.1:$port", timeoutSeconds = 5)

        client.generate("qwen2.5:7b", "prompt") shouldBe null
    }

    @Test
    fun `returns null when the server is unreachable`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        client.generate("qwen2.5:7b", "prompt") shouldBe null
    }

    @Test
    fun `isAvailable reflects server reachability`() {
        val port = startServer("/api/tags", 200, "{}")
        val client = OllamaClient(baseUrl = "http://127.0.0.1:$port", timeoutSeconds = 5)

        client.isAvailable() shouldBe true
    }

    @Test
    fun `isAvailable is false when unreachable`() {
        val client = OllamaClient(baseUrl = "http://127.0.0.1:1", timeoutSeconds = 2)
        client.isAvailable() shouldBe false
    }
}
