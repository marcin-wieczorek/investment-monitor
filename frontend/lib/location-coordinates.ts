/**
 * Approximate centroid coordinates (WGS84, decimal degrees) for every
 * location name known to the backend's `LocationCatalog`
 * (`src/main/kotlin/.../domain/LocationCatalog.kt`) - kept in sync
 * manually, same rationale as that file: a small, curated, reviewable
 * lookup rather than a live geocoding API call (this project is
 * local-first and deliberately has no external API dependencies for
 * anything that doesn't need one).
 *
 * These are good enough for a metro-area overview map (pin placement at
 * city/village level, zoom ~9-13) - they are NOT precise street-level
 * geocodes and should never be used for routing/distance calculations
 * that need meter-level accuracy.
 *
 * Keys must match `LocationCatalog` location names exactly (including
 * Polish diacritics) so `investment.location` -> coordinate lookups
 * work without a second normalization step.
 */
export const LOCATION_COORDINATES: Record<string, [lat: number, lng: number]> = {
  // Core Metropolia Poznań locations
  "Poznań": [52.4064, 16.9252],
  "Swarzędz": [52.4125, 17.0819],
  "Zalasewo": [52.385, 17.05],
  "Rabowice": [52.43, 17.1],
  "Kruszewnia": [52.455, 17.115],
  "Gowarzewo": [52.44, 17.17],
  "Garby": [52.42, 17.19],
  "Kleszczewo": [52.335, 17.21],
  "Tulce": [52.32, 17.15],
  "Borówiec": [52.285, 17.07],
  "Kamionki": [52.37, 17.05],
  "Komorniki": [52.335, 16.808],
  "Plewiska": [52.365, 16.81],
  "Dąbrówka": [52.32, 16.73],
  "Palędzie": [52.355, 16.72],
  "Dopiewo": [52.383, 16.683],
  "Skórzewo": [52.413, 16.767],
  "Luboń": [52.34, 16.878],
  "Mosina": [52.242, 16.846],
  "Rokietnica": [52.472, 16.79],
  "Suchy Las": [52.49, 16.856],
  "Tarnowo Podgórne": [52.403, 16.73],
  "Czerwonak": [52.49, 16.955],
  "Murowana Goślina": [52.568, 17.017],
  "Kostrzyn": [52.402, 17.227],
  "Buk": [52.356, 16.517],
  "Oborniki": [52.647, 16.813],
  "Pobiedziska": [52.493, 17.267],
  "Puszczykowo": [52.286, 16.846],
  "Skoki": [52.682, 17.183],
  "Stęszew": [52.306, 16.627],
  "Szamotuły": [52.611, 16.579],
  "Śrem": [52.087, 17.017],
  // Gmina Swarzędz villages
  "Jasin": [52.43, 17.12],
  "Gruszczyn": [52.46, 17.155],
  "Gortatowo": [52.475, 17.165],
  "Paczkowo": [52.395, 17.1],
  "Bogucin": [52.385, 17.035],
  "Łowęcin": [52.445, 17.09],
  "Wierzenica": [52.465, 17.035],
  "Uzarzewo": [52.455, 17.05],
  "Janikowo": [52.44, 17.045],
  "Sarbinowo": [52.47, 17.11],
  "Gruszczynek": [52.465, 17.145],
  "Karłowice": [52.395, 17.135],
  "Kobylnica": [52.42, 17.04],
  // Gmina Śrem villages
  "Kaleje": [52.11, 17.07],
  "Nochowo": [52.14, 17.045],
  "Wyrzeka": [52.07, 16.97],
  // Gmina Murowana Goślina villages
  "Wojnowo": [52.595, 17.07],
  "Białężyn": [52.55, 16.97],
  "Głębocko": [52.585, 17.09],
  "Długa Goślina": [52.625, 17.055],
  "Łopuchowo": [52.55, 17.085],
};

/** Center of the Poznań metropolitan area - the map's default view. */
export const METRO_CENTER: [lat: number, lng: number] = [52.4064, 16.9252];
export const METRO_DEFAULT_ZOOM = 10;

export function coordinatesFor(location: string | null): [number, number] | null {
  if (!location) return null;
  return LOCATION_COORDINATES[location] ?? null;
}
