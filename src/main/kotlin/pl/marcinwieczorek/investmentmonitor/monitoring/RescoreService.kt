package pl.marcinwieczorek.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer
import pl.marcinwieczorek.investmentmonitor.analysis.LocationProfiles
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentScoreRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import java.time.Clock
import java.time.Instant

/**
 * Recomputes `investment_score` for every currently known investment
 * against the current [UserPreferencesRepository.effectiveScoringProfile],
 * without fetching any live source - used after a user changes their
 * scoring preferences in `/settings` so the change is reflected
 * immediately rather than waiting for the next `bootRun` scan (see
 * docs/PLAN-configurable-scoring.md).
 *
 * Deliberately separate from [MonitoringService]: this never touches
 * sources, validation, correlation or deduplication - it is a pure
 * "re-score with the same already-known facts" pass.
 */
@Service
class RescoreService(
    private val investmentRepository: InvestmentRepository,
    private val scorer: DeterministicScorer,
    private val investmentScoreRepository: InvestmentScoreRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun rescoreAll(): Int {
        val profile = userPreferencesRepository.effectiveScoringProfile()
        val investments = investmentRepository.findAll()
        val now = Instant.now(clock)

        investments.forEach { investment ->
            val locationProfile = locationProfileFor(investment)
            val scoring = scorer.score(investment, locationProfile, profile)
            investmentScoreRepository.save(investment.canonicalKey, scoring, now)
        }

        logger.info("Rescored {} investment(s) against profile '{}'", investments.size, profile.name)
        return investments.size
    }

    private fun locationProfileFor(investment: Investment): LocationProfile? =
        investment.location?.let(LocationCatalog::findIn)?.let(LocationProfiles::find)

    private companion object {
        val logger = LoggerFactory.getLogger(RescoreService::class.java)
    }
}
