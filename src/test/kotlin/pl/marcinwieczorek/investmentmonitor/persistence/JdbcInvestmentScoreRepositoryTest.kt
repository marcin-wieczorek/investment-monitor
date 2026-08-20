package pl.marcinwieczorek.investmentmonitor.persistence

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.analysis.ScoringResult
import pl.marcinwieczorek.investmentmonitor.testsupport.TestDatabase
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import java.time.Instant

class JdbcInvestmentScoreRepositoryTest {

    private lateinit var db: TestDatabase
    private lateinit var repository: JdbcInvestmentScoreRepository
    private lateinit var investmentRepository: JdbcInvestmentRepository

    private val scoring = ScoringResult(
        propertyTypeMatch = true,
        locationTierMatch = true,
        houseAreaScore = 0.8,
        plotAreaScore = 0.9,
        priceScore = 0.7,
        largePlotBonus = false,
        plotToHouseRatio = 3.0,
        overallScore = 0.8
    )

    @BeforeEach
    fun setUp() {
        db = TestDatabase.create()
        repository = JdbcInvestmentScoreRepository(db.jdbcTemplate)
        investmentRepository = JdbcInvestmentRepository(db.jdbcTemplate)
    }

    @AfterEach
    fun tearDown() = db.close()

    @Test
    fun `save then find returns the same scoring result`() {
        repository.save("chronos:https://example.com/aura", scoring, Instant.now())

        val found = repository.find("chronos:https://example.com/aura")
        found shouldBe scoring
    }

    @Test
    fun `resolves investment_id via canonical_key when the investment already exists`() {
        val investment = testInvestment(name = "Aura")
        investmentRepository.upsert(investment, Instant.now())

        repository.save(investment.canonicalKey, scoring, Instant.now())

        val investmentId = db.jdbcTemplate.queryForObject(
            "SELECT investment_id FROM investment_score WHERE investment_canonical_key = ?",
            Long::class.java,
            investment.canonicalKey
        )
        investmentId shouldNotBe null
    }

    @Test
    fun `leaves investment_id null when scored before the investment is persisted, self-heals on re-save`() {
        val investment = testInvestment(name = "Aura")

        // Scoring happens before commit (see MonitoringService.processIfNew) - no investment row exists yet.
        repository.save(investment.canonicalKey, scoring, Instant.now())
        val beforeCommit = db.jdbcTemplate.queryForObject(
            "SELECT investment_id FROM investment_score WHERE investment_canonical_key = ?",
            Long::class.java,
            investment.canonicalKey
        )
        beforeCommit shouldBe null

        investmentRepository.upsert(investment, Instant.now())
        repository.save(investment.canonicalKey, scoring, Instant.now())

        val afterCommit = db.jdbcTemplate.queryForObject(
            "SELECT investment_id FROM investment_score WHERE investment_canonical_key = ?",
            Long::class.java,
            investment.canonicalKey
        )
        afterCommit shouldNotBe null
    }
}
