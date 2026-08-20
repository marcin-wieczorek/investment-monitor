package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SagarisParserTest {

    private val parser = SagarisParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/sagaris/investment-list.html"))
    }

    @Test
    fun `parses the published investment without duplication`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `parses Niedzialkowskiego Park with units and house area`() {
        val park = parser.parse(fixtureHtml).single()

        park.source shouldBe SourceId("sagaris")
        park.developer shouldBe "Sagaris"
        park.name shouldBe "Niedziałkowskiego Park"
        park.units shouldBe 247
        park.houseArea?.min shouldBe 28.0
        park.houseArea?.max shouldBe 115.0
    }
}
