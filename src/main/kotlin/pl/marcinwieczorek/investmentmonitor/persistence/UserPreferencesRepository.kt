package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.analysis.ReferenceProfiles
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile

/**
 * User-configurable preferences, currently limited to a single scoring
 * reference profile (see docs/PLAN-configurable-scoring.md). Backed by a
 * generic key-value table (`user_preferences`) so future preferences don't
 * need a new migration each time, but for now there is exactly one key.
 */
interface UserPreferencesRepository {
    /** `null` if no profile has ever been saved (fresh install). */
    fun findScoringProfile(): ReferenceInvestmentProfile?
    fun saveScoringProfile(profile: ReferenceInvestmentProfile)

    /**
     * The profile scoring should actually use right now: the stored one if
     * present, otherwise [ReferenceProfiles.DEFAULT] - callers (scoring,
     * rescoring) should always use this rather than [findScoringProfile]
     * directly, so "nothing configured yet" never means "no scoring
     * happens" (same rationale as [pl.marcinwieczorek.investmentmonitor.analysis.DefaultInvestmentAnalyzer]).
     */
    fun effectiveScoringProfile(): ReferenceInvestmentProfile = findScoringProfile() ?: ReferenceProfiles.DEFAULT
}
