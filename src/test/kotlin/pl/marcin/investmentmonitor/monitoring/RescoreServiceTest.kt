package pl.marcin.investmentmonitor.monitoring

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.analysis.DeterministicScorer
import pl.marcin.investmentmonitor.analysis.ScoringResult
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.DevelopmentTier
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.PriceRange
import pl.marcin.investmentmonitor.domain.PropertyType
import pl.marcin.investmentmonitor.domain.ReferenceInvestmentProfile
import pl.marcin.investmentmonitor.persistence.InvestmentRepository
import pl.marcin.investmentmonitor.persistence.UserPreferencesRepository
import pl.marcin.investmentmonitor.testsupport.testInvestment
import java.time.Instant

private class RescoreTestInvestmentRepository(private val investments: List<Investment>) : InvestmentRepository {
    override fun findAllBySource(source: String): Map<String, Investment> =
        investments.filter { it.source == source }.associateBy { it.canonicalKey }
    override fun findAll(): List<Investment> = investments
    override fun upsert(investment: Investment, seenAt: Instant) {}
    override fun findIdByCanonicalKey(canonicalKey: String): Long? = null
    override fun updateAggregatorOnlyDiscoveryFlag(canonicalKey: String, isAggregatorOnly: Boolean) {}
}

private class RescoreTestInvestmentScoreRepository : pl.marcin.investmentmonitor.persistence.InvestmentScoreRepository {
    val saved = mutableMapOf<String, ScoringResult>()
    override fun save(investmentCanonicalKey: String, scoring: ScoringResult, scoredAt: Instant) {
        saved[investmentCanonicalKey] = scoring
    }
    override fun find(investmentCanonicalKey: String): ScoringResult? = saved[investmentCanonicalKey]
}

private class FixedUserPreferencesRepository(
    private val profile: ReferenceInvestmentProfile
) : UserPreferencesRepository {
    override fun findScoringProfile(): ReferenceInvestmentProfile = profile
    override fun saveScoringProfile(profile: ReferenceInvestmentProfile) {}
}

class RescoreServiceTest {

    @Test
    fun `recomputes scores for every known investment against the current profile`() {
        val investments = listOf(
            testInvestment(
                name = "Aura",
                propertyType = PropertyType.TERRACED,
                houseArea = AreaRange(120.0, 120.0),
                plotArea = AreaRange(500.0, 500.0),
                price = PriceRange(800_000, 800_000)
            ),
            testInvestment(name = "OsiedleB", propertyType = PropertyType.APARTMENT)
        )
        val investmentScoreRepository = RescoreTestInvestmentScoreRepository()
        val profile = ReferenceInvestmentProfile(
            name = "custom",
            preferredPropertyTypes = setOf(PropertyType.TERRACED),
            preferredLocationTiers = setOf(DevelopmentTier.S),
            houseAreaRange = AreaRange(80.0, 160.0),
            plotAreaRange = AreaRange(250.0, 1000.0),
            priceRange = PriceRange(600_000, 1_500_000),
            largePlotPreferred = true,
            maxDistanceFromPoznanKm = 25
        )

        val service = RescoreService(
            investmentRepository = RescoreTestInvestmentRepository(investments),
            scorer = DeterministicScorer(),
            investmentScoreRepository = investmentScoreRepository,
            userPreferencesRepository = FixedUserPreferencesRepository(profile)
        )

        val count = service.rescoreAll()

        count shouldBe 2
        investmentScoreRepository.saved.keys shouldBe investments.map { it.canonicalKey }.toSet()
        investmentScoreRepository.saved[investments[0].canonicalKey]?.propertyTypeMatch shouldBe true
        investmentScoreRepository.saved[investments[1].canonicalKey]?.propertyTypeMatch shouldBe false
    }

    @Test
    fun `falls back to the default profile when nothing has been configured`() {
        val investments = listOf(testInvestment(name = "Aura", propertyType = PropertyType.TERRACED))
        val fallbackRepository = object : UserPreferencesRepository {
            override fun findScoringProfile(): ReferenceInvestmentProfile? = null
            override fun saveScoringProfile(profile: ReferenceInvestmentProfile) {}
        }
        val investmentScoreRepository = RescoreTestInvestmentScoreRepository()

        val service = RescoreService(
            investmentRepository = RescoreTestInvestmentRepository(investments),
            scorer = DeterministicScorer(),
            investmentScoreRepository = investmentScoreRepository,
            userPreferencesRepository = fallbackRepository
        )

        service.rescoreAll() shouldBe 1
        investmentScoreRepository.saved[investments[0].canonicalKey]?.propertyTypeMatch shouldBe true
    }
}
