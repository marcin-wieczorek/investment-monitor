package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class UWIParserTest {

    private val parser = UWIParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/uwi/investment-list.html"))
    }

    @Test
    fun `parses the single active investment from the page heading`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `leaves unit-level fields null since they are JS-only`() {
        val malta = parser.parse(fixtureHtml).single()

        malta.source shouldBe SourceId("uwi")
        malta.developer shouldBe "UWI"
        malta.name shouldBe "Oferta Malta Wołkowyska"
        malta.units shouldBe null
        malta.price shouldBe null
    }
}
