package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ATALParserTest {

    private val parser = ATALParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/atal/investment-list.html"))
    }

    @Test
    fun `parses every published investment, deduplicated by site URL`() {
        val investments = parser.parse(fixtureHtml)
        // 8 cards are published, but "ATAL Idea Swarzędz"/"II" and "Naramowice Odnova"/"II"
        // are separate cards pointing at the same external site, so 6 unique URLs remain.
        investments shouldHaveSize 6
    }

    @Test
    fun `parses a Poznań investment with unit count`() {
        val unii = parser.parse(fixtureHtml).single { it.name == "ATAL Unii Lubelskiej" }

        unii.source shouldBe "atal"
        unii.developer shouldBe "ATAL"
        unii.location shouldBe "Poznań, ul. Unii Lubelskiej"
        unii.units shouldBe 291
    }

    @Test
    fun `keeps only the first stage of a multi-stage investment sharing one site`() {
        val ideaEntries = parser.parse(fixtureHtml).filter { it.url.toString().contains("atalidea.pl") }
        ideaEntries shouldHaveSize 1
    }

    @Test
    fun `leaves unpublished fields null`() {
        val unii = parser.parse(fixtureHtml).single { it.name == "ATAL Unii Lubelskiej" }
        unii.price shouldBe null
        unii.propertyType shouldBe null
        unii.houseArea shouldBe null
    }
}
