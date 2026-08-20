package pl.marcinwieczorek.investmentmonitor.persistence

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.testsupport.TestDatabase
import pl.marcinwieczorek.investmentmonitor.testsupport.testSignal
import java.time.Instant

class JdbcSignalRepositoryTest {

    private lateinit var db: TestDatabase
    private lateinit var repository: JdbcSignalRepository

    @BeforeEach
    fun setUp() {
        db = TestDatabase.create()
        repository = JdbcSignalRepository(db.jdbcTemplate)
    }

    @AfterEach
    fun tearDown() = db.close()

    @Test
    fun `upsert then find returns the same signal, including raw facts`() {
        val signal = testSignal(rawFacts = mapOf("area" to "150 m2"))
        repository.upsert(signal, Instant.now())

        val found = repository.findAllBySource(signal.source)[signal.canonicalKey]
        found shouldNotBe null
        found?.title shouldBe signal.title
        found?.rawFacts shouldBe mapOf("area" to "150 m2")
    }

    @Test
    fun `upsert twice updates via ON CONFLICT without creating a duplicate row`() {
        val signal = testSignal(reference = "WAU.0000.0.2026")
        repository.upsert(signal, Instant.parse("2026-01-01T00:00:00Z"))
        repository.upsert(signal.copy(reference = "WAU.0000.1.2026"), Instant.parse("2026-01-02T00:00:00Z"))

        val all = repository.findAll()
        all.count { it.canonicalKey == signal.canonicalKey } shouldBe 1
        all.single().reference shouldBe "WAU.0000.1.2026"
    }

    @Test
    fun `findAllBySource only returns signals for that source`() {
        repository.upsert(testSignal(source = "swarzedz-wz"), Instant.now())
        repository.upsert(testSignal(source = "buk-obwieszczenia", municipality = "Buk"), Instant.now())

        repository.findAllBySource(SourceId("swarzedz-wz")).size shouldBe 1
    }
}
