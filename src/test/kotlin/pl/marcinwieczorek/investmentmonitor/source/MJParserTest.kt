package pl.marcinwieczorek.investmentmonitor.source

import java.net.URI

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class MJParserTest {

    private val parser = MJParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/mj/investment-list.html"))
    }

    @Test
    fun `parses all three hub investments`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 3
    }

    @Test
    fun `parses the Poznań investment hosted on its own external domain`() {
        val naramowicka = parser.parse(fixtureHtml).single { it.name == "NARAMOWICKA 100" }

        naramowicka.source shouldBe SourceId("mj")
        naramowicka.developer shouldBe "MJ Deweloper"
        naramowicka.url.toString() shouldBe "https://naramowicka100.pl/"
        naramowicka.location shouldBe null
        naramowicka.imageUrl shouldBe URI("https://mjdeweloper.pl/wp-content/uploads/2024/08/Naramowicka-100_wizualizacja_8.webp")
    }
}
