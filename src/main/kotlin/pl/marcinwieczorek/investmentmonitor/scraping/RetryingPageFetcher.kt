package pl.marcinwieczorek.investmentmonitor.scraping

import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Decorates any [PageFetcher] with a bounded retry-with-backoff policy for
 * transient failures (DNS blips, connection resets, read timeouts) -
 * previously a single flaky network moment could drop an entire source for
 * the whole scan, since every fetcher was a single-attempt call (see docs
 * review - "no retry logic anywhere in the scraping layer" finding).
 *
 * The final attempt's failure still propagates unchanged, so
 * [pl.marcinwieczorek.investmentmonitor.monitoring.MonitoringService]'s
 * existing per-source `runCatching` fail-closed handling is unaffected -
 * this only absorbs *transient* failures, it never turns a genuinely
 * broken source into a silent success.
 */
class RetryingPageFetcher(
    private val delegate: PageFetcher,
    private val maxAttempts: Int = 3,
    private val initialBackoffMillis: Long = 1_000,
    private val sleep: (Long) -> Unit = Thread::sleep
) : PageFetcher {

    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    }

    override fun fetch(uri: URI): String {
        var attempt = 1
        var backoff = initialBackoffMillis
        while (true) {
            try {
                return delegate.fetch(uri)
            } catch (e: Exception) {
                if (attempt >= maxAttempts) throw e
                logger.warn(
                    "Fetch attempt {}/{} failed for '{}': {} - retrying in {}ms",
                    attempt, maxAttempts, uri, e.message, backoff
                )
                sleep(backoff)
                attempt++
                backoff *= 2
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(RetryingPageFetcher::class.java)
    }
}
