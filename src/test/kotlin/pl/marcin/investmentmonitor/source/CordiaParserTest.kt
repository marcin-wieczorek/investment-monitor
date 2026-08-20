package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.PropertyType
import java.nio.file.Files
import java.nio.file.Path

class CordiaParserTest {

    private val parser = CordiaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/cordia/investment-list.html"))
    }

    @Test
    fun `parses the one active Poznań investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `parses Modena`() {
        val investment = parser.parse(fixtureHtml).single()

        investment.source shouldBe "cordia"
        investment.developer shouldBe "Cordia"
        investment.name shouldBe "Modena"
        investment.url.toString() shouldBe "https://cordiapolska.pl/inwestycje/modena/"
        investment.location shouldBe "Jeżyce, Poznań"
        investment.propertyType shouldBe PropertyType.APARTMENT
        investment.units shouldBe null
        investment.houseArea shouldBe null
        investment.price shouldBe null
        investment.status shouldBe null
    }
}
