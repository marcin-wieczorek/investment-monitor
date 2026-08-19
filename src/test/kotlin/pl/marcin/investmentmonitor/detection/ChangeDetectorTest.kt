package pl.marcin.investmentmonitor.detection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

class ChangeDetectorTest {

    private val investment = Investment(
        source = "chronos",
        developer = "Chronos",
        name = "Tercja",
        url = URI("https://example.com/inwestycja/tercja"),
        location = "Rabowice",
        propertyType = null,
        units = null,
        houseArea = null,
        plotArea = null,
        price = null,
        status = null,
        imageUrl = null
    )

    @Test
    fun `detects new investment`() {
        val changes = ChangeDetector().detect(listOf(investment), emptyMap())
        changes.single().type shouldBe ChangeType.NEW
    }

    @Test
    fun `canonical key is stable`() {
        investment.canonicalKey shouldBe "chronos:https://example.com/inwestycja/tercja"
    }
}
