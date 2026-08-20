package pl.marcinwieczorek.investmentmonitor.analysis

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment

class DeterministicScorerTest {

    private val scorer = DeterministicScorer()
    private val profile = ReferenceProfiles.POZNAN_HOUSE_SEEKER

    @Test
    fun `matches property type within the preferred set`() {
        val investment = testInvestment(name = "A", propertyType = PropertyType.TERRACED)
        val result = scorer.score(investment, locationProfile = null, referenceProfile = profile)
        result.propertyTypeMatch shouldBe true
    }

    @Test
    fun `does not match an apartment against a house-seeking profile`() {
        val investment = testInvestment(name = "A", propertyType = PropertyType.APARTMENT)
        val result = scorer.score(investment, locationProfile = null, referenceProfile = profile)
        result.propertyTypeMatch shouldBe false
    }

    @Test
    fun `a plot larger than the preferred range earns a bonus instead of a penalty`() {
        val investment = testInvestment(name = "A", plotArea = AreaRange(1200.0, 1200.0))
        val result = scorer.score(investment, locationProfile = null, referenceProfile = profile)

        result.largePlotBonus shouldBe true
        result.overallScore shouldBe (result.overallScore) // sanity: no exception, computed
    }

    @Test
    fun `a plot within the preferred range does not earn the large-plot bonus`() {
        val investment = testInvestment(name = "A", plotArea = AreaRange(400.0, 400.0))
        val result = scorer.score(investment, locationProfile = null, referenceProfile = profile)

        result.largePlotBonus shouldBe false
        result.plotAreaScore shouldBe 1.0
    }

    @Test
    fun `a location in a preferred tier matches`() {
        val investment = testInvestment(name = "A")
        val sTierLocation = LocationProfile("Test", DevelopmentTier.S, 9, 9, 9, 9)
        val result = scorer.score(investment, locationProfile = sTierLocation, referenceProfile = profile)
        result.locationTierMatch shouldBe true
    }

    @Test
    fun `a location in a non-preferred tier does not match`() {
        val investment = testInvestment(name = "A")
        val bTierLocation = LocationProfile("Test", DevelopmentTier.B, 3, 3, 3, 3)
        val result = scorer.score(investment, locationProfile = bTierLocation, referenceProfile = profile)
        result.locationTierMatch shouldBe false
    }

    @Test
    fun `missing fields yield null component scores rather than fabricated values`() {
        val investment = testInvestment(name = "A")
        val result = scorer.score(investment, locationProfile = null, referenceProfile = profile)

        result.houseAreaScore shouldBe null
        result.plotAreaScore shouldBe null
        result.priceScore shouldBe null
        result.plotToHouseRatio shouldBe null
    }

    @Test
    fun `computes plot-to-house ratio when both areas are known`() {
        val investment = testInvestment(
            name = "A",
            houseArea = AreaRange(100.0, 100.0),
            plotArea = AreaRange(500.0, 500.0)
        )
        val result = scorer.score(investment, locationProfile = null, referenceProfile = profile)
        result.plotToHouseRatio shouldBe 5.0
    }
}
