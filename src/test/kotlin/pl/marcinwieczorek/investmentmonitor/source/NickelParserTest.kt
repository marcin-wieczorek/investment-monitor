package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class NickelParserTest {

    private val parser = NickelParser()

    private val pages: List<String> by lazy {
        (1..6).map { Files.readString(Path.of("src/test/resources/fixtures/nickel/page-$it.html")) }
    }

    @Test
    fun `finds the last page number from the pagination control`() {
        parser.findLastPage(pages[0]) shouldBe 6
    }

    @Test
    fun `parses every unit row across all six pages`() {
        val units = pages.flatMap { parser.parseUnits(it) }
        units shouldHaveSize 157
    }

    @Test
    fun `maps every id_loc filter checkbox to a real per-investment search URL`() {
        val urls = parser.findInvestmentUrls(pages[0], NickelSource.LIST_URL)

        urls["Warzelnia II"] shouldBe "https://nickel.com.pl/pl/wyszukiwarka-mieszkan?id_loc%5B%5D=28"
        urls["Naturama II"] shouldBe "https://nickel.com.pl/pl/wyszukiwarka-mieszkan?id_loc%5B%5D=26"
        urls["Osiedle Księżnej Dąbrówki"] shouldBe "https://nickel.com.pl/pl/wyszukiwarka-mieszkan?id_loc%5B%5D=22"
        // Resort locations exist in the filter too - excluded later at aggregation, not here.
        urls["Nickel Resort & Wellnest Grzybowo"] shouldBe "https://nickel.com.pl/pl/wyszukiwarka-mieszkan?id_loc%5B%5D=25"
    }

    @Test
    fun `aggregates units per investment, excluding seaside-mountain resorts`() {
        val units = pages.flatMap { parser.parseUnits(it) }
        val urls = parser.findInvestmentUrls(pages[0], NickelSource.LIST_URL)
        val investments = parser.aggregate(units, urls)

        val names = investments.map { it.name }.toSet()
        names shouldBe setOf("Osiedle Księżnej Dąbrówki", "Warzelnia II", "Naturama II")
    }

    @Test
    fun `aggregates Warzelnia II's unit count, area range and price range, leaving location-property-status null`() {
        val units = pages.flatMap { parser.parseUnits(it) }
        val urls = parser.findInvestmentUrls(pages[0], NickelSource.LIST_URL)
        val investment = parser.aggregate(units, urls).first { it.name == "Warzelnia II" }

        investment.source shouldBe "nickel"
        investment.developer shouldBe "Nickel Development"
        investment.units shouldBe 62
        investment.houseArea?.min shouldBe 30.89
        investment.houseArea?.max shouldBe 417.7
        investment.price?.min shouldBe 507523
        investment.price?.max shouldBe 5908366
        investment.url.toString() shouldBe "https://nickel.com.pl/pl/wyszukiwarka-mieszkan?id_loc%5B%5D=28"
        investment.location shouldBe null
        investment.propertyType shouldBe null
        investment.plotArea shouldBe null
        investment.status shouldBe null
        investment.imageUrl shouldBe null
    }

    @Test
    fun `aggregates the small Osiedle Ksieznej Dabrowki investment`() {
        val units = pages.flatMap { parser.parseUnits(it) }
        val urls = parser.findInvestmentUrls(pages[0], NickelSource.LIST_URL)
        val investment = parser.aggregate(units, urls).first { it.name == "Osiedle Księżnej Dąbrówki" }

        investment.units shouldBe 4
        investment.houseArea?.min shouldBe 57.24
        investment.houseArea?.max shouldBe 65.96
        investment.price?.min shouldBe 628432
        investment.price?.max shouldBe 721711
    }
}
