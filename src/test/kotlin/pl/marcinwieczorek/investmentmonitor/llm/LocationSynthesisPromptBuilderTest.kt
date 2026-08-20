package pl.marcinwieczorek.investmentmonitor.llm

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.LocationActivity
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import java.net.URI

private fun testReferenceProfile(): ReferenceInvestmentProfile = ReferenceInvestmentProfile(
    name = "test-profile",
    preferredPropertyTypes = setOf(PropertyType.TERRACED),
    preferredLocationTiers = setOf(DevelopmentTier.A),
    houseAreaRange = AreaRange(100.0, 150.0),
    plotAreaRange = AreaRange(400.0, 800.0),
    priceRange = PriceRange(500_000, 900_000),
    largePlotPreferred = true,
    maxDistanceFromPoznanKm = 20
)

class LocationSynthesisPromptBuilderTest {

    @Test
    fun `includes location name, municipality and profile scores`() {
        val activity = LocationActivity(
            location = "Kruszewnia",
            municipality = "Swarzędz",
            locationProfile = LocationProfile("Kruszewnia", DevelopmentTier.A, 8, 6, 6, 8),
            investments = emptyList(),
            signals = emptyList(),
            correlations = emptyList()
        )

        val prompt = LocationSynthesisPromptBuilder.build(activity, testReferenceProfile())

        prompt shouldContain "name: Kruszewnia"
        prompt shouldContain "municipality: Swarzędz"
        prompt shouldContain "tier: A"
        prompt shouldContain "growthScore: 8/10"
    }

    @Test
    fun `includes every signal and investment`() {
        val signal = testSignal(title = "budowa 74 budynkow", reference = "WAU.6730.23.2026")
        val investment = testInvestment(name = "Osiedle Kruszewnia", developer = "Chronos", url = URI("https://example.com/osiedle-kruszewnia"))
        val activity = LocationActivity(
            location = "Kruszewnia",
            municipality = "Swarzędz",
            locationProfile = null,
            investments = listOf(investment),
            signals = listOf(signal),
            correlations = listOf(CorrelationLeadTime("Osiedle Kruszewnia", "budowa 74 budynkow", 30, "Kruszewnia"))
        )

        val prompt = LocationSynthesisPromptBuilder.build(activity, testReferenceProfile())

        prompt shouldContain "budowa 74 budynkow"
        prompt shouldContain "WAU.6730.23.2026"
        prompt shouldContain "Osiedle Kruszewnia"
        prompt shouldContain "Chronos"
        prompt shouldContain "lead time: +30 days"
    }

    @Test
    fun `renders empty sections explicitly rather than omitting them`() {
        val activity = LocationActivity(
            location = "Jasin",
            municipality = "Swarzędz",
            locationProfile = null,
            investments = emptyList(),
            signals = emptyList(),
            correlations = emptyList()
        )

        val prompt = LocationSynthesisPromptBuilder.build(activity, testReferenceProfile())

        prompt shouldContain "(none)"
    }

    @Test
    fun `demands Polish response and a strict JSON schema`() {
        val activity = LocationActivity("X", null, null, emptyList(), emptyList(), emptyList())
        val prompt = LocationSynthesisPromptBuilder.build(activity, testReferenceProfile())

        prompt shouldContain "Respond in Polish"
        prompt shouldContain "developmentTrend"
        prompt shouldContain "recommendedAction"
    }
}

class HotspotSynthesisPromptBuilderTest {

    @Test
    fun `ranks locations by signal count and includes activity summary`() {
        val busy = LocationActivity(
            location = "Kruszewnia", municipality = "Swarzędz",
            locationProfile = LocationProfile("Kruszewnia", DevelopmentTier.A, 8, 6, 6, 8),
            investments = listOf(testInvestment(name = "A", developer = "Chronos")),
            signals = listOf(
                testSignal(url = URI("https://example.com/1")),
                testSignal(url = URI("https://example.com/2")),
                testSignal(url = URI("https://example.com/3"))
            ),
            correlations = emptyList()
        )
        val quiet = LocationActivity(
            location = "Jasin", municipality = "Swarzędz", locationProfile = null,
            investments = emptyList(), signals = listOf(testSignal(url = URI("https://example.com/4"))),
            correlations = emptyList()
        )

        val prompt = HotspotSynthesisPromptBuilder.build(listOf(quiet, busy), testReferenceProfile())

        // Kruszewnia (3 signals) must be ranked before Jasin (1 signal).
        val kruszewniaIndex = prompt.indexOf("Kruszewnia")
        val jasinIndex = prompt.indexOf("Jasin")
        (kruszewniaIndex in 0 until jasinIndex) shouldBe true
        prompt shouldContain "Chronos"
    }
    @Test
    fun `demands Polish response and a strict JSON schema`() {
        val prompt = HotspotSynthesisPromptBuilder.build(emptyList(), testReferenceProfile())

        prompt shouldContain "Respond in Polish"
        prompt shouldContain "hotspots"
        prompt shouldContain "emergingAreas"
    }
}
