package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class JaksBudParserTest {

    private val parser = JaksBudParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/jaksbud/investment-list.html"))
    }

    @Test
    fun `parses exactly one aggregated investment from the unit table`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `aggregates unit count and house area range from the table rows`() {
        val natura = parser.parse(fixtureHtml).single()

        natura.source shouldBe "jaksbud"
        natura.developer shouldBe "JakśBud"
        natura.name shouldBe "Osiedle Natura Biedrusko"
        natura.units shouldBe 22
        natura.houseArea?.min shouldBe 60.3
        natura.houseArea?.max shouldBe 136.48
    }

    @Test
    fun `aggregates plot area range from the table rows`() {
        val natura = parser.parse(fixtureHtml).single()
        natura.plotArea?.min shouldBe 65.0
        natura.plotArea?.max shouldBe 172.0
    }
}
