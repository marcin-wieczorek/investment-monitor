"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { useI18n } from "@/lib/i18n";
import { Switch } from "@/components/ui/switch";
import { coordinatesFor } from "@/lib/location-coordinates";
import { SOURCE_CATEGORY_DOT_CLASS } from "@/lib/badge-styles";
import type { InvestmentWithState } from "@/lib/types";
import type { LocationGroup, MapInvestment } from "@/components/map/investment-map";

const InvestmentMap = dynamic(
  () => import("@/components/map/investment-map").then((mod) => mod.InvestmentMap),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-[600px] items-center justify-center rounded-xl border border-border bg-card text-sm text-muted-foreground">
        …
      </div>
    ),
  }
);

interface MapViewProps {
  investments: InvestmentWithState[];
}

const CATEGORIES = ["DEVELOPER", "DISCOVERY", "AGGREGATOR"] as const;
type Category = (typeof CATEGORIES)[number];

export function MapView({ investments }: MapViewProps) {
  const { t } = useI18n();
  const [visibleCategories, setVisibleCategories] = useState<Record<Category, boolean>>({
    DEVELOPER: true,
    DISCOVERY: true,
    AGGREGATOR: true,
  });

  const filtered = useMemo(
    () =>
      investments.filter((investment) => {
        if (investment.archived) return false;
        if (!investment.source_category) return true;
        return visibleCategories[investment.source_category as Category] ?? true;
      }),
    [investments, visibleCategories]
  );

  const groups = useMemo<LocationGroup[]>(() => {
    const byLocation = new Map<string, LocationGroup>();
    filtered.forEach((investment) => {
      const location = investment.location;
      if (!location) return;
      const coords = coordinatesFor(location);
      if (!coords) return;

      const entry: MapInvestment = {
        id: investment.id,
        name: investment.name,
        source_category: investment.source_category,
      };
      const existing = byLocation.get(location);
      if (existing) {
        existing.investments.push(entry);
      } else {
        byLocation.set(location, { location, lat: coords[0], lng: coords[1], investments: [entry] });
      }
    });
    return Array.from(byLocation.values());
  }, [filtered]);

  const mappedCount = groups.reduce((sum, group) => sum + group.investments.length, 0);
  const unmappedCount = filtered.length - mappedCount;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("map.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("map.subtitle")}</p>
      </div>

      <div className="flex flex-wrap items-center gap-4 rounded-xl border border-border bg-card p-3 text-sm">
        {CATEGORIES.map((category) => (
          <label key={category} className="flex items-center gap-2 text-muted-foreground">
            <Switch
              checked={visibleCategories[category]}
              onCheckedChange={(checked) =>
                setVisibleCategories((prev) => ({ ...prev, [category]: checked }))
              }
            />
            <span className="inline-flex items-center gap-1.5">
              <span className={`size-2.5 rounded-full ${SOURCE_CATEGORY_DOT_CLASS[category]}`} />
              {t(`sources.${category.toLowerCase()}` as "sources.developer")}
            </span>
          </label>
        ))}
        <span className="ml-auto text-xs text-muted-foreground">
          {t("map.locationsShown").replace("{count}", String(groups.length))}
        </span>
      </div>

      <InvestmentMap groups={groups} />

      {unmappedCount > 0 ? (
        <p className="text-xs text-muted-foreground">
          {t("map.unmappedNotice").replace("{count}", String(unmappedCount))}
        </p>
      ) : null}
    </div>
  );
}
