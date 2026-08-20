package pl.marcin.investmentmonitor.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URI

class DeveloperTest {

    @Test
    fun `monitored developer must reference an adapter source id`() {
        shouldThrow<IllegalArgumentException> {
            Developer(
                id = "test",
                name = "Test Developer",
                website = URI("https://example.com"),
                investmentListUrls = emptyList(),
                tier = DeveloperTier.A,
                status = DeveloperStatus.MONITORED,
                geographicScope = setOf("Poznań"),
                adapterSourceId = null
            )
        }
    }

    @Test
    fun `candidate developer may have a null adapter source id`() {
        val developer = Developer(
            id = "candidate-dev",
            name = "Candidate Dev",
            website = null,
            investmentListUrls = emptyList(),
            tier = DeveloperTier.B,
            status = DeveloperStatus.CANDIDATE,
            geographicScope = emptySet(),
            adapterSourceId = null
        )
        developer.adapterSourceId shouldBe null
    }

    @Test
    fun `geographic scope is preserved`() {
        val developer = Developer(
            id = "chronos",
            name = "Chronos Development",
            website = URI("https://www.chronos.poznan.pl"),
            investmentListUrls = listOf(URI("https://www.chronos.poznan.pl/inwestycje")),
            tier = DeveloperTier.A,
            status = DeveloperStatus.MONITORED,
            geographicScope = setOf("Komorniki", "Swarzędz"),
            adapterSourceId = "chronos"
        )
        developer.geographicScope shouldContain "Swarzędz"
    }
}
