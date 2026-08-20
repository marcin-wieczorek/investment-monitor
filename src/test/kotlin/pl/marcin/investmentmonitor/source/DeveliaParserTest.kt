package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.InvestmentStatus
import java.nio.file.Files
import java.nio.file.Path

class DeveliaParserTest {

    private val parser = DeveliaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/develia/investment-list.html"))
    }

    @Test
    fun `parses every published investment without duplication`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 4
    }

    @Test
    fun `parses Krolowej Jadwigi 51`() {
        val krolowej = parser.parse(fixtureHtml).single { it.name == "Królowej Jadwigi 51" }

        krolowej.source shouldBe "develia"
        krolowej.developer shouldBe "Develia"
        krolowej.url.toString() shouldBe "https://develia.pl/pl/mieszkania/poznan/krolowej-jadwigi-51/"
        krolowej.location shouldBe "Królowej Jadwigi 51, Wilda, Poznań"
    }

    @Test
    fun `maps recognized readiness badges to status`() {
        val investments = parser.parse(fixtureHtml)
        investments.single { it.name == "Unii Lubelskiej Vita" }.status shouldBe InvestmentStatus.LAST_UNITS
        investments.single { it.name == "Vilda Arte" }.status shouldBe InvestmentStatus.READY_FOR_HANDOVER
    }

    @Test
    fun `leaves status null for generic marketing badges`() {
        val investments = parser.parse(fixtureHtml)
        investments.single { it.name == "Królowej Jadwigi 51" }.status shouldBe null
        investments.single { it.name == "Ptasia Vita" }.status shouldBe null
    }
}
