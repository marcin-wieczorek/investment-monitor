package pl.marcin.investmentmonitor.registry

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MunicipalityRegistryTest {

    @Test
    fun `contains all target Metropolia Poznan municipalities from AGENTS md`() {
        // Poznań + the 21 named gminas listed in AGENTS.md section 1 ("Powiat Poznański" is a
        // heading in that list, not itself a municipality).
        MunicipalityRegistry.ALL shouldHaveSize 22
    }

    @Test
    fun `municipality ids are unique`() {
        val ids = MunicipalityRegistry.ALL.map { it.id }
        ids.toSet() shouldHaveSize ids.size
    }

    @Test
    fun `swarzedz has full source coverage`() {
        val swarzedz = MunicipalityRegistry.find("swarzedz")
        swarzedz?.developerCoverage shouldBe pl.marcin.investmentmonitor.domain.MunicipalitySourceStatus.IMPLEMENTED
        swarzedz?.discoveryCoverage shouldBe pl.marcin.investmentmonitor.domain.MunicipalitySourceStatus.IMPLEMENTED
    }

    @Test
    fun `unknown municipality id returns null`() {
        MunicipalityRegistry.find("nonexistent") shouldBe null
    }
}

class DiscoverySourceRegistryTest {

    @Test
    fun `every implemented entry has an adapter source id`() {
        DiscoverySourceRegistry.ALL
            .filter { it.status == DiscoverySourceStatus.IMPLEMENTED }
            .forEach { entry -> entry.adapterSourceId shouldBe entry.adapterSourceId }
    }

    @Test
    fun `every blocked entry has a documented reason`() {
        DiscoverySourceRegistry.ALL
            .filter { it.status == DiscoverySourceStatus.BLOCKED }
            .forEach { entry -> entry.blockedReason?.isNotBlank() shouldBe true }
    }

    @Test
    fun `swarzedz entry references the implemented adapter`() {
        DiscoverySourceRegistry.find("swarzedz")?.adapterSourceId shouldBe "swarzedz-wz"
    }
}
