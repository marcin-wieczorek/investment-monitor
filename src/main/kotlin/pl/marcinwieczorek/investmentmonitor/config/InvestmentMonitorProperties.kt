package pl.marcinwieczorek.investmentmonitor.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Typed, fail-fast binding of the `investment-monitor.*` YAML namespace
 * (see `application.yml`).
 *
 * Before this class existed, every setting was read via a scattered
 * `@Value("${'$'}{investment-monitor.foo.bar:default}")` injection with an
 * inline default - a typo in `application.yml` (e.g.
 * `investment-monitor.llm.timeout-second` instead of `timeout-seconds`)
 * would silently fall back to the default rather than failing at startup
 * (see docs review - "no @ConfigurationProperties validation" finding).
 * `@Validated` + Bean Validation annotations make Spring Boot refuse to
 * start if a bound value is out of range, and binding to a single
 * `@ConfigurationProperties` class means every property name is checked
 * against this class's structure at startup instead of only at first use.
 *
 * Deliberately *not* wired into the constructors that still take
 * `@Value`-annotated primitives directly ([pl.marcinwieczorek.investmentmonitor.validation.SourceValidator],
 * [pl.marcinwieczorek.investmentmonitor.scraping.JsoupPageFetcher], ...) -
 * those constructors are exercised directly (with plain primitive
 * defaults) by ~20 existing unit tests, and swapping them to depend on
 * this properties class would only add indirection for those tests
 * without a correctness benefit. This class's job is purely fail-fast
 * startup validation of the YAML itself.
 */
@ConfigurationProperties(prefix = "investment-monitor")
@Validated
data class InvestmentMonitorProperties(
    @field:Valid val validation: Validation = Validation(),
    @field:Valid val jsoup: Jsoup = Jsoup(),
    @field:Valid val llm: Llm = Llm(),
    @field:Valid val archival: Archival = Archival(),
    @field:Valid val playwright: Playwright = Playwright(),
    @field:Valid val locationIntelligence: LocationIntelligence = LocationIntelligence()
) {
    data class Validation(
        @field:Min(0) @field:Max(100) val maxInvestmentDropPercentage: Int = 50
    )

    data class Jsoup(
        @field:Positive val timeoutMs: Int = 30_000
    )

    data class Llm(
        val enabled: Boolean = true,
        @field:NotBlank val baseUrl: String = "http://localhost:11434",
        @field:NotBlank val model: String = "qwen2.5:7b",
        @field:Positive val timeoutSeconds: Long = 60
    )

    data class Archival(
        val enabled: Boolean = true,
        @field:NotBlank val path: String = "raw",
        @field:Positive val retentionDays: Long = 30
    )

    data class Playwright(
        val enabled: Boolean = false,
        @field:Positive val timeoutMs: Long = 30_000
    )

    /**
     * Location-intelligence synthesis (see docs/ARCHITECTURE.md phase 12
     * and `analysis/LocationActivityCollector.kt`) - LLM-assisted, per-
     * location and region-wide summaries of discovery-signal/investment
     * activity, generated once per scan.
     */
    data class LocationIntelligence(
        /**
         * How far back to look when collecting signals/investments for a
         * location's activity snapshot. Investments in this domain
         * typically take many months from a first zoning signal to a
         * marketable listing, so this defaults to roughly a year rather
         * than the shorter windows used elsewhere in the codebase.
         */
        @field:Positive val activityPeriodDays: Long = 365,

        /** A location needs at least this many signals (or any investment) to get its own synthesis. */
        @field:Positive val minSignalsForSynthesis: Int = 2,

        /** Upper bound on how many per-location syntheses are (re)computed in a single scan. */
        @field:Positive val maxLocationsPerScan: Int = 20,

        /** How many top-ranked locations are included in the region-wide hotspot synthesis. */
        @field:Positive val hotspotTopN: Int = 10
    )
}
