package pl.marcinwieczorek.investmentmonitor.source.aggregator

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class RynekPierwotnyParserTest {

    private val parser = RynekPierwotnyParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/rynekpierwotny/nowe-domy-poznan.html"))
    }

    @Test
    fun `parses every listed offer`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 10
    }

    @Test
    fun `parses name, developer, location and house area for a suburban offer`() {
        val offer = parser.parse(fixtureHtml).single { it.name == "Na Wzgórzu 2" }

        offer.source shouldBe "rynekpierwotny"
        offer.developer shouldBe "VIEW DEVELOPMENT 2.0 Sp. z o.o."
        offer.location shouldBe "Suchy Las"
        offer.houseArea?.min shouldBe 149.0
        offer.houseArea?.max shouldBe 158.0
        offer.url.toString() shouldBe
            "https://www.rynekpierwotny.pl/oferty/view-development-sp-z-oo/na-wzgorzu-2-poznanski-suchy-las-20492/"
    }

    @Test
    fun `leaves property type, units and status unset rather than guessing`() {
        val offer = parser.parse(fixtureHtml).single { it.name == "Na Wzgórzu 2" }

        offer.propertyType shouldBe null
        offer.units shouldBe null
        offer.status shouldBe null
        offer.plotArea shouldBe null
    }
}
