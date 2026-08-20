package pl.marcinwieczorek.investmentmonitor.source.aggregator

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class RynekPierwotnyParserTest {

    private val parser = RynekPierwotnyParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/rynekpierwotny/nowe-domy-wielkopolskie-liczba-pokoi-od-4.html"))
    }

    @Test
    fun `parses every listed offer`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 10
    }

    @Test
    fun `parses name, developer, location and house area for a suburban offer`() {
        val offer = parser.parse(fixtureHtml).single { it.name == "Kameralny Gruszczyn" }

        offer.source shouldBe "rynekpierwotny"
        offer.developer shouldBe "Budopol-Poznań sp. z o.o."
        offer.location shouldBe "Gruszczyn"
        offer.houseArea?.min shouldBe 93.0
        offer.houseArea?.max shouldBe 93.0
        offer.url.toString() shouldBe
            "https://rynekpierwotny.pl/oferty/budopol-poznan-sp-z-oo/kameralny-gruszczyn-poznanski-gruszczyn-17389/"
    }

    @Test
    fun `leaves property type, units and status unset rather than guessing`() {
        val offer = parser.parse(fixtureHtml).single { it.name == "Kameralny Gruszczyn" }

        offer.propertyType shouldBe null
        offer.units shouldBe null
        offer.status shouldBe null
        offer.plotArea shouldBe null
    }
}
