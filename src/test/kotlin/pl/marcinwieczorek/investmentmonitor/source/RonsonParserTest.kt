package pl.marcinwieczorek.investmentmonitor.source

import java.net.URI

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import java.nio.file.Files
import java.nio.file.Path

class RonsonParserTest {

    private val parser = RonsonParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/ronson/investment-list.html"))
    }

    @Test
    fun `parses the one active Poznań investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `parses Grunwald Między Drzewami`() {
        val investment = parser.parse(fixtureHtml).single()

        investment.source shouldBe SourceId("ronson")
        investment.developer shouldBe "Ronson"
        investment.name shouldBe "Grunwald Między Drzewami"
        investment.url.toString() shouldBe "https://ronson.pl/inwestycja/grunwald-miedzy-drzewami/"
        investment.location shouldBe "Poznań, Grunwald, ul. Unii Europejskiej 2"
        investment.propertyType shouldBe PropertyType.APARTMENT
        investment.units shouldBe null
        investment.houseArea shouldBe null
        investment.price shouldBe null
        investment.status shouldBe null
        investment.imageUrl shouldBe URI("https://ronson.pl/wp-content/uploads/2024/05/RONSON-GMD-II-U2-Final-02-2048x2048.webp")
    }
}
