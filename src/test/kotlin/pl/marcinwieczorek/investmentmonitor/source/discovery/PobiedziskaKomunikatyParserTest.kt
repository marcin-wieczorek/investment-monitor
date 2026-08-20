package pl.marcinwieczorek.investmentmonitor.source.discovery

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class PobiedziskaKomunikatyParserTest {

    private val parser = PobiedziskaKomunikatyParser()

    private val html: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/pobiedziska-komunikaty/announcements.html"))
    }

    @Test
    fun `parses every announcement on the list page`() {
        val signals = parser.parse(html, PobiedziskaKomunikatySource.LIST_URL)
        signals shouldHaveSize 4
    }

    @Test
    fun `parses a public-purpose siting announcement with its full description already in the list page`() {
        val signals = parser.parse(html, PobiedziskaKomunikatySource.LIST_URL)
        val signal = signals.first { it.title.contains("12 sierpnia 2026") }

        signal.source shouldBe SourceId("pobiedziska-komunikaty")
        signal.municipality shouldBe "Pobiedziska"
        signal.signalType shouldBe SignalType.LAND_DEVELOPMENT_SIGNAL
        // The title names both the village ("Główna") and the parent gmina
        // ("Pobiedziska", via "Miasta i Gminy Pobiedziska") - LocationCatalog.findIn
        // matches whichever it iterates to first; either is a correct
        // location (same accepted ambiguity as MurowanaGoslinaObwieszczeniaParserTest).
        signal.location shouldBe "Pobiedziska"
        signal.reference shouldBe "15/26"
        signal.url.toString() shouldBe
            "https://bip.pobiedziska.pl/a,54551,obwieszczenie-burmistrza-miasta-i-gminy-pobiedziska-o-wydaniu-w-dniu-12-sierpnia-2026-r-postanowieni.html"
        signal.detectedAt.toString() shouldBe "2026-08-19T14:15:27Z"
    }

    @Test
    fun `parses a second announcement with a different reference and location`() {
        val signals = parser.parse(html, PobiedziskaKomunikatySource.LIST_URL)
        val signal = signals.first { it.title.contains("18 sierpnia 2026") }

        signal.signalType shouldBe SignalType.LAND_DEVELOPMENT_SIGNAL
        signal.location shouldBe "Pobiedziska"
        signal.reference shouldBe "24/2026"
    }
}
