package pl.marcinwieczorek.investmentmonitor.detection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import java.net.URI

class ChangeDetectorTest {

    private val investment = testInvestment(
        name = "Tercja",
        developer = "Chronos",
        url = URI("https://example.com/inwestycja/tercja"),
        location = "Rabowice"
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
