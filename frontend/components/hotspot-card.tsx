"use client";

import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { parseHotspotEntries } from "@/lib/hotspot-utils";
import { ACTIVITY_LEVEL_BADGE_CLASS, TREND_BADGE_CLASS } from "@/lib/badge-styles";
import type { HotspotSynthesisRow } from "@/lib/types";

interface HotspotCardProps {
  hotspotSynthesis: HotspotSynthesisRow | undefined;
  /** How many top-ranked locations to show - the full ranking lives on `/locations`. */
  limit?: number;
}

export function HotspotCard({ hotspotSynthesis, limit = 5 }: HotspotCardProps) {
  const { t, tEnum } = useI18n();

  const hotspots = hotspotSynthesis ? parseHotspotEntries(hotspotSynthesis).slice(0, limit) : [];

  return (
    <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-sm font-medium text-muted-foreground">{t("dashboard.hotspots")}</h2>
        <Link
          href="/locations"
          className="text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
        >
          {t("locations.viewAll")}
        </Link>
      </div>

      {hotspots.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("dashboard.noHotspotsYet")}</p>
      ) : (
        <ul className="space-y-2">
          {hotspots.map((entry, index) => (
            <li key={entry.location} className="flex flex-wrap items-center gap-2 text-sm">
              <span className="w-5 shrink-0 font-mono text-xs text-muted-foreground">{index + 1}.</span>
              <span className="font-medium">{entry.location}</span>
              <Badge variant="outline" className={ACTIVITY_LEVEL_BADGE_CLASS[entry.activityLevel]}>
                {tEnum("activityLevel", entry.activityLevel)}
              </Badge>
              <Badge variant="outline" className={TREND_BADGE_CLASS[entry.trend]}>
                {tEnum("developmentTrend", entry.trend)}
              </Badge>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
