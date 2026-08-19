package pl.marcin.investmentmonitor.domain

/**
 * Explicit registry of municipalities/villages in the Poznań metropolitan
 * area currently in scope for discovery and location scoring.
 *
 * This list is deliberately not exhaustive (see docs/ARCHITECTURE.md
 * geographic scope) - it should grow as new discovery sources or
 * developer investments reveal new relevant locations, rather than being
 * hard-coded into individual parsers.
 */
object LocationCatalog {

    /** Locations explicitly named in the target geographic scope. */
    val CORE_LOCATIONS: Set<String> = setOf(
        "Poznań", "Swarzędz", "Zalasewo", "Rabowice", "Kruszewnia", "Gowarzewo", "Garby",
        "Kleszczewo", "Tulce", "Borówiec", "Kamionki", "Komorniki", "Plewiska", "Dąbrówka",
        "Palędzie", "Dopiewo", "Skórzewo", "Luboń", "Mosina", "Rokietnica", "Suchy Las",
        "Tarnowo Podgórne", "Czerwonak", "Murowana Goślina", "Kostrzyn"
    )

    /**
     * Additional villages within Gmina Swarzędz observed directly in official
     * sources (BIP zoning-conditions register) - same metropolitan target
     * area as [CORE_LOCATIONS], added as they turned up in real evidence.
     */
    val SWARZEDZ_GMINA_VILLAGES: Set<String> = setOf(
        "Jasin", "Gruszczyn", "Gortatowo", "Paczkowo", "Bogucin", "Łowęcin", "Wierzenica",
        "Uzarzewo", "Janikowo", "Sarbinowo", "Gruszczynek", "Karłowice", "Kobylnica"
    )

    val ALL_LOCATIONS: Set<String> = CORE_LOCATIONS + SWARZEDZ_GMINA_VILLAGES

    /** Finds the first known location name mentioned as a whole word in [text], if any. */
    fun findIn(text: String): String? =
        ALL_LOCATIONS.firstOrNull { location ->
            Regex("\\b${Regex.escape(location)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }
}
