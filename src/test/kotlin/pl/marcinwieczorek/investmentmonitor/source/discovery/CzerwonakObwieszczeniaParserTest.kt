package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class CzerwonakObwieszczeniaParserTest {

    private val parser = RekordBipParser(CzerwonakObwieszczeniaSource.MUNICIPALITY, CzerwonakObwieszczeniaSource.SOURCE_ID)

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/czerwonak-obwieszczenia/announcements.html"))
    }

    @Test
    fun `parses every published announcement`() {
        val signals = parser.parse(fixtureHtml, CzerwonakObwieszczeniaSource.LIST_URL)
        signals shouldHaveSize 10
    }

    @Test
    fun `parses a decision announcement with reference and date`() {
        val signal = parser.parse(fixtureHtml, CzerwonakObwieszczeniaSource.LIST_URL)
            .single { it.reference == "31027.2026" }

        signal.source shouldBe "czerwonak-obwieszczenia"
        signal.municipality shouldBe "Czerwonak"
        signal.title shouldBe "obwieszczenie Wójta Gminy Czerwonak o wydaniu decyzji WRO.6831.123.2025"
        signal.url.toString() shouldBe "https://bip.czerwonak.pl/6469/dokument/42032"
    }

    @Test
    fun `accepts non-ASCII case-style symbols alongside bare numeric ones`() {
        val signals = parser.parse(fixtureHtml, CzerwonakObwieszczeniaSource.LIST_URL)
        signals.map { it.reference } shouldBe listOf(
            "31027.2026", "30748.2026", "30747.2026", "30890.2025", "WOŚ.6220.8.2025",
            "29790.2025", "WOŚ.6220.31.2025", "WOŚ.6220.29.2025", "16636.2025", "15732.2025"
        )
    }
}
