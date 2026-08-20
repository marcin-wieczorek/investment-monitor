package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import java.nio.file.Files
import java.nio.file.Path

class InwestycjeWielkopolskiParserTest {

    private val parser = InwestycjeWielkopolskiParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/inwestycje_wielkopolski/investment-list.html"))
    }

    @Test
    fun `parses all eleven completed investments`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 11
    }

    @Test
    fun `parses Plac Wolności 6`() {
        val placWolnosci = parser.parse(fixtureHtml).single { it.name == "Plac Wolności 6" }

        placWolnosci.source shouldBe "inwestycje_wielkopolski"
        placWolnosci.developer shouldBe "Inwestycje Wielkopolski"
        placWolnosci.url.toString() shouldBe "https://inwestycjewielkopolski.pl/realizacja/plac-wolnosci-6/"
        placWolnosci.location shouldBe "UL. PLAC WOLNOŚCI 6, POZNAŃ"
        placWolnosci.status shouldBe InvestmentStatus.SOLD_OUT
        placWolnosci.imageUrl shouldBe "https://inwestycjewielkopolski.pl/wp-content/uploads/2026/07/Plac-Wolnosc-6-Inwestycje-Wielkopolski-www.png"
    }
}
