package pl.marcinwieczorek.investmentmonitor.detection

import io.kotest.matchers.collections.shouldHaveSize
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
    fun `detects an unchanged investment when content is identical`() {
        val changes = ChangeDetector().detect(listOf(investment), mapOf(investment.canonicalKey to investment))
        changes.single().type shouldBe ChangeType.UNCHANGED
    }

    @Test
    fun `detects a changed investment when a field differs`() {
        val updated = investment.copy(units = 42)
        val changes = ChangeDetector().detect(listOf(updated), mapOf(investment.canonicalKey to investment))

        val change = changes.single()
        change.type shouldBe ChangeType.CHANGED
        change.current shouldBe updated
        change.previous shouldBe investment
    }

    @Test
    fun `detects a removed investment absent from the current list`() {
        val changes = ChangeDetector().detect(emptyList(), mapOf(investment.canonicalKey to investment))

        val change = changes.single()
        change.type shouldBe ChangeType.REMOVED
        change.current shouldBe null
        change.previous shouldBe investment
    }

    @Test
    fun `detects new, changed, unchanged and removed together in a single call`() {
        val unchanged = testInvestment(name = "Stable")
        val changedOld = testInvestment(name = "Aura", units = 10)
        val changedNew = changedOld.copy(units = 20)
        val removed = testInvestment(name = "Gone")
        val brandNew = testInvestment(name = "Fresh")

        val previous = mapOf(
            unchanged.canonicalKey to unchanged,
            changedOld.canonicalKey to changedOld,
            removed.canonicalKey to removed
        )
        val current = listOf(unchanged, changedNew, brandNew)

        val changes = ChangeDetector().detect(current, previous)

        changes.first { it.current?.name == "Stable" }.type shouldBe ChangeType.UNCHANGED
        changes.first { it.current?.name == "Aura" }.type shouldBe ChangeType.CHANGED
        changes.first { it.current?.name == "Fresh" }.type shouldBe ChangeType.NEW
        changes.first { it.previous?.name == "Gone" }.type shouldBe ChangeType.REMOVED
    }

    @Test
    fun `both empty inputs produce no changes`() {
        ChangeDetector().detect(emptyList(), emptyMap()) shouldBe emptyList()
    }

    @Test
    fun `does not throw when the current list contains a duplicate canonicalKey`() {
        val duplicate = investment.copy(units = 99)
        val changes = ChangeDetector().detect(listOf(investment, duplicate), emptyMap())
        changes shouldHaveSize 2
    }

    @Test
    fun `canonical key is stable`() {
        investment.canonicalKey shouldBe "chronos:https://example.com/inwestycja/tercja"
    }
}
