package pl.marcinwieczorek.investmentmonitor.analysis

import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTier
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile

/**
 * Explicit, reviewable location-development-potential data for the Poznań
 * metropolitan area (see docs/ARCHITECTURE.md location potential section).
 *
 * These are domain judgements, not derived from any source or LLM - an
 * analyzer may interpret them but must never silently overwrite them.
 * Scores are all 0..10 (see [LocationProfile]).
 */
object LocationProfiles {

    val ALL: List<LocationProfile> = listOf(
        LocationProfile("Poznań", DevelopmentTier.A, growthScore = 6, infrastructureScore = 10, transportScore = 10, familyScore = 7),
        LocationProfile("Swarzędz", DevelopmentTier.S, growthScore = 9, infrastructureScore = 9, transportScore = 8, familyScore = 9),
        LocationProfile("Zalasewo", DevelopmentTier.S, growthScore = 9, infrastructureScore = 8, transportScore = 8, familyScore = 9),
        LocationProfile("Rabowice", DevelopmentTier.S, growthScore = 9, infrastructureScore = 8, transportScore = 8, familyScore = 9),
        LocationProfile("Kruszewnia", DevelopmentTier.A, growthScore = 8, infrastructureScore = 6, transportScore = 6, familyScore = 8),
        LocationProfile("Gowarzewo", DevelopmentTier.A, growthScore = 7, infrastructureScore = 6, transportScore = 6, familyScore = 8),
        LocationProfile("Garby", DevelopmentTier.A, growthScore = 7, infrastructureScore = 5, transportScore = 6, familyScore = 8),
        LocationProfile("Kleszczewo", DevelopmentTier.A, growthScore = 7, infrastructureScore = 6, transportScore = 6, familyScore = 7),
        LocationProfile("Tulce", DevelopmentTier.A, growthScore = 7, infrastructureScore = 6, transportScore = 6, familyScore = 8),
        LocationProfile("Borówiec", DevelopmentTier.A, growthScore = 7, infrastructureScore = 6, transportScore = 6, familyScore = 8),
        LocationProfile("Kamionki", DevelopmentTier.A, growthScore = 7, infrastructureScore = 6, transportScore = 6, familyScore = 7),
        LocationProfile("Komorniki", DevelopmentTier.S, growthScore = 8, infrastructureScore = 8, transportScore = 9, familyScore = 8),
        LocationProfile("Plewiska", DevelopmentTier.A, growthScore = 7, infrastructureScore = 8, transportScore = 9, familyScore = 7),
        LocationProfile("Dąbrówka", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 6, familyScore = 7),
        LocationProfile("Palędzie", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 6, familyScore = 7),
        LocationProfile("Dopiewo", DevelopmentTier.A, growthScore = 7, infrastructureScore = 7, transportScore = 7, familyScore = 8),
        LocationProfile("Skórzewo", DevelopmentTier.A, growthScore = 7, infrastructureScore = 8, transportScore = 8, familyScore = 7),
        LocationProfile("Luboń", DevelopmentTier.A, growthScore = 6, infrastructureScore = 8, transportScore = 9, familyScore = 7),
        LocationProfile("Mosina", DevelopmentTier.B, growthScore = 6, infrastructureScore = 6, transportScore = 6, familyScore = 8),
        LocationProfile("Rokietnica", DevelopmentTier.A, growthScore = 8, infrastructureScore = 7, transportScore = 7, familyScore = 8),
        LocationProfile("Suchy Las", DevelopmentTier.S, growthScore = 8, infrastructureScore = 9, transportScore = 8, familyScore = 9),
        LocationProfile("Tarnowo Podgórne", DevelopmentTier.A, growthScore = 8, infrastructureScore = 8, transportScore = 7, familyScore = 8),
        LocationProfile("Czerwonak", DevelopmentTier.B, growthScore = 6, infrastructureScore = 6, transportScore = 7, familyScore = 7),
        LocationProfile("Murowana Goślina", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 5, familyScore = 7),
        LocationProfile("Kostrzyn", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 5, familyScore = 7),
        // Additional Gmina Swarzędz villages observed via discovery signals.
        LocationProfile("Jasin", DevelopmentTier.A, growthScore = 8, infrastructureScore = 5, transportScore = 6, familyScore = 8),
        LocationProfile("Gruszczyn", DevelopmentTier.A, growthScore = 7, infrastructureScore = 5, transportScore = 6, familyScore = 8),
        LocationProfile("Gortatowo", DevelopmentTier.B, growthScore = 6, infrastructureScore = 4, transportScore = 5, familyScore = 7),
        LocationProfile("Paczkowo", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 6, familyScore = 7),
        LocationProfile("Kobylnica", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 6, familyScore = 7),
        // Remaining Metropolia Poznań municipalities (AGENTS.md geographic coverage section) -
        // further out, lower growth/infrastructure scores until source coverage says otherwise.
        LocationProfile("Buk", DevelopmentTier.B, growthScore = 5, infrastructureScore = 5, transportScore = 5, familyScore = 7),
        LocationProfile("Oborniki", DevelopmentTier.B, growthScore = 5, infrastructureScore = 5, transportScore = 5, familyScore = 6),
        LocationProfile("Pobiedziska", DevelopmentTier.B, growthScore = 6, infrastructureScore = 5, transportScore = 6, familyScore = 7),
        LocationProfile("Puszczykowo", DevelopmentTier.B, growthScore = 5, infrastructureScore = 6, transportScore = 6, familyScore = 8),
        LocationProfile("Skoki", DevelopmentTier.B, growthScore = 4, infrastructureScore = 4, transportScore = 4, familyScore = 6),
        LocationProfile("Stęszew", DevelopmentTier.B, growthScore = 5, infrastructureScore = 5, transportScore = 5, familyScore = 7),
        LocationProfile("Szamotuły", DevelopmentTier.B, growthScore = 5, infrastructureScore = 5, transportScore = 5, familyScore = 6),
        LocationProfile("Śrem", DevelopmentTier.B, growthScore = 5, infrastructureScore = 5, transportScore = 5, familyScore = 6)
    )

    private val byName: Map<String, LocationProfile> = ALL.associateBy { it.name.lowercase() }

    fun find(name: String): LocationProfile? = byName[name.lowercase()]
}
