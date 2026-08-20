package pl.marcinwieczorek.investmentmonitor.validation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment

class SourceValidatorTest {

    @Test
    fun `rejects suspicious drop`() {
        val result = SourceValidator().validate(listOf(testInvestment(name = "one")), previousCount = 10)
        result.valid shouldBe false
    }

    @Test
    fun `accepts normal result`() {
        val investments = (1..8).map { testInvestment(name = "i$it") }
        val result = SourceValidator().validate(investments, previousCount = 10)
        result.valid shouldBe true
    }

    @Test
    fun `rejects an empty result even without a previous count`() {
        val result = SourceValidator().validate(emptyList(), previousCount = null)
        result.valid shouldBe false
        result.reason shouldBe "Source returned zero investments."
    }

    @Test
    fun `accepts the first-ever run for a source, skipping the drop check`() {
        val investments = listOf(testInvestment(name = "one"))
        val result = SourceValidator().validate(investments, previousCount = null)
        result.valid shouldBe true
    }

    @Test
    fun `accepts any result when the previous count was zero`() {
        val investments = listOf(testInvestment(name = "one"))
        val result = SourceValidator().validate(investments, previousCount = 0)
        result.valid shouldBe true
    }

    @Test
    fun `accepts growth beyond the previous count`() {
        val investments = (1..20).map { testInvestment(name = "i$it") }
        val result = SourceValidator().validate(investments, previousCount = 10)
        result.valid shouldBe true
    }

    @Test
    fun `accepts a drop exactly at the configured threshold`() {
        // previousCount=10, current=5 -> exactly 50% drop, threshold is 50 (not exceeded).
        val investments = (1..5).map { testInvestment(name = "i$it") }
        val result = SourceValidator(maxInvestmentDropPercentage = 50).validate(investments, previousCount = 10)
        result.valid shouldBe true
    }

    @Test
    fun `rejects a drop one point over the configured threshold`() {
        val investments = (1..4).map { testInvestment(name = "i$it") }
        val result = SourceValidator(maxInvestmentDropPercentage = 50).validate(investments, previousCount = 10)
        result.valid shouldBe false
    }

    @Test
    fun `respects a custom drop threshold`() {
        val investments = (1..7).map { testInvestment(name = "i$it") }
        val result = SourceValidator(maxInvestmentDropPercentage = 20).validate(investments, previousCount = 10)
        result.valid shouldBe false
    }
}
