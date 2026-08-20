package pl.marcinwieczorek.investmentmonitor.source

import java.net.URI

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class GreenbudParserTest {

    private val parser = GreenbudParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/greenbud/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 5
    }

    @Test
    fun `parses a single-value house area and upper-bound-only plot area`() {
        val rabowice2 = parser.parse(fixtureHtml).single { it.name == "Zielone Rabowice II – etap VII" }

        rabowice2.source shouldBe SourceId("greenbud")
        rabowice2.developer shouldBe "Greenbud Development"
        rabowice2.url.toString() shouldBe "https://www.greenbud.com.pl/zielone-rabowice-2/"
        rabowice2.location shouldBe "Swarzędz – Rabowice"
        rabowice2.houseArea?.min shouldBe 87.43
        rabowice2.houseArea?.max shouldBe 87.43
        rabowice2.plotArea?.min shouldBe null
        rabowice2.plotArea?.max shouldBe 363.0
        rabowice2.imageUrl shouldBe URI("https://www.greenbud.com.pl/wp-content/uploads/2024/05/camera_03_front1.jpg")
    }

    @Test
    fun `parses an explicit house area range`() {
        val jasin = parser.parse(fixtureHtml).single { it.name == "Nowy Jasin – etap III" }

        jasin.location shouldBe "Swarzędz – Jasin"
        jasin.houseArea?.min shouldBe 75.0
        jasin.houseArea?.max shouldBe 79.0
        jasin.plotArea?.max shouldBe 118.0
    }

    @Test
    fun `treats garden area as plot area`() {
        val botanika = parser.parse(fixtureHtml).single { it.name == "OSIEDLE BOTANIKA" }

        botanika.plotArea?.max shouldBe 526.0
    }
}
