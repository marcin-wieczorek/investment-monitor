package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class VastbouwParserTest {

    private val parser = VastbouwParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/vastbouw/investment-list.html"))
    }

    @Test
    fun `parses the one active Poznań investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `derives the name from the detail URL slug`() {
        val investment = parser.parse(fixtureHtml).single()

        investment.source shouldBe "vastbouw"
        investment.developer shouldBe "Vastbouw"
        investment.name shouldBe "Osiedle Literatura"
        investment.url.toString() shouldBe "https://vastbouw.pl/inwestycje/mieszkania-domy-poznan/osiedle-literatura/"
        investment.location shouldBe "ul. Literacka, Poznań"
        investment.imageUrl shouldBe "https://vastbouw.pl/wp-content/uploads/VASTBOUW-Literatura-Nowy-Etap-Mieszkania-Poznan.jpg"
    }
}
