package pl.marcinwieczorek.investmentmonitor.analysis

import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile

/**
 * Default [ReferenceInvestmentProfile]s, generalizing what used to be a
 * hard-coded "similarity to Tercja" concept (see
 * docs/ARCHITECTURE.md reference-profile section).
 *
 * [POZNAN_HOUSE_SEEKER] captures the characteristics of Tercja (Chronos,
 * Rabowice: terraced/semi-detached houses, large plots, strong-growth
 * suburb) as an explicit, editable configuration rather than an
 * investment-specific comparison.
 */
object ReferenceProfiles {

    val POZNAN_HOUSE_SEEKER = ReferenceInvestmentProfile(
        name = "poznan-house-seeker",
        preferredPropertyTypes = setOf(PropertyType.TERRACED, PropertyType.SEMI_DETACHED, PropertyType.DETACHED),
        preferredLocationTiers = setOf(DevelopmentTier.S, DevelopmentTier.A),
        houseAreaRange = AreaRange(80.0, 160.0),
        plotAreaRange = AreaRange(250.0, 1000.0),
        priceRange = PriceRange(600_000, 1_500_000),
        largePlotPreferred = true,
        maxDistanceFromPoznanKm = 25
    )

    val DEFAULT: ReferenceInvestmentProfile = POZNAN_HOUSE_SEEKER
}
