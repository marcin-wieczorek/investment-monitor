package pl.marcin.investmentmonitor.source

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
    }

    @Test
    fun `parses Osiedle Gardenia despite differing markup`() {
        val gardenia = parser.parse(fixtureHtml).single { it.name == "Gardenia" }

        gardenia.url.toString() shouldBe "https://www.osiedle-gardenia.pl"
        gardenia.location shouldBe "Rokietnica ul. Szkolna"
    }

    @Test
    fun `does not duplicate investments across the media and call-to-action cards`() {
        val names = parser.parse(fixtureHtml).map { it.name }
        names shouldBe listOf("Aura", "Tercja", "Kruszewnia", "Gardenia")
    }
}
