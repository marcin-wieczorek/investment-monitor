package pl.marcinwieczorek.investmentmonitor.source

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher

/**
 * Enriches an investment with detail-page fields when a matching
 * [InvestmentDetailParser] is registered for its URL.
 *
 * No matching parser, and any fetch/parse failure, both result in the
 * original investment being returned unchanged - detail enrichment is a
 * best-effort enhancement, never a reason to fail a scan (see
 * docs/ADR-003-fail-closed-source-validation.md: list-page identity and
 * change detection must not depend on detail-page availability).
 */
@Component
class InvestmentDetailEnricher(
    private val parsers: List<InvestmentDetailParser>,
    private val pageFetcher: PageFetcher
) {
    fun enrich(investment: Investment): Investment {
        val parser = parsers.firstOrNull { it.supports(investment) } ?: return investment

        return runCatching {
            val html = pageFetcher.fetch(investment.url)
            parser.enrich(investment, html)
        }.getOrElse { error ->
            logger.warn("Detail enrichment failed for {} ({}): {}", investment.canonicalKey, investment.url, error.message)
            investment
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(InvestmentDetailEnricher::class.java)
    }
}
