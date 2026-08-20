package pl.marcinwieczorek.investmentmonitor.config

import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import org.junit.jupiter.api.Test

class InvestmentMonitorPropertiesTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `defaults match application yml so an empty override is always valid`() {
        val properties = InvestmentMonitorProperties()

        properties.validation.maxInvestmentDropPercentage shouldBe 50
        properties.jsoup.timeoutMs shouldBe 30_000
        properties.llm.enabled shouldBe true
        properties.archival.enabled shouldBe true
        properties.playwright.enabled shouldBe false
        properties.locationIntelligence.activityPeriodDays shouldBe 365
        properties.locationIntelligence.minSignalsForSynthesis shouldBe 2
        properties.locationIntelligence.maxLocationsPerScan shouldBe 20
        properties.locationIntelligence.hotspotTopN shouldBe 10

        validator.validate(properties) shouldBe emptySet()
    }

    @Test
    fun `rejects a drop percentage above 100`() {
        val properties = InvestmentMonitorProperties(validation = InvestmentMonitorProperties.Validation(maxInvestmentDropPercentage = 150))

        validator.validate(properties).isEmpty() shouldBe false
    }

    @Test
    fun `rejects a negative drop percentage`() {
        val properties = InvestmentMonitorProperties(validation = InvestmentMonitorProperties.Validation(maxInvestmentDropPercentage = -1))

        validator.validate(properties).isEmpty() shouldBe false
    }

    @Test
    fun `rejects a non-positive jsoup timeout`() {
        val properties = InvestmentMonitorProperties(jsoup = InvestmentMonitorProperties.Jsoup(timeoutMs = 0))

        validator.validate(properties).isEmpty() shouldBe false
    }

    @Test
    fun `rejects a blank llm base url`() {
        val properties = InvestmentMonitorProperties(llm = InvestmentMonitorProperties.Llm(baseUrl = "  "))

        validator.validate(properties).isEmpty() shouldBe false
    }

    @Test
    fun `rejects a blank archival path`() {
        val properties = InvestmentMonitorProperties(archival = InvestmentMonitorProperties.Archival(path = ""))

        validator.validate(properties).isEmpty() shouldBe false
    }
}
