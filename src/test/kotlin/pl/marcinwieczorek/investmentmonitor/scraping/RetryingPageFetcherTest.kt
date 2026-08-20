package pl.marcinwieczorek.investmentmonitor.scraping

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URI

class RetryingPageFetcherTest {

    @Test
    fun `returns the result on the first successful attempt`() {
        var calls = 0
        val fetcher = RetryingPageFetcher(
            delegate = PageFetcher { calls++; "ok" },
            sleep = { }
        )

        fetcher.fetch(URI("https://example.com")) shouldBe "ok"
        calls shouldBe 1
    }

    @Test
    fun `retries a transient failure and returns the eventual success`() {
        var calls = 0
        val fetcher = RetryingPageFetcher(
            delegate = PageFetcher {
                calls++
                if (calls < 3) throw java.io.IOException("connection reset") else "ok"
            },
            maxAttempts = 3,
            sleep = { }
        )

        fetcher.fetch(URI("https://example.com")) shouldBe "ok"
        calls shouldBe 3
    }

    @Test
    fun `propagates the failure once maxAttempts is exhausted`() {
        var calls = 0
        val fetcher = RetryingPageFetcher(
            delegate = PageFetcher { calls++; throw java.io.IOException("permanent failure") },
            maxAttempts = 3,
            sleep = { }
        )

        val error = runCatching { fetcher.fetch(URI("https://example.com")) }
        error.isFailure shouldBe true
        calls shouldBe 3
    }

    @Test
    fun `backs off with exponential delay between attempts`() {
        var calls = 0
        val delays = mutableListOf<Long>()
        val fetcher = RetryingPageFetcher(
            delegate = PageFetcher {
                calls++
                if (calls < 3) throw java.io.IOException("connection reset") else "ok"
            },
            maxAttempts = 3,
            initialBackoffMillis = 100,
            sleep = { delays += it }
        )

        fetcher.fetch(URI("https://example.com"))
        delays shouldBe listOf(100L, 200L)
    }
}
