package pl.marcin.investmentmonitor.analysis

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.DevelopmentTier
import pl.marcin.investmentmonitor.domain.LocationProfile
import pl.marcin.investmentmonitor.domain.PriceRange
import pl.marcin.investmentmonitor.domain.PropertyType
import pl.marcin.investmentmonitor.testsupport.testInvestment

class DefaultInvestmentAnalyzerTest {

    private val analyzer = DefaultInvestmentAnalyzer(DeterministicScorer())

    @Test
    fun `computes a real deterministic score even without an LLM configured`() {
        val investment = testInvestment(
            name = "Tercja",
            propertyType = PropertyType.TERRACED,
            houseArea = AreaRange(120.0, 120.0),
            plotArea = AreaRange(500.0, 500.0),
            price = PriceRange(800_000, 800_000)
        )

        val analysis = analyzer.analyze(investment)

        analysis.investmentScore shouldNotBe null
        analysis.referenceProfileScore shouldNotBe null
        analysis.priority shouldNotBe Priority.UNKNOWN
    }

    @Test
    fun `derives HIGH priority from a strongly matching investment`() {
        val investment = testInvestment(
            name = "Tercja",
            propertyType = PropertyType.TERRACED,
            houseArea = AreaRange(120.0, 120.0),
            plotArea = AreaRange(500.0, 500.0),
            price = PriceRange(800_000, 800_000)
        )
        val locationProfile = LocationProfile("Rabowice", DevelopmentTier.S, 9, 9, 8, 9)

        val analysis = analyzer.analyze(investment, locationProfile)

        analysis.priority shouldBe Priority.HIGH
        analysis.locationScore shouldNotBe null
    }

    @Test
    fun `derives LOW priority from a poorly matching investment`() {
        val investment = testInvestment(name = "ApartamentyXYZ", propertyType = PropertyType.APARTMENT)

        val analysis = analyzer.analyze(investment)

        analysis.priority shouldBe Priority.LOW
    }

    @Test
    fun `never fabricates a location score when no location profile is available`() {
        val investment = testInvestment(name = "A")
        val analysis = analyzer.analyze(investment, locationProfile = null)
        analysis.locationScore shouldBe null
    }
}
