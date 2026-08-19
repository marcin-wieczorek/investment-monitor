package pl.marcin.investmentmonitor.validation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.testsupport.testInvestment

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
}
