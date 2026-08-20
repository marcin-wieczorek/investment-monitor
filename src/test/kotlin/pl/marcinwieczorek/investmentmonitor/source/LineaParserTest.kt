package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import java.nio.file.Files
import java.nio.file.Path

class LineaParserTest {

    private val parser = LineaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/linea/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 4
    }

    @Test
    fun `parses Dopiewiec, separating city from estate name`() {
        val lesnaPolana = parser.parse(fixtureHtml).single { it.location == "Dopiewiec" }

        lesnaPolana.source shouldBe SourceId("linea")
        lesnaPolana.developer shouldBe "Linea"
        lesnaPolana.name shouldBe "os. Dąbrówka – Leśna Polana"
        lesnaPolana.url.toString() shouldBe "https://linea-deweloper.pl/inwestycje/lesna-polana"
        lesnaPolana.status shouldBe InvestmentStatus.FOR_SALE
    }

    @Test
    fun `maps a closed-sale investment to SOLD_OUT`() {
        val osada = parser.parse(fixtureHtml).single { it.location == "Dąbrówka" }
        osada.status shouldBe InvestmentStatus.SOLD_OUT
    }
}
