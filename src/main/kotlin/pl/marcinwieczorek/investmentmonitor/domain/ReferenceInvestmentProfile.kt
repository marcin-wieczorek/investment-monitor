package pl.marcinwieczorek.investmentmonitor.domain

/**
 * Generalizes the previously hard-coded "similarity to Tercja" concept: a
 * reference set of characteristics an investment is compared against.
 *
 * A profile may be derived from a specific manually-selected investment
 * (like Tercja) or defined directly. Deterministic feature comparison
 * against a profile is performed by the scoring layer
 * (`analysis.DeterministicScorer`); an LLM may add qualitative
 * interpretation on top but must never replace the numeric comparison
 * (see docs/ARCHITECTURE.md reference-profile section).
 */
data class ReferenceInvestmentProfile(
    val name: String,
    val preferredPropertyTypes: Set<PropertyType>,
    val preferredLocationTiers: Set<DevelopmentTier>,
    val houseAreaRange: AreaRange?,
    val plotAreaRange: AreaRange?,
    val priceRange: PriceRange?,
    /** Large plots are a positive feature, never an automatic rejection (see docs/ARCHITECTURE.md). */
    val largePlotPreferred: Boolean,
    val maxDistanceFromPoznanKm: Int?
)
