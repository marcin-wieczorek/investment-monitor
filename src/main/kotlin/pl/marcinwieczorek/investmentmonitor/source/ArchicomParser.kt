package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses Archicom's Poznań investment listing
 * ([ArchicomSource.LIST_URL]).
 *
 * Structure verified against the live page, fetched via
 * [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher] (see
 * ADR-007) - Archicom's site is a client-side-rendered React/PWA
 * ("Oops! JavaScript is disabled" with plain HTTP); the listing itself is
 * a page-builder grid whose cards are otherwise unremarkable once
 * rendered.
 *
 * Each investment card renders as two anchors sharing the same `href`
 * (one wrapping the thumbnail image, one wrapping the title text) - only
 * the text anchor is selected, via `:has(span)`, which also naturally
 * dedupes the pair. The card's own CSS classes
 * (`column-root-7Tj`, `text-root-0Rs`, ...) are content-hashed
 * page-builder output and regenerate on redeploy (same reasoning as
 * `RynekPierwotnyParser`'s KDoc), so this parser never selects by them -
 * only by the stable `href` prefix and DOM shape (an outer `<span>` whose
 * own direct text is the investment name, followed by a `<br>` and a
 * nested `<span>` holding "city, district").
 *
 * The listing card publishes no price, area, unit count or property type
 * - those fields are deliberately left `null` rather than guessed (see
 * AGENTS.md "no fake implementations"), same as `RynekPierwotnyParser`.
 */
class ArchicomParser {

    fun parse(html: String, baseUri: String = ArchicomSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("a[href^=/poznan/]:has(span)")
            .distinctBy { it.absUrl("href") }
            .mapNotNull(::toInvestment)
    }

    private fun toInvestment(link: Element): Investment? {
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val outerSpan = link.selectFirst("span") ?: return null
        val name = outerSpan.ownText().trim().takeIf(String::isNotBlank) ?: return null
        // ">" (direct-child combinator) is required here, not a bare "span" -
        // Jsoup's selectFirst/select include the element itself if it
        // matches the query, so a bare "span" on outerSpan (itself a span)
        // would just return outerSpan again instead of its nested child.
        val location = outerSpan.selectFirst("> span")?.text()?.trim()?.takeIf(String::isNotBlank)
        val imageUrl = link.closest(".pagebuilder-column")
            ?.selectFirst("img")
            ?.absUrl("src")
            ?.takeIf(String::isNotBlank)

        return Investment(
            source = ArchicomSource.SOURCE_ID,
            developer = "Archicom",
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = null,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = imageUrl
        )
    }
}
