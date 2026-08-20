package pl.marcin.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class MurowanaGoslinaObwieszczeniaParserTest {

    private val parser = MurowanaGoslinaObwieszczeniaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/murowana-goslina-obwieszczenia/announcements.html"))
    }

    @Test
    fun `parses every announcement on the list page`() {
        val signals = parser.parse(fixtureHtml)
        signals shouldHaveSize 10
    }

    @Test
    fun `classifies a residential warunki zabudowy announcement`() {
        val signal = parser.parse(fixtureHtml).first { it.reference == "PP.6730.298.2025" }

        signal.source shouldBe "murowana-goslina-obwieszczenia"
        signal.municipality shouldBe "Murowana Goślina"
        signal.signalType shouldBe SignalType.WZ_DECISION
        // The title names both the village ("Wojnowo") and the parent
        // gmina ("Murowana Goślina") - LocationCatalog.findIn matches
        // whichever it iterates to first; either is a correct location.
        signal.location shouldBe "Murowana Goślina"
        signal.url.toString() shouldBe
            "https://bip.murowana-goslina.pl/wiadomosci/9179/wiadomosc/897630/obwieszczenie_o_wszczeciu_postepowania_nr_sprawy_pp67302982025_d"
    }

    @Test
    fun `classifies a public-purpose siting announcement`() {
        val signal = parser.parse(fixtureHtml).first { it.reference == "PP.6733.9.2026" }

        signal.signalType shouldBe SignalType.LAND_DEVELOPMENT_SIGNAL
        signal.location shouldBe "Murowana Goślina"
    }
}
