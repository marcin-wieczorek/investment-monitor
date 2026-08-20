package pl.marcinwieczorek.investmentmonitor.domain

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
        "Tarnowo Podgórne", "Czerwonak", "Murowana Goślina", "Kostrzyn",
        // Remaining Metropolia Poznań municipalities (AGENTS.md geographic coverage section).
        "Buk", "Oborniki", "Pobiedziska", "Puszczykowo", "Skoki", "Stęszew", "Szamotuły", "Śrem"
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

    /**
     * Villages within Gmina Śrem observed directly in its BIP
     * zoning-conditions (warunki zabudowy) obwieszczenia register.
     */
    val SREM_GMINA_VILLAGES: Set<String> = setOf("Kaleje", "Nochowo", "Wyrzeka")

    /**
     * Villages within Gmina Murowana Goślina observed directly in its BIP
     * obwieszczenia register (zoning-conditions and public-purpose siting
     * decisions).
     */
    val MUROWANA_GOSLINA_GMINA_VILLAGES: Set<String> = setOf(
        "Wojnowo", "Białężyn", "Głębocko", "Długa Goślina", "Łopuchowo"
    )

    /**
     * Villages within Gmina Buk observed directly in its BIP "Obwieszczenia
     * i komunikaty" register (zoning-conditions and public-purpose siting
     * decisions) - see `BukObwieszczeniaParser`.
     */
    val BUK_GMINA_VILLAGES: Set<String> = setOf(
        "Wielka Wieś", "Niepruszewo", "Dobieżyn", "Kalwy", "Otusz", "Szewce", "Cieśle"
    )

    /**
     * Villages within Gmina Szamotuły observed directly in its BIP
     * public-purpose siting ("ustalenie lokalizacji inwestycji celu
     * publicznego") register - see `SzamotulyUlicpParser`.
     */
    val SZAMOTULY_GMINA_VILLAGES: Set<String> = setOf("Lulinek", "Gąsawy", "Mutowo")

    /**
     * Villages within Gmina Pobiedziska observed directly in its BIP
     * "Komunikaty" (planning announcements) register - see
     * `PobiedziskaKomunikatyParser`.
     */
    val POBIEDZISKA_GMINA_VILLAGES: Set<String> = setOf("Główna", "Kowalskie")

    /**
     * Villages within Gmina Kórnik observed directly in its BIP
     * "Obwieszczenia i ogłoszenia" (Wydział Planowania Przestrzennego)
     * register - see `KornikObwieszczeniaParser`.
     */
    val KORNIK_GMINA_VILLAGES: Set<String> = setOf(
        "Biernatki", "Błażejewko", "Czmoniec", "Czmoń", "Czołowo", "Dachowa", "Gądki",
        "Konarskie", "Koninko", "Pierzchno", "Radzewo", "Robakowo", "Runowo", "Szczytniki",
        "Żerniki", "Bnin"
    )

    /**
     * Villages within Gmina Dopiewo observed directly in its BIP "Decyzje
     * o warunkach zabudowy" register - see `DopiewoWzParser`.
     */
    val DOPIEWO_GMINA_VILLAGES: Set<String> = setOf("Konarzewo")

    val ALL_LOCATIONS: Set<String> =
        CORE_LOCATIONS + SWARZEDZ_GMINA_VILLAGES + SREM_GMINA_VILLAGES + MUROWANA_GOSLINA_GMINA_VILLAGES +
            BUK_GMINA_VILLAGES + SZAMOTULY_GMINA_VILLAGES + POBIEDZISKA_GMINA_VILLAGES +
            KORNIK_GMINA_VILLAGES + DOPIEWO_GMINA_VILLAGES

    /**
     * Maps each gmina's outlying villages to their parent municipality name
     * (the same name used as `InvestmentSignal.municipality` by the
     * corresponding discovery source, and as the matching entry in
     * [CORE_LOCATIONS]). Used by location-intelligence aggregation
     * ([pl.marcinwieczorek.investmentmonitor.analysis.LocationActivityCollector])
     * to group a village-level `location` (e.g. "Jasin") together with
     * signals/investments recorded directly under its municipality (e.g.
     * "Swarzędz").
     */
    private val VILLAGE_TO_MUNICIPALITY: Map<String, String> = buildMap {
        SWARZEDZ_GMINA_VILLAGES.forEach { put(it, "Swarzędz") }
        SREM_GMINA_VILLAGES.forEach { put(it, "Śrem") }
        MUROWANA_GOSLINA_GMINA_VILLAGES.forEach { put(it, "Murowana Goślina") }
        BUK_GMINA_VILLAGES.forEach { put(it, "Buk") }
        SZAMOTULY_GMINA_VILLAGES.forEach { put(it, "Szamotuły") }
        POBIEDZISKA_GMINA_VILLAGES.forEach { put(it, "Pobiedziska") }
        KORNIK_GMINA_VILLAGES.forEach { put(it, "Kórnik") }
        DOPIEWO_GMINA_VILLAGES.forEach { put(it, "Dopiewo") }
    }

    /**
     * The parent municipality for [location]: itself if [location] is
     * already a top-level entry in [CORE_LOCATIONS], its gmina if it's one
     * of the villages mapped above, or `null` if [location] isn't in this
     * catalog at all.
     */
    fun parentMunicipality(location: String): String? =
        CORE_LOCATIONS.firstOrNull { it.equals(location, ignoreCase = true) }
            ?: VILLAGE_TO_MUNICIPALITY.entries.firstOrNull { it.key.equals(location, ignoreCase = true) }?.value

    /**
     * Finds the first known location name mentioned as a whole word in
     * [text], if any.
     *
     * Uses explicit Unicode letter/digit lookarounds rather than `\b`:
     * Java's `\b` defines "word" as ASCII `[a-zA-Z0-9_]` unless
     * `UNICODE_CHARACTER_CLASS` is set, so it silently fails to find a
     * boundary next to a Polish diacritic letter - e.g. `\bPoznań\b` does
     * not match "Poznań," because `ń` isn't an ASCII word character. Since
     * almost every location in this catalog starts or ends with one
     * (Poznań, Śrem, Łowęcin, Głębocko, ...), that bug would silently break
     * this function for most of its own catalog.
     */
    fun findIn(text: String): String? =
        ALL_LOCATIONS.firstOrNull { location ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(location)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        }
}
