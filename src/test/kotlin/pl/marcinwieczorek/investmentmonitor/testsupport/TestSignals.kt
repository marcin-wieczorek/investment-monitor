package pl.marcinwieczorek.investmentmonitor.testsupport

import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.net.URI
import java.time.Instant

/** Builds an [InvestmentSignal] for tests with sensible defaults, mirroring [testInvestment]. */
fun testSignal(
    source: String = "swarzedz-wz",
    municipality: String = "Swarzędz",
    location: String? = "Kruszewnia",
    signalType: SignalType = SignalType.WZ_DECISION,
    title: String = "Test signal",
    reference: String? = "WAU.0000.0.2026",
    detectedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    url: URI = URI("https://example.com/test-signal"),
    rawFacts: Map<String, String> = emptyMap()
): InvestmentSignal = InvestmentSignal(
    source = SourceId(source),
    municipality = municipality,
    location = location,
    signalType = signalType,
    title = title,
    reference = reference,
    detectedAt = detectedAt,
    url = url,
    rawFacts = rawFacts
)
