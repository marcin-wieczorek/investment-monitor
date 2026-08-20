package pl.marcinwieczorek.investmentmonitor.persistence

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.DuplicateConfidence
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentDuplicate
import pl.marcinwieczorek.investmentmonitor.testsupport.TestDatabase
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import java.time.Instant

class JdbcInvestmentDuplicateRepositoryTest {

    private lateinit var db: TestDatabase
    private lateinit var repository: JdbcInvestmentDuplicateRepository
    private var idA: Long = -1
    private var idB: Long = -1

    @BeforeEach
    fun setUp() {
        db = TestDatabase.create()
        repository = JdbcInvestmentDuplicateRepository(db.jdbcTemplate)

        val investmentRepository = JdbcInvestmentRepository(db.jdbcTemplate)
        val a = testInvestment(name = "Tercja", source = "chronos")
        val b = testInvestment(name = "Osiedle Tercja", source = "rynekpierwotny", url = java.net.URI("https://rynekpierwotny.pl/osiedle-tercja"))
        investmentRepository.upsert(a, Instant.now())
        investmentRepository.upsert(b, Instant.now())
        idA = investmentRepository.findIdByCanonicalKey(a.canonicalKey)!!
        idB = investmentRepository.findIdByCanonicalKey(b.canonicalKey)!!
    }

    @AfterEach
    fun tearDown() = db.close()

    @Test
    fun `save then exists is true regardless of argument order`() {
        repository.save(
            InvestmentDuplicate(
                investmentIdA = idA,
                investmentIdB = idB,
                confidence = DuplicateConfidence.HIGH,
                matchedFeatures = listOf("developer", "nameOverlap:1.00"),
                reason = "same project",
                createdAt = Instant.now()
            )
        )

        repository.exists(idA, idB) shouldBe true
        repository.exists(idB, idA) shouldBe true
    }

    @Test
    fun `saving the same pair twice in reversed order does not create a duplicate row`() {
        repository.save(
            InvestmentDuplicate(investmentIdA = idA, investmentIdB = idB, confidence = DuplicateConfidence.HIGH, matchedFeatures = listOf("developer"), reason = "reason", createdAt = Instant.now())
        )
        repository.save(
            InvestmentDuplicate(investmentIdA = idB, investmentIdB = idA, confidence = DuplicateConfidence.HIGH, matchedFeatures = listOf("developer"), reason = "reason", createdAt = Instant.now())
        )

        db.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM investment_duplicate", Int::class.java) shouldBe 1
    }

    @Test
    fun `findByInvestment finds the pair from either side`() {
        repository.save(
            InvestmentDuplicate(investmentIdA = idA, investmentIdB = idB, confidence = DuplicateConfidence.HIGH, matchedFeatures = listOf("developer"), reason = "reason", createdAt = Instant.now())
        )

        repository.findByInvestment(idA) shouldBe repository.findByInvestment(idB)
    }
}
