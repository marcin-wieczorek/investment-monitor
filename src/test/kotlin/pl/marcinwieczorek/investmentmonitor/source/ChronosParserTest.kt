package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ChronosParserTest {

    private val parser = ChronosParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/chronos/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 4
    }

    @Test
    fun `parses Tercja`() {
        val tercja = parser.parse(fixtureHtml).single { it.name == "Tercja" }

        tercja.source shouldBe "chronos"
        tercja.developer shouldBe "Chronos Development"
        tercja.url.toString() shouldBe "https://www.tercja.eu"
        tercja.location shouldBe "Swarzędz – Rabowice, ul. Swanka"
        tercja.imageUrl shouldBe "https://www.chronos.poznan.pl/photo/420x320/1e181add05b87f5501a59dc4259c7cb6.jpg/1"
    }

    @Test
    fun `parses Osiedle Gardenia despite differing markup`() {
        val gardenia = parser.parse(fixtureHtml).single { it.name == "Gardenia" }

        gardenia.url.toString() shouldBe "https://www.osiedle-gardenia.pl"
        gardenia.location shouldBe "Rokietnica ul. Szkolna"
        gardenia.imageUrl shouldBe "https://www.chronos.poznan.pl/tinyfinder/assets/uploads/img/gardenia-video.jpg"
    }

    @Test
    fun `does not duplicate investments across the media and call-to-action cards`() {
        val names = parser.parse(fixtureHtml).map { it.name }
        names shouldBe listOf("Aura", "Tercja", "Kruszewnia", "Gardenia")
    }
}
