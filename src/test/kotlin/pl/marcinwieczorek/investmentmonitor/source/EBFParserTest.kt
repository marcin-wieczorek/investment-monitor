package pl.marcinwieczorek.investmentmonitor.source

import java.net.URI

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class EBFParserTest {

    private val parser = EBFParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/ebf/investment-list.html"))
    }

    @Test
    fun `parses every published listing without duplicating the nav menu`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 4
    }

    @Test
    fun `parses Grunwald PARK with lazily-loaded image`() {
        val grunwald = parser.parse(fixtureHtml).single { it.name == "Grunwald PARK - ul. Wieruszowska 8" }

        grunwald.source shouldBe SourceId("ebf")
        grunwald.developer shouldBe "EBF Development"
        grunwald.location shouldBe "Poznań"
        grunwald.url.toString() shouldBe "https://ebfdevelopment.pl/poznan/grunwald-park-ul-wieruszowska-8"
        grunwald.imageUrl shouldBe URI("https://ebfdevelopment.pl/storage/investment/grunwald_park_zycart.webp")
    }
}
