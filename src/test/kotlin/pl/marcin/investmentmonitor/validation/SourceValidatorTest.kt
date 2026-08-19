package pl.marcin.investmentmonitor.validation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

class SourceValidatorTest {

    private fun investment(name: String) = Investment(
        source = "chronos",
        developer = "Chronos",
        name = name,
        url = URI("https://example.com/$name"),
        location = null,
        propertyType = null,
        units = null,
        houseArea = null,
        plotArea = null,
        price = null,
        status = null,
        imageUrl = null
    )

    @Test
    fun `rejects suspicious drop`() {
        val result = SourceValidator().validate(listOf(investment("one")), previousCount = 10)
        result.valid shouldBe false
    }

    @Test
    fun `accepts normal result`() {
        val investments = (1..8).map { investment("i$it") }
        val result = SourceValidator().validate(investments, previousCount = 10)
        result.valid shouldBe true
    }
}
