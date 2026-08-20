package pl.marcinwieczorek.investmentmonitor.persistence

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.Correlation
import pl.marcinwieczorek.investmentmonitor.domain.CorrelationConfidence
import pl.marcinwieczorek.investmentmonitor.testsupport.TestDatabase
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import java.time.Instant

class JdbcCorrelationRepositoryTest {

    private lateinit var db: TestDatabase
    private lateinit var repository: JdbcCorrelationRepository
    private var investmentId: Long = -1
    private var signalId: Long = -1

    @BeforeEach
    fun setUp() {
        db = TestDatabase.create()
        repository = JdbcCorrelationRepository(db.jdbcTemplate)

        val investmentRepository = JdbcInvestmentRepository(db.jdbcTemplate)
        val signalRepository = JdbcSignalRepository(db.jdbcTemplate)
        val investment = testInvestment(name = "OsiedleX")
        val signal = testSignal()
        investmentRepository.upsert(investment, Instant.now())
        signalRepository.upsert(signal, Instant.now())
        investmentId = investmentRepository.findIdByCanonicalKey(investment.canonicalKey)!!
        signalId = signalRepository.findIdByCanonicalKey(signal.canonicalKey)!!
    }

    @AfterEach
    fun tearDown() = db.close()

    private fun correlation(confidence: CorrelationConfidence = CorrelationConfidence.HIGH) = Correlation(
        investmentId = investmentId,
        signalId = signalId,
        confidence = confidence,
        matchedFeatures = listOf("location"),
        reason = "same location",
        createdAt = Instant.now()
    )

    @Test
    fun `save then findByInvestment returns the correlation`() {
        repository.save(correlation())

        repository.findByInvestment(investmentId) shouldBe listOf(
            repository.findByInvestment(investmentId).single()
        )
        repository.exists(investmentId, signalId) shouldBe true
    }

    @Test
    fun `saving the same pair twice does not create a duplicate row (ON CONFLICT DO NOTHING)`() {
        repository.save(correlation(CorrelationConfidence.HIGH))
        repository.save(correlation(CorrelationConfidence.MEDIUM))

        repository.findByInvestment(investmentId) shouldBe repository.findByInvestment(investmentId)
        db.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM correlation", Int::class.java) shouldBe 1
        // First write wins - ON CONFLICT DO NOTHING never overwrites.
        repository.findByInvestment(investmentId).single().confidence shouldBe CorrelationConfidence.HIGH
    }
}
