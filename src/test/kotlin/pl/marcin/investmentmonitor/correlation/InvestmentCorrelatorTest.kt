package pl.marcin.investmentmonitor.correlation

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.CorrelationConfidence
import pl.marcin.investmentmonitor.domain.SignalType
import pl.marcin.investmentmonitor.testsupport.testInvestment
import pl.marcin.investmentmonitor.testsupport.testSignal

class InvestmentCorrelatorTest {

    private val correlator = InvestmentCorrelator()

    @Test
    fun `correlates a signal and an investment sharing the same known location`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Kruszewnia")
        val signal = testSignal(location = "Kruszewnia", title = "budowa domów jednorodzinnych")

        val correlations = correlator.correlate(listOf(investment), listOf(signal))

        correlations shouldHaveSize 1
        correlations.single().confidence shouldBe CorrelationConfidence.MEDIUM
    }

    @Test
    fun `raises confidence to HIGH when the developer name appears in the signal text`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Kruszewnia", developer = "Chronos Development")
        val signal = testSignal(location = "Kruszewnia", title = "wniosek Chronos o warunki zabudowy budynku mieszkalnego")

        val correlation = correlator.correlate(listOf(investment), listOf(signal)).single()

        correlation.confidence shouldBe CorrelationConfidence.HIGH
        correlation.matchedFeatures.any { it.startsWith("developer:") } shouldBe true
    }

    @Test
    fun `does not correlate signals and investments in different locations`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Kruszewnia")
        val signal = testSignal(location = "Suchy Las")

        correlator.correlate(listOf(investment), listOf(signal)).shouldBeEmpty()
    }

    @Test
    fun `does not correlate a signal with no recognized location`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Kruszewnia")
        val signal = testSignal(location = null)

        correlator.correlate(listOf(investment), listOf(signal)).shouldBeEmpty()
    }

    @Test
    fun `does not correlate an investment whose location is not in the catalog`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Nieznana Wieś")
        val signal = testSignal(location = "Kruszewnia")

        correlator.correlate(listOf(investment), listOf(signal)).shouldBeEmpty()
    }

    @Test
    fun `does not treat the signal's own municipality as a discriminating location match`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Swarzędz")
        val signal = testSignal(location = "Swarzędz", municipality = "Swarzędz")

        correlator.correlate(listOf(investment), listOf(signal)).shouldBeEmpty()
    }

    @Test
    fun `excludes non-residential signals such as retaining walls or utility infrastructure`() {
        val investment = testInvestment(name = "OsiedleTest", location = "Kruszewnia")
        val signal = testSignal(location = "Kruszewnia", title = "budowa murów oporowych - dz. 46 Kruszewnia")

        correlator.correlate(listOf(investment), listOf(signal)).shouldBeEmpty()
    }
}
