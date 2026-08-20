package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ArchicomParserTest {

    private val parser = ArchicomParser()

    private val html: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/archicom/investment-list.html"))
    }

    @Test
    fun `parses every investment card on the Poznań listing`() {
        val investments = parser.parse(html)
        investments shouldHaveSize 3
    }

    @Test
    fun `parses Apartamenty Esencja II with location and image, leaving unpublished fields null`() {
        val investments = parser.parse(html)
        val investment = investments.first { it.name == "Apartamenty Esencja II" }

        investment.source shouldBe "archicom"
        investment.developer shouldBe "Archicom"
        investment.location shouldBe "Poznań, Garbary"
        investment.url.toString() shouldBe "https://archicom.pl/poznan/esencja"
        investment.imageUrl shouldBe "https://archicom.pl/media/.renditions/wysiwyg/Poznan/esencja_822x920.png?auto=webp&format=png&quality=85"
        investment.propertyType shouldBe null
        investment.units shouldBe null
        investment.houseArea shouldBe null
        investment.plotArea shouldBe null
        investment.price shouldBe null
        investment.status shouldBe null
    }

    @Test
    fun `parses the other two investment cards without duplication from the image anchor`() {
        val investments = parser.parse(html)
        val names = investments.map { it.name }.toSet()

        names shouldBe setOf("Apartamenty Esencja II", "Osiedle Kolektyw", "Wieża Jeżyce")
    }
}
