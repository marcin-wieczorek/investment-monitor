package pl.marcinwieczorek.investmentmonitor.llm

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.analysis.ReferenceProfiles
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment

class InvestmentPromptBuilderTest {

    @Test
    fun `includes the required JSON schema so the model knows exactly what to return`() {
        val prompt = InvestmentPromptBuilder.build(testInvestment(name = "Aura"), null, ReferenceProfiles.DEFAULT)

        prompt shouldContain "\"attractiveness\":\"HIGH|MEDIUM|LOW\""
        prompt shouldContain "\"reason\":\"...\""
    }

    @Test
    fun `includes only the facts the investment actually published`() {
        val investment = testInvestment(name = "Aura", location = "Rabowice")
        val prompt = InvestmentPromptBuilder.build(investment, null, ReferenceProfiles.DEFAULT)

        prompt shouldContain "name: Aura"
        prompt shouldContain "location: Rabowice"
    }

    @Test
    fun `marks unpublished fields as unknown rather than omitting or fabricating them`() {
        val investment = testInvestment(name = "Bare")
        val prompt = InvestmentPromptBuilder.build(investment, null, ReferenceProfiles.DEFAULT)

        prompt shouldContain "propertyType: unknown"
        prompt shouldContain "houseArea: unknown"
        prompt shouldContain "plotArea: unknown"
        prompt shouldContain "price: unknown"
        prompt shouldContain "units: unknown"
    }

    @Test
    fun `omits the LocationProfile section entirely when none is available`() {
        val prompt = InvestmentPromptBuilder.build(testInvestment(name = "Aura"), null, ReferenceProfiles.DEFAULT)

        prompt shouldNotContain "LocationProfile:"
    }

    @Test
    fun `includes the LocationProfile section when one is available`() {
        val locationProfile = LocationProfile(
            name = "Rabowice", tier = DevelopmentTier.S,
            growthScore = 9, infrastructureScore = 7, transportScore = 6, familyScore = 8
        )
        val prompt = InvestmentPromptBuilder.build(testInvestment(name = "Aura"), locationProfile, ReferenceProfiles.DEFAULT)

        prompt shouldContain "LocationProfile:"
        prompt shouldContain "tier: S"
        prompt shouldContain "growthScore: 9/10"
    }

    @Test
    fun `includes the reference profile name and ranges`() {
        val prompt = InvestmentPromptBuilder.build(testInvestment(name = "Aura"), null, ReferenceProfiles.DEFAULT)

        prompt shouldContain "ReferenceInvestmentProfile (${ReferenceProfiles.DEFAULT.name})"
        prompt shouldContain "largePlotPreferred: true"
    }

    @Test
    fun `never includes raw HTML - only structured facts`() {
        val prompt = InvestmentPromptBuilder.build(testInvestment(name = "Aura"), null, ReferenceProfiles.DEFAULT)

        prompt shouldNotContain "<html"
        prompt shouldNotContain "<div"
    }
}
