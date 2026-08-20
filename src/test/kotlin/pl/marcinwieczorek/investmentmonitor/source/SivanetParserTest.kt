package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import java.nio.file.Files
import java.nio.file.Path

class SivanetParserTest {

    private val parser = SivanetParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/sivanet/investment-list.html"))
    }

    @Test
    fun `parses the single Lechicka 65 investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 1
    }

    @Test
    fun `parses every published fact`() {
        val investment = parser.parse(fixtureHtml).single()

        investment.source shouldBe "sivanet"
        investment.developer shouldBe "SIVANET"
        investment.name shouldBe "Lechicka 65"
        investment.url.toString() shouldBe "https://sivanet.pl/nieruchomosci/lechicka-65/"
        investment.location shouldBe "Poznań, Piątkowo"
        investment.propertyType shouldBe PropertyType.APARTMENT
        investment.units shouldBe 190
        investment.houseArea shouldBe AreaRange(29.0, 93.0)
        investment.plotArea shouldBe null
        investment.price shouldBe null
        investment.status shouldBe InvestmentStatus.FOR_SALE
        investment.imageUrl shouldBe "https://sivanet.pl/Zdjecia-video/lechicka-park.webp"
    }
}
