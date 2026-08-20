package pl.marcin.investmentmonitor.correlation

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.DuplicateConfidence
import pl.marcin.investmentmonitor.testsupport.testInvestment
import java.net.URI

class InvestmentDeduplicatorTest {

    private val deduplicator = InvestmentDeduplicator()

    @Test
    fun `matches the same project published by its developer and listed on an aggregator as HIGH confidence`() {
        val developerListing = testInvestment(
            name = "Tercja", source = "chronos", developer = "Chronos Development", location = "Kruszewnia"
        )
        val aggregatorListing = testInvestment(
            name = "Osiedle Tercja", source = "rynekpierwotny", developer = "Chronos Development", location = "Kruszewnia",
            url = URI("https://rynekpierwotny.pl/osiedle-tercja")
        )

        val duplicates = deduplicator.findDuplicates(listOf(developerListing, aggregatorListing))

        duplicates shouldHaveSize 1
        duplicates.single().confidence shouldBe DuplicateConfidence.HIGH
    }

    @Test
    fun `matches same developer and location even with a legal-entity suffix difference as at least MEDIUM`() {
        val a = testInvestment(
            name = "Aura Etap 1", source = "chronos", developer = "Chronos Development Sp. z o.o.", location = "Kruszewnia",
            url = URI("https://chronos.poznan.pl/aura-etap-1")
        )
        val b = testInvestment(
            name = "Aura Etap 2", source = "rynekpierwotny", developer = "Chronos Development", location = "Kruszewnia",
            url = URI("https://rynekpierwotny.pl/aura-etap-2")
        )

        val duplicate = deduplicator.findDuplicates(listOf(a, b)).single()

        duplicate.confidence shouldBe DuplicateConfidence.MEDIUM
        duplicate.matchedFeatures.any { it.startsWith("developer:") } shouldBe true
    }

    @Test
    fun `does not compare two investments from the same source`() {
        val a = testInvestment(name = "Tercja", source = "chronos", location = "Kruszewnia")
        val b = testInvestment(name = "Tercja", source = "chronos", location = "Kruszewnia", url = URI("https://example.com/other"))

        deduplicator.findDuplicates(listOf(a, b)).shouldBeEmpty()
    }

    @Test
    fun `does not match investments in different locations`() {
        val a = testInvestment(name = "Tercja", source = "chronos", developer = "Chronos Development", location = "Kruszewnia")
        val b = testInvestment(
            name = "Tercja", source = "rynekpierwotny", developer = "Chronos Development", location = "Mosina",
            url = URI("https://rynekpierwotny.pl/tercja")
        )

        deduplicator.findDuplicates(listOf(a, b)).shouldBeEmpty()
    }

    @Test
    fun `does not match unrelated investments from different developers with dissimilar names`() {
        val a = testInvestment(name = "Aura", source = "chronos", developer = "Chronos Development", location = "Kruszewnia")
        val b = testInvestment(
            name = "Zielona Dolina", source = "rynekpierwotny", developer = "Unknown (RynekPierwotny)", location = "Kruszewnia",
            url = URI("https://rynekpierwotny.pl/zielona-dolina")
        )

        deduplicator.findDuplicates(listOf(a, b)).shouldBeEmpty()
    }

    @Test
    fun `does not treat an aggregator placeholder developer name as a real developer match`() {
        val a = testInvestment(
            name = "Zielona Dolina", source = "chronos", developer = "Unknown (RynekPierwotny)", location = "Kruszewnia",
            url = URI("https://chronos.poznan.pl/zielona-dolina")
        )
        val b = testInvestment(
            name = "Aura", source = "rynekpierwotny", developer = "Unknown (RynekPierwotny)", location = "Kruszewnia",
            url = URI("https://rynekpierwotny.pl/aura")
        )

        deduplicator.findDuplicates(listOf(a, b)).shouldBeEmpty()
    }

    @Test
    fun `ignores investments with no recognized location`() {
        val a = testInvestment(name = "Tercja", source = "chronos", developer = "Chronos Development", location = "Nieznana Wieś")
        val b = testInvestment(
            name = "Tercja", source = "rynekpierwotny", developer = "Chronos Development", location = "Nieznana Wieś",
            url = URI("https://rynekpierwotny.pl/tercja")
        )

        deduplicator.findDuplicates(listOf(a, b)).shouldBeEmpty()
    }

    @Test
    fun `matches strong name overlap alone as MEDIUM even without a developer match`() {
        val a = testInvestment(
            name = "Osiedle Kruszewnia Park", source = "chronos", developer = "Chronos Development", location = "Kruszewnia",
            url = URI("https://chronos.poznan.pl/kruszewnia-park")
        )
        val b = testInvestment(
            name = "Kruszewnia Park", source = "rynekpierwotny", developer = "Unknown (RynekPierwotny)", location = "Kruszewnia",
            url = URI("https://rynekpierwotny.pl/kruszewnia-park")
        )

        val duplicate = deduplicator.findDuplicates(listOf(a, b)).single()

        duplicate.confidence shouldBe DuplicateConfidence.MEDIUM
    }
}
