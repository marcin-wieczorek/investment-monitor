/**
 * Maps every location name known to the backend's `LocationCatalog`
 * (`src/main/kotlin/.../domain/LocationCatalog.kt`) to its parent gmina
 * (municipality) name, so the investments filter UI can present one
 * option per administrative unit instead of one per village - many
 * `CORE_LOCATIONS` entries and all `*_GMINA_VILLAGES` entries are
 * villages/neighborhoods within a single gmina and would otherwise
 * needlessly fragment the location dropdown.
 *
 * Standalone cities/gminas map to themselves (identity entry) so the
 * lookup is total for every known location and callers don't need a
 * separate "is this already gmina-level" check.
 *
 * Frontend-only, same rationale as `location-coordinates.ts`: a small,
 * curated, reviewable lookup rather than deriving this from a live
 * administrative-boundaries API.
 *
 * Keep in sync with `LocationCatalog.kt` if new locations are added
 * there.
 */
export const LOCATION_TO_GMINA: Record<string, string> = {
  // Gmina Swarzędz (core + BIP-observed villages)
  "Swarzędz": "Swarzędz",
  "Zalasewo": "Swarzędz",
  "Rabowice": "Swarzędz",
  "Kruszewnia": "Swarzędz",
  "Gowarzewo": "Swarzędz",
  "Garby": "Swarzędz",
  "Jasin": "Swarzędz",
  "Gruszczyn": "Swarzędz",
  "Gortatowo": "Swarzędz",
  "Paczkowo": "Swarzędz",
  "Bogucin": "Swarzędz",
  "Łowęcin": "Swarzędz",
  "Wierzenica": "Swarzędz",
  "Uzarzewo": "Swarzędz",
  "Janikowo": "Swarzędz",
  "Sarbinowo": "Swarzędz",
  "Gruszczynek": "Swarzędz",
  "Karłowice": "Swarzędz",
  "Kobylnica": "Swarzędz",

  // Gmina Kleszczewo
  "Kleszczewo": "Kleszczewo",
  "Tulce": "Kleszczewo",

  // Gmina Kórnik (core + BIP-observed villages)
  "Kamionki": "Kórnik",
  "Borówiec": "Kórnik",
  "Biernatki": "Kórnik",
  "Błażejewko": "Kórnik",
  "Czmoniec": "Kórnik",
  "Czmoń": "Kórnik",
  "Czołowo": "Kórnik",
  "Dachowa": "Kórnik",
  "Gądki": "Kórnik",
  "Konarskie": "Kórnik",
  "Koninko": "Kórnik",
  "Pierzchno": "Kórnik",
  "Radzewo": "Kórnik",
  "Robakowo": "Kórnik",
  "Runowo": "Kórnik",
  "Szczytniki": "Kórnik",
  "Żerniki": "Kórnik",
  "Bnin": "Kórnik",

  // Gmina Komorniki (core + BIP-observed villages)
  "Komorniki": "Komorniki",
  "Plewiska": "Komorniki",

  // Gmina Dopiewo (core + BIP-observed villages)
  "Dopiewo": "Dopiewo",
  "Skórzewo": "Dopiewo",
  "Dąbrówka": "Dopiewo",
  "Palędzie": "Dopiewo",
  "Konarzewo": "Dopiewo",

  // Gmina Śrem (core + BIP-observed villages)
  "Śrem": "Śrem",
  "Kaleje": "Śrem",
  "Nochowo": "Śrem",
  "Wyrzeka": "Śrem",

  // Gmina Murowana Goślina (core + BIP-observed villages)
  "Murowana Goślina": "Murowana Goślina",
  "Wojnowo": "Murowana Goślina",
  "Białężyn": "Murowana Goślina",
  "Głębocko": "Murowana Goślina",
  "Długa Goślina": "Murowana Goślina",
  "Łopuchowo": "Murowana Goślina",

  // Gmina Buk (core + BIP-observed villages)
  "Buk": "Buk",
  "Wielka Wieś": "Buk",
  "Niepruszewo": "Buk",
  "Dobieżyn": "Buk",
  "Kalwy": "Buk",
  "Otusz": "Buk",
  "Szewce": "Buk",
  "Cieśle": "Buk",

  // Gmina Szamotuły (core + BIP-observed villages)
  "Szamotuły": "Szamotuły",
  "Lulinek": "Szamotuły",
  "Gąsawy": "Szamotuły",
  "Mutowo": "Szamotuły",

  // Gmina Pobiedziska (core + BIP-observed villages)
  "Pobiedziska": "Pobiedziska",
  "Główna": "Pobiedziska",
  "Kowalskie": "Pobiedziska",

  // Standalone cities/gminas - no known sub-villages in the catalog
  "Poznań": "Poznań",
  "Luboń": "Luboń",
  "Mosina": "Mosina",
  "Rokietnica": "Rokietnica",
  "Suchy Las": "Suchy Las",
  "Tarnowo Podgórne": "Tarnowo Podgórne",
  "Czerwonak": "Czerwonak",
  "Kostrzyn": "Kostrzyn",
  "Oborniki": "Oborniki",
  "Puszczykowo": "Puszczykowo",
  "Skoki": "Skoki",
  "Stęszew": "Stęszew",
};

/**
 * Normalizes a location name to its gmina-level (municipality) name for
 * filtering/grouping purposes.
 *
 * Real `investment.location` values are frequently richer than a bare
 * catalog name - parsers store whatever the source page published, e.g.
 * "Swarzędz – Jasin", "Poznań, ul. Bielicowa", "Komorniki ul. Młyńska",
 * "UL. SZKOLNA, POZNAŃ" - so an exact-match lookup alone would leave
 * almost every real investment unnormalized. Falls back to a
 * case-insensitive substring search against every known catalog name
 * (longest first, so e.g. "Suchy Las" wins over a shorter unrelated
 * match) - same rationale as the backend's `LocationCatalog.findIn`.
 *
 * Locations matching nothing in the catalog (out-of-scope cities from
 * the aggregator source, e.g. "Wrocław", "Bydgoszcz") pass through
 * unchanged rather than being dropped.
 */
const SORTED_LOCATION_ENTRIES: [name: string, gmina: string][] = Object.entries(LOCATION_TO_GMINA).sort(
  ([a], [b]) => b.length - a.length
);

export function normalizeToGmina(location: string | null): string | null {
  if (!location) return null;

  const exact = LOCATION_TO_GMINA[location];
  if (exact) return exact;

  const upper = location.toUpperCase();
  for (const [name, gmina] of SORTED_LOCATION_ENTRIES) {
    if (upper.includes(name.toUpperCase())) return gmina;
  }

  return location;
}
