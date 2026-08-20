package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import java.net.URI

/**
 * Parses the SIVANET single-investment landing page.
 *
 * SIVANET (verified against the live homepage at `https://sivanet.pl`) has
 * exactly one active investment - `Lechicka 65` - published as a dedicated
 * one-page site at [SivanetSource.LIST_URL], not a list of cards. Every
 * fact is rendered as an `.atile` block with a `.lbl`/`.val` pair, keyed
 * off the label text so the parser survives layout re-ordering.
 *
 * The "Oferta specjalna" price tile only publishes a per-square-metre
 * starting price ("Od 10 950 PLN/m2"), not a total price range, so
 * [Investment.price] stays null rather than deriving a total from it.
 */
class SivanetParser {

    fun parse(html: String, baseUri: String = SivanetSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        val name = document.selectFirst("h1.t-display")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return emptyList()

        return listOf(
            Investment(
                source = SOURCE_ID,
                developer = DEVELOPER_NAME,
                name = name,
                url = URI(baseUri),
                location = tileValue(document, "Lokalizacja"),
                propertyType = PropertyType.APARTMENT,
                units = tileValue(document, "Liczba lokali")?.let { UNIT_COUNT.find(it)?.value?.toIntOrNull() },
                houseArea = tileValue(document, "Powierzchnia mieszkania")?.let(::parseAreaRange),
                plotArea = null,
                price = null,
                status = tileValue(document, "Status")?.let(::toStatus),
                imageUrl = document.selectFirst(".ab-photo img")?.absUrl("src")?.takeIf(String::isNotBlank)
            )
        )
    }

    private fun tileValue(document: Document, label: String): String? =
        document.select(".atile").firstOrNull { it.selectFirst(".lbl")?.text()?.trim() == label }
            ?.selectFirst(".val")?.text()?.trim()?.ifBlank { null }

    private fun parseAreaRange(text: String): AreaRange? {
        val match = AREA_RANGE.find(text) ?: return null
        val (min, max) = match.destructured
        return AreaRange(min.replace(',', '.').toDouble(), max.replace(',', '.').toDouble())
    }

    private fun toStatus(text: String): InvestmentStatus = when {
        text.contains("na przedaż", ignoreCase = true) -> InvestmentStatus.FOR_SALE
        text.contains("sprzeda", ignoreCase = true) && text.contains("zakończ", ignoreCase = true) ->
            InvestmentStatus.SOLD_OUT
        else -> InvestmentStatus.UNKNOWN
    }

    companion object {
        const val SOURCE_ID = "sivanet"
        const val DEVELOPER_NAME = "SIVANET"
        private val AREA_RANGE = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*[–-]\\s*([0-9]+(?:[.,][0-9]+)?)")
        private val UNIT_COUNT = Regex("[0-9]+")
    }
}
