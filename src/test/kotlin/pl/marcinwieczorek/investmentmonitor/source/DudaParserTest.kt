package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import java.nio.file.Files
import java.nio.file.Path

class DudaParserTest {

    private val parser = DudaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/duda/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 7
    }

    @Test
    fun `parses Dobry Szelag as for sale`() {
        val dobry = parser.parse(fixtureHtml).single { it.name.contains("DOBRY SZEL", ignoreCase = true) }

        dobry.source shouldBe "duda"
        dobry.developer shouldBe "Duda Development"
        dobry.location shouldBe "Poznań Szeląg"
        dobry.status shouldBe InvestmentStatus.FOR_SALE
        dobry.url.toString() shouldBe "https://dobryszelag.pl/"
    }

    @Test
    fun `leaves area and units null since only free-text prose is published`() {
        val dobry = parser.parse(fixtureHtml).single { it.name.contains("DOBRY SZEL", ignoreCase = true) }
        dobry.units shouldBe null
        dobry.houseArea shouldBe null
    }
}
