package pl.marcinwieczorek.investmentmonitor.persistence

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.ActivityLevel
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTrend
import pl.marcinwieczorek.investmentmonitor.domain.HotspotEntry
import pl.marcinwieczorek.investmentmonitor.domain.HotspotSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.LocationSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.RecommendedAction
import pl.marcinwieczorek.investmentmonitor.testsupport.TestDatabase
import java.time.Instant

class JdbcLocationSynthesisRepositoryTest {

    private lateinit var db: TestDatabase
    private lateinit var repository: JdbcLocationSynthesisRepository

    @BeforeEach
    fun setUp() {
        db = TestDatabase.create()
        repository = JdbcLocationSynthesisRepository(db.jdbcTemplate)
    }

    @AfterEach
    fun tearDown() = db.close()

    private fun synthesis(location: String = "Kruszewnia") = LocationSynthesis(
        location = location,
        municipality = "Swarzędz",
        developmentTrend = DevelopmentTrend.ACCELERATING,
        summary = "Duza aktywnosc deweloperska.",
        estimatedTimeline = "6-12 miesiecy",
        keyDevelopers = listOf("Chronos", "Greenbud"),
        opportunities = listOf("Duze dzialki"),
        risks = listOf("Brak infrastruktury"),
        recommendedAction = RecommendedAction.WATCH_CLOSELY,
        reason = "Wiele sygnalow WZ.",
        signalCount = 4,
        investmentCount = 2,
        averageLeadTimeDays = 28.5,
        synthesizedAt = Instant.parse("2026-06-01T00:00:00Z")
    )

    @Test
    fun `upsert then findByLocation round-trips every field`() {
        repository.upsertLocation(synthesis())

        val found = repository.findByLocation("Kruszewnia")

        found shouldBe synthesis()
    }

    @Test
    fun `upserting the same location twice replaces the row instead of duplicating it`() {
        repository.upsertLocation(synthesis())
        repository.upsertLocation(synthesis().copy(summary = "Zaktualizowane podsumowanie.", signalCount = 5))

        repository.findAllLocations() shouldBe listOf(
            synthesis().copy(summary = "Zaktualizowane podsumowanie.", signalCount = 5)
        )
    }

    @Test
    fun `findByLocation returns null for an unknown location`() {
        repository.findByLocation("Nieznana") shouldBe null
    }

    @Test
    fun `saveHotspot replaces the previous global ranking rather than accumulating history`() {
        val first = HotspotSynthesis(
            hotspots = listOf(
                HotspotEntry("Kruszewnia", ActivityLevel.HIGH, DevelopmentTrend.ACCELERATING, "reason", ActivityLevel.HIGH)
            ),
            emergingAreas = listOf("Jasin"),
            summary = "Pierwsza synteza.",
            recommendation = "Obserwuj Kruszewnie.",
            synthesizedAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        val second = first.copy(summary = "Druga synteza.", synthesizedAt = Instant.parse("2026-02-01T00:00:00Z"))

        repository.saveHotspot(first)
        repository.saveHotspot(second)

        repository.findLatestHotspot() shouldBe second
        db.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hotspot_synthesis", Int::class.java) shouldBe 1
    }

    @Test
    fun `findLatestHotspot returns null when nothing has been saved yet`() {
        repository.findLatestHotspot() shouldBe null
    }
}
