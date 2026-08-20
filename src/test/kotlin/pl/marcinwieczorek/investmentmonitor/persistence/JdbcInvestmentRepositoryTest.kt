package pl.marcinwieczorek.investmentmonitor.persistence

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.testsupport.TestDatabase
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import java.time.Instant

/**
 * Persistence-layer smoke tests against a real, migrated SQLite database
 * (see [TestDatabase]) - the codebase previously had zero tests exercising
 * actual SQL (all repository behavior was only exercised indirectly via
 * in-memory fakes in [pl.marcinwieczorek.investmentmonitor.monitoring.MonitoringServiceTest]).
 */
class JdbcInvestmentRepositoryTest {

    private lateinit var db: TestDatabase
    private lateinit var repository: JdbcInvestmentRepository

    @BeforeEach
    fun setUp() {
        db = TestDatabase.create()
        repository = JdbcInvestmentRepository(db.jdbcTemplate)
    }

    @AfterEach
    fun tearDown() = db.close()

    @Test
    fun `upsert then find returns the same investment`() {
        val investment = testInvestment(name = "Aura", price = PriceRange(700_000, 750_000))
        repository.upsert(investment, Instant.parse("2026-01-01T00:00:00Z"))

        val found = repository.findAllBySource(investment.source)[investment.canonicalKey]
        found shouldNotBe null
        found?.name shouldBe "Aura"
        found?.price shouldBe PriceRange(700_000, 750_000)
        found?.source shouldBe investment.source
    }

    @Test
    fun `upsert twice updates fields via ON CONFLICT without creating a duplicate row`() {
        val first = testInvestment(name = "Aura", price = PriceRange(700_000, 750_000))
        repository.upsert(first, Instant.parse("2026-01-01T00:00:00Z"))

        val updated = first.copy(price = PriceRange(720_000, 760_000))
        repository.upsert(updated, Instant.parse("2026-01-02T00:00:00Z"))

        val all = repository.findAll()
        all.count { it.canonicalKey == first.canonicalKey } shouldBe 1
        all.single().price shouldBe PriceRange(720_000, 760_000)
    }

    @Test
    fun `findIdByCanonicalKey resolves an id only after the investment is persisted`() {
        val investment = testInvestment(name = "Aura")
        repository.findIdByCanonicalKey(investment.canonicalKey) shouldBe null

        repository.upsert(investment, Instant.now())
        repository.findIdByCanonicalKey(investment.canonicalKey) shouldNotBe null
    }

    @Test
    fun `findAllBySource only returns investments for that source`() {
        repository.upsert(testInvestment(name = "Aura", source = "chronos"), Instant.now())
        repository.upsert(testInvestment(name = "Osiedle", source = "greenbud"), Instant.now())

        repository.findAllBySource(SourceId("chronos")).values.map { it.name } shouldBe listOf("Aura")
    }

    @Test
    fun `updateAggregatorOnlyDiscoveryFlag persists the flag`() {
        val investment = testInvestment(name = "AggregatorOnly", source = "rynekpierwotny")
        repository.upsert(investment, Instant.now())

        repository.updateAggregatorOnlyDiscoveryFlag(investment.canonicalKey, true)

        val flag = db.jdbcTemplate.queryForObject(
            "SELECT aggregator_only_discovery FROM investment WHERE canonical_key = ?",
            Int::class.java,
            investment.canonicalKey
        )
        flag shouldBe 1
    }
}
