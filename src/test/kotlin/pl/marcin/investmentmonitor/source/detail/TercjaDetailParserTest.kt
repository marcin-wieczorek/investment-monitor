package pl.marcin.investmentmonitor.source.detail

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.testsupport.testInvestment
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class TercjaDetailParserTest {

    private val parser = TercjaDetailParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/tercja/detail-page.html"))
    }

    private val listOnlyTercja = testInvestment(
        name = "Tercja",
        url = URI("https://www.tercja.eu"),
        location = "Swarzędz – Rabowice, ul. Swanka",
        imageUrl = "https://www.chronos.poznan.pl/photo/420x320/1e181add05b87f5501a59dc4259c7cb6.jpg/1"
    )

    @Test
    fun `supports the tercja eu domain`() {
        parser.supports(listOnlyTercja) shouldBe true
    }

    @Test
    fun `does not support other domains`() {
        val other = listOnlyTercja.copy(url = URI("https://willeaura.pl"))
        parser.supports(other) shouldBe false
    }

    @Test
    fun `enriches unit count and house area from the descriptive paragraph`() {
        val enriched = parser.enrich(listOnlyTercja, fixtureHtml)

        enriched.units shouldBe 44
        enriched.houseArea?.min shouldBe 117.0
        enriched.houseArea?.max shouldBe 139.0
    }

    @Test
    fun `enriches plot area as a lower bound per segment`() {
        val enriched = parser.enrich(listOnlyTercja, fixtureHtml)

        enriched.plotArea?.min shouldBe 400.0
        enriched.plotArea?.max shouldBe null
    }

    @Test
    fun `leaves price and property type unset rather than guessing`() {
        val enriched = parser.enrich(listOnlyTercja, fixtureHtml)

        enriched.price shouldBe null
        enriched.propertyType shouldBe null
    }
}
