package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import java.nio.file.Files
import java.nio.file.Path

class PWDParserTest {

    private val parser = PWDParser()

    private val etap1Html: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/pwd/etap-1.html"))
    }
    private val etap2Html: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/pwd/etap-2.html"))
    }

    @Test
    fun `parses Etap I as a single investment aggregating unit-level area and property type`() {
        val investments = parser.parse(etap1Html, PWDSource.STAGE_1_URL)
        investments shouldHaveSize 1

        val investment = investments.first()
        investment.source shouldBe "pwd"
        investment.developer shouldBe "PWD Deweloper"
        investment.name shouldBe "Osiedle Zagajnik – Etap I"
        investment.url.toString() shouldBe PWDSource.STAGE_1_URL
        investment.location shouldBe "Poznań, Umultowo"
        investment.units shouldBe 38
        investment.propertyType shouldBe PropertyType.TERRACED
        investment.houseArea?.min shouldBe 76.09
        investment.houseArea?.max shouldBe 164.38
        investment.plotArea?.min shouldBe 306.0
        investment.plotArea?.max shouldBe 898.0
        investment.price shouldBe null
        investment.status shouldBe null
    }

    @Test
    fun `parses Etap II as a separate investment`() {
        val investments = parser.parse(etap2Html, PWDSource.STAGE_2_URL)
        investments shouldHaveSize 1

        val investment = investments.first()
        investment.name shouldBe "Osiedle Zagajnik – Etap II"
        investment.url.toString() shouldBe PWDSource.STAGE_2_URL
        investment.units shouldBe 38
    }

    @Test
    fun `excludes purely informational site-plan markers without a da-style class`() {
        // Both fixtures include a couple of "II Etap..."/"III Etap w
        // przygotowaniu" markers with no unit data - verified not counted
        // by asserting the exact unit count above already excludes them
        // (40 total divs on Etap I, only 38 have a da-style-* class).
        val investments = parser.parse(etap1Html, PWDSource.STAGE_1_URL)
        investments.first().units shouldBe 38
    }
}
