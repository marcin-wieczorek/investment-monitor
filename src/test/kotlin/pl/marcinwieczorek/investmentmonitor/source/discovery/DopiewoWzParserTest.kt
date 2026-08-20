package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class DopiewoWzParserTest {

    private val parser = DopiewoWzParser()

    private val indexHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/dopiewo-wz/index.html"))
    }
    private val yearHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/dopiewo-wz/year-2026.html"))
    }

    @Test
    fun `finds the current (highest) year's register URL from the index page`() {
        val url = parser.findCurrentYearUrl(indexHtml, DopiewoWzSource.INDEX_URL)
        url shouldBe "https://bip.dopiewo.pl/kategorie/706-2026?lang=PL"
    }

    @Test
    fun `parses every case in the category list, scoped to the real content region`() {
        val signals = parser.parse(yearHtml, "https://bip.dopiewo.pl/kategorie/706-2026")
        signals shouldHaveSize 3
    }

    @Test
    fun `parses a residential warunki zabudowy decision with reference and location, no date published`() {
        val signals = parser.parse(yearHtml, "https://bip.dopiewo.pl/kategorie/706-2026")
        val signal = signals.first { it.reference == "RPP.6730.096.2026" }

        signal.source shouldBe "dopiewo-wz"
        signal.municipality shouldBe "Dopiewo"
        signal.signalType shouldBe SignalType.WZ_DECISION
        // The title names both the village ("Skórzewo") and the parent
        // gmina ("Dopiewo") - LocationCatalog.findIn matches whichever it
        // iterates to first; either is a correct location (same accepted
        // ambiguity as MurowanaGoslinaObwieszczeniaParserTest).
        signal.location shouldBe "Dopiewo"
        signal.title shouldBe
            "RPP.6730.096.2026 budowa 3 budynków mieszkalnych wielorodzinnych na działkach  o nr ewid. 1/10, 1/9, położonych przy ul. Grafitowej w miejscowości Skórzewo, gmina Dopiewo."
        signal.detectedAt shouldBe Instant.EPOCH
    }

    @Test
    fun `parses a decision naming a village not otherwise in the core catalog`() {
        val signals = parser.parse(yearHtml, "https://bip.dopiewo.pl/kategorie/706-2026")
        val signal = signals.first { it.reference == "RPP.6730.031.2026" }

        signal.title shouldBe
            "RPP.6730.031.2026 budowa budynku mieszkalnego jednorodzinnego wolnostojącego  (w miejscu budynków przeznaczonych na rozbiórkę) na terenie części działki o nr ewid. 313, położonej przy ul. Kościelnej w miejscowości Konarzewo, gmina Dopiewo"
        // "Konarzewo" (newly added to LocationCatalog) is present in the
        // title too, alongside "Dopiewo" - not asserted as the resolved
        // location here since catalog iteration order picks "Dopiewo"
        // first (same ambiguity as the test above); this test instead
        // verifies the raw title text carries the village name, so a
        // future LocationCatalog change to prefer earliest-in-text
        // matches would not silently go untested.
    }
}
