import type { HotspotEntry, HotspotSynthesisRow } from "@/lib/types";

/**
 * Parses a JSON-array column (`key_developers`/`opportunities`/`risks`/
 * `emerging_areas`) - node:sqlite returns these as raw TEXT. Lives in this
 * client-safe file (not lib/queries.ts, which pulls in node:sqlite and
 * must never be imported from a "use client" component - see
 * lib/location-coordinates.ts for the same rationale) so
 * locations-view.tsx/hotspot-card.tsx can call it directly. Defensive
 * against malformed/empty JSON (never thrown by a page render).
 */
export function parseJsonArray(value: string): string[] {
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === "string") : [];
  } catch {
    return [];
  }
}

/** Parses the `hotspots` JSON-array column of {@link HotspotSynthesisRow} into typed entries. */
export function parseHotspotEntries(hotspot: HotspotSynthesisRow): HotspotEntry[] {
  try {
    const parsed = JSON.parse(hotspot.hotspots);
    return Array.isArray(parsed) ? (parsed as HotspotEntry[]) : [];
  } catch {
    return [];
  }
}
