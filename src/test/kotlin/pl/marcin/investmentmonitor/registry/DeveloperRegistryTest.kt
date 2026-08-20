package pl.marcin.investmentmonitor.registry

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.DeveloperStatus
import pl.marcin.investmentmonitor.domain.DeveloperTier

class DeveloperRegistryTest {

    @Test
    fun `contains all tier A and tier B developers from AGENTS md`() {
        val tierA = DeveloperRegistry.ALL.count { it.tier == DeveloperTier.A }
        val tierB = DeveloperRegistry.ALL.count { it.tier == DeveloperTier.B }
        tierA shouldBe 20
        tierB shouldBe 20
    }

    @Test
    fun `developer ids are unique`() {
        val ids = DeveloperRegistry.ALL.map { it.id }
        ids.toSet() shouldHaveSize ids.size
    }

    @Test
    fun `monitored developers reference a working adapter source id`() {
        DeveloperRegistry.ALL
            .filter { it.status == DeveloperStatus.MONITORED }
            .forEach { developer -> developer.adapterSourceId shouldNotBe null }
    }

    @Test
    fun `blocked developers have no adapter source id`() {
        DeveloperRegistry.ALL
            .filter { it.status == DeveloperStatus.BLOCKED }
            .forEach { developer -> developer.adapterSourceId shouldBe null }
    }

    @Test
    fun `finds chronos by name case-insensitively`() {
        DeveloperRegistry.findByName("chronos development")?.id shouldBe "chronos"
    }

    @Test
    fun `finds a developer by name despite an added legal-entity suffix`() {
        DeveloperRegistry.findByName("Chronos Development Sp. z o.o.")?.id shouldBe "chronos"
    }

    @Test
    fun `unknown developer name returns null`() {
        DeveloperRegistry.findByName("Totally Unknown Developer Xyz") shouldBe null
    }

    @Test
    fun `find by id returns the registered developer`() {
        DeveloperRegistry.find("greenbud")?.name shouldBe "Greenbud Development"
    }
}
