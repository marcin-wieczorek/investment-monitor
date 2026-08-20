"use client";

import "leaflet/dist/leaflet.css";
import L from "leaflet";
import Link from "next/link";
import { MapContainer, Marker, Popup, TileLayer } from "react-leaflet";
import { useI18n } from "@/lib/i18n";
import { METRO_CENTER, METRO_DEFAULT_ZOOM } from "@/lib/location-coordinates";
import type { DevelopmentTrend, RecommendedAction } from "@/lib/badge-styles";

export interface MapInvestment {
  id: number;
  name: string;
  source_category: string | null;
}

/** Minimal location-intelligence context for a map marker (see `/locations`). */
export interface LocationActivitySummary {
  trend: DevelopmentTrend;
  recommendedAction: RecommendedAction;
  summary: string;
}

export interface LocationGroup {
  location: string;
  lat: number;
  lng: number;
  investments: MapInvestment[];
  activity?: LocationActivitySummary;
}

/** Same category-authority colors as SOURCE_CATEGORY_BADGE_CLASS in lib/badge-styles.ts (blue/purple/orange), as plain hex since Leaflet marker HTML is not JSX. */
const CATEGORY_COLOR: Record<string, string> = {
  DEVELOPER: "#3b82f6",
  DISCOVERY: "#a855f7",
  AGGREGATOR: "#f97316",
};
const DEFAULT_COLOR = "#6b7280";

/** Lower = more authoritative (see SourceCategory.kt: DEVELOPER > DISCOVERY > AGGREGATOR). */
const CATEGORY_PRIORITY: Record<string, number> = { DEVELOPER: 0, DISCOVERY: 1, AGGREGATOR: 2 };

function dominantColor(investments: MapInvestment[]): string {
  let best: string | null = null;
  let bestPriority = Number.POSITIVE_INFINITY;
  investments.forEach((investment) => {
    const category = investment.source_category;
    if (!category) return;
    const priority = CATEGORY_PRIORITY[category] ?? 3;
    if (priority < bestPriority) {
      bestPriority = priority;
      best = category;
    }
  });
  return best ? (CATEGORY_COLOR[best] ?? DEFAULT_COLOR) : DEFAULT_COLOR;
}

/** Same trend colors as TREND_BADGE_CLASS in lib/badge-styles.ts, as plain hex since Leaflet marker HTML is not JSX. */
const TREND_RING_COLOR: Record<DevelopmentTrend, string> = {
  ACCELERATING: "#10b981",
  STABLE: "#3b82f6",
  SLOWING: "#f59e0b",
  MINIMAL: "#6b7280",
};

function createMarkerIcon(color: string, count: number, ringColor?: string): L.DivIcon {
  const size = count > 9 ? 32 : count > 1 ? 28 : 22;
  const ring = ringColor ? `outline:3px solid ${ringColor};outline-offset:2px;` : "";
  return L.divIcon({
    className: "",
    html: `<div style="background-color:${color};width:${size}px;height:${size}px;border-radius:9999px;display:flex;align-items:center;justify-content:center;color:white;font-size:11px;font-weight:600;border:2px solid white;box-shadow:0 1px 3px rgba(0,0,0,0.4);${ring}">${
      count > 1 ? count : ""
    }</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

const MAX_LISTED_PER_POPUP = 6;

interface InvestmentMapProps {
  groups: LocationGroup[];
  /** When true, marker rings are colored by development trend (see `/locations`) and popups show a trend summary. */
  showActivity?: boolean;
}

/**
 * Pin-per-location overview map (not pin-per-investment): with ~50 known
 * locations in LocationCatalog and no per-investment geocoding, grouping by
 * location keeps the map readable without needing a marker-cluster plugin.
 * Dynamically imported with `ssr: false` by MapView - react-leaflet's
 * MapContainer touches `window`/`document` and cannot render on the server.
 */
export function InvestmentMap({ groups, showActivity = false }: InvestmentMapProps) {
  const { t, tEnum } = useI18n();

  return (
    <MapContainer center={METRO_CENTER} zoom={METRO_DEFAULT_ZOOM} scrollWheelZoom className="h-[600px] w-full rounded-xl">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {groups.map((group) => (
        <Marker
          key={group.location}
          position={[group.lat, group.lng]}
          icon={createMarkerIcon(
            dominantColor(group.investments),
            group.investments.length,
            showActivity && group.activity ? TREND_RING_COLOR[group.activity.trend] : undefined
          )}
        >
          <Popup>
            <div className="space-y-1">
              <p className="font-medium">{group.location}</p>
              <p className="text-xs text-muted-foreground">
                {t("map.investmentCount").replace("{count}", String(group.investments.length))}
              </p>
              <ul className="space-y-0.5">
                {group.investments.slice(0, MAX_LISTED_PER_POPUP).map((investment) => (
                  <li key={investment.id}>
                    <Link href={`/investments/${investment.id}`} className="text-xs text-blue-600 hover:underline">
                      {investment.name}
                    </Link>
                  </li>
                ))}
              </ul>
              {group.investments.length > MAX_LISTED_PER_POPUP ? (
                <p className="text-xs text-muted-foreground">
                  {t("map.andMore").replace("{count}", String(group.investments.length - MAX_LISTED_PER_POPUP))}
                </p>
              ) : null}
              {showActivity && group.activity ? (
                <div className="mt-2 space-y-1 border-t border-border pt-2">
                  <p className="text-xs">
                    <span className="font-medium">{tEnum("developmentTrend", group.activity.trend)}</span>
                    {" · "}
                    {tEnum("recommendedAction", group.activity.recommendedAction)}
                  </p>
                  <p className="text-xs text-muted-foreground">{group.activity.summary}</p>
                  <Link href="/locations" className="text-xs text-blue-600 hover:underline">
                    {t("locations.viewAll")}
                  </Link>
                </div>
              ) : null}
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
