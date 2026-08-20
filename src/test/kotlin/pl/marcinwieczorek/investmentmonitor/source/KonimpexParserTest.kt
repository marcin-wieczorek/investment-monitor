package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class KonimpexParserTest {

    private val parser = KonimpexParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/konimpex/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 16
    }

    @Test
    fun `parses Wilda Story, splitting name from district and city`() {
        val wilda = parser.parse(fixtureHtml).single { it.name == "WILDA STORY" }

        wilda.source shouldBe SourceId("konimpex")
        wilda.developer shouldBe "Konimpex-Invest"
        wilda.location shouldBe "Wilda Poznań"
    }

    @Test
    fun `strips the lightbox frame suffix from the detail URL`() {
        val wilda = parser.parse(fixtureHtml).single { it.name == "WILDA STORY" }
        wilda.url.toString() shouldBe "https://www.konimpex-invest.pl/pl/wilda-story-poznan-mieszkania-gotowe-do-odbioru-5"
    }
}
