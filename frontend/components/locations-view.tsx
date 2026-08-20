"use client";

import { useState } from "react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { parseHotspotEntries, parseJsonArray } from "@/lib/hotspot-utils";
import { formatRelativeTime } from "@/lib/utils";
import {
  ACTION_BADGE_CLASS,
  ACTIVITY_LEVEL_BADGE_CLASS,
  TREND_BADGE_CLASS,
} from "@/lib/badge-styles";
import type { HotspotSynthesisRow, LocationSynthesisRow } from "@/lib/types";

interface LocationsViewProps {
  locationSyntheses: LocationSynthesisRow[];
  hotspotSynthesis: HotspotSynthesisRow | undefined;
}

const COLUMNS_COUNT = 5;

export function LocationsView({ locationSyntheses, hotspotSynthesis }: LocationsViewProps) {
  const { t, tEnum, locale } = useI18n();
  const [expandedLocation, setExpandedLocation] = useState<string | null>(null);

  const hotspots = hotspotSynthesis ? parseHotspotEntries(hotspotSynthesis) : [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("locations.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("locations.subtitle")}</p>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
        <h2 className="mb-1 text-sm font-medium">{t("locations.hotspotsTitle")}</h2>
        <p className="mb-4 text-xs text-muted-foreground">{t("locations.hotspotsSubtitle")}</p>

        {hotspotSynthesis == null || hotspots.length === 0 ? (
          <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            {t("locations.noHotspots")}
          </p>
        ) : (
          <>
            <ol className="space-y-2">
              {hotspots.map((entry, index) => (
                <li
                  key={entry.location}
                  className="flex flex-wrap items-center gap-2 rounded-lg border border-border p-3 text-sm"
                >
                  <span className="w-5 shrink-0 font-mono text-xs text-muted-foreground">{index + 1}.</span>
                  <span className="font-medium">{entry.location}</span>
                  <Badge variant="outline" className={ACTIVITY_LEVEL_BADGE_CLASS[entry.activityLevel]}>
                    {tEnum("activityLevel", entry.activityLevel)}
                  </Badge>
                  <Badge variant="outline" className={TREND_BADGE_CLASS[entry.trend]}>
                    {tEnum("developmentTrend", entry.trend)}
                  </Badge>
                  <span className="ml-auto text-xs text-muted-foreground">
                    {t("locations.relevance")}:{" "}
                    <Badge variant="outline" className={ACTIVITY_LEVEL_BADGE_CLASS[entry.relevanceToProfile]}>
                      {tEnum("activityLevel", entry.relevanceToProfile)}
                    </Badge>
                  </span>
                  <p className="w-full text-xs text-muted-foreground">{entry.reason}</p>
                </li>
              ))}
            </ol>
            <p className="mt-4 text-sm">{hotspotSynthesis.summary}</p>
            {hotspotSynthesis.recommendation ? (
              <p className="mt-2 text-sm text-muted-foreground">{hotspotSynthesis.recommendation}</p>
            ) : null}
            {parseJsonArray(hotspotSynthesis.emerging_areas).length > 0 ? (
              <div className="mt-4 flex flex-wrap items-center gap-2">
                <span className="text-xs text-muted-foreground">{t("locations.emergingAreas")}:</span>
                {parseJsonArray(hotspotSynthesis.emerging_areas).map((area) => (
                  <Badge key={area} variant="secondary">
                    {area}
                  </Badge>
                ))}
              </div>
            ) : null}
          </>
        )}
      </div>

      {locationSyntheses.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("locations.noResults")}
        </p>
      ) : (
        <div className="rounded-xl border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("locations.location")}</TableHead>
                <TableHead className="hidden md:table-cell">{t("locations.municipality")}</TableHead>
                <TableHead>{t("locations.trend")}</TableHead>
                <TableHead>{t("locations.action")}</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {locationSyntheses.map((synthesis) => {
                const isOpen = expandedLocation === synthesis.location;
                const keyDevelopers = parseJsonArray(synthesis.key_developers);
                const opportunities = parseJsonArray(synthesis.opportunities);
                const risks = parseJsonArray(synthesis.risks);

                return (
                  <ExpandableTableRow
                    key={synthesis.location}
                    isOpen={isOpen}
                    onToggle={() => setExpandedLocation(isOpen ? null : synthesis.location)}
                    columnsCount={COLUMNS_COUNT}
                    data={synthesis}
                    expandedExtra={
                      <div className="space-y-3 border-b border-border px-4 py-3 text-sm">
                        <p>{synthesis.summary}</p>
                        <div className="grid gap-3 sm:grid-cols-2">
                          <div>
                            <h3 className="mb-1 text-xs font-medium text-muted-foreground">
                              {t("locations.signals")} / {t("locations.investments")}
                            </h3>
                            <p>
                              {synthesis.signal_count} / {synthesis.investment_count}
                              {synthesis.average_lead_time_days != null
                                ? ` · ${t("locations.avgLeadTime")}: ${synthesis.average_lead_time_days >= 0 ? "+" : ""}${Math.round(synthesis.average_lead_time_days)}d`
                                : ""}
                            </p>
                          </div>
                          {synthesis.estimated_timeline ? (
                            <div>
                              <h3 className="mb-1 text-xs font-medium text-muted-foreground">
                                {t("locations.estimatedTimeline")}
                              </h3>
                              <p>{synthesis.estimated_timeline}</p>
                            </div>
                          ) : null}
                        </div>
                        {keyDevelopers.length > 0 ? (
                          <div>
                            <h3 className="mb-1 text-xs font-medium text-muted-foreground">
                              {t("locations.keyDevelopers")}
                            </h3>
                            <div className="flex flex-wrap gap-1.5">
                              {keyDevelopers.map((dev) => (
                                <Badge key={dev} variant="secondary">
                                  {dev}
                                </Badge>
                              ))}
                            </div>
                          </div>
                        ) : null}
                        {opportunities.length > 0 ? (
                          <div>
                            <h3 className="mb-1 text-xs font-medium text-muted-foreground">
                              {t("locations.opportunities")}
                            </h3>
                            <ul className="list-inside list-disc space-y-0.5 text-muted-foreground">
                              {opportunities.map((item, i) => (
                                <li key={i}>{item}</li>
                              ))}
                            </ul>
                          </div>
                        ) : null}
                        {risks.length > 0 ? (
                          <div>
                            <h3 className="mb-1 text-xs font-medium text-muted-foreground">{t("locations.risks")}</h3>
                            <ul className="list-inside list-disc space-y-0.5 text-muted-foreground">
                              {risks.map((item, i) => (
                                <li key={i}>{item}</li>
                              ))}
                            </ul>
                          </div>
                        ) : null}
                        <div>
                          <h3 className="mb-1 text-xs font-medium text-muted-foreground">{t("locations.reason")}</h3>
                          <p className="text-muted-foreground">{synthesis.reason}</p>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          {t("locations.synthesizedAt")}: {formatRelativeTime(synthesis.synthesized_at, locale)}
                        </p>
                      </div>
                    }
                  >
                    <TableCell>
                      <div className="flex flex-col gap-0.5">
                        <span className="font-medium">{synthesis.location}</span>
                        <span className="text-xs text-muted-foreground">
                          {synthesis.signal_count} {t("locations.signals").toLowerCase()} ·{" "}
                          {synthesis.investment_count} {t("locations.investments").toLowerCase()}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {synthesis.municipality ?? "—"}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={TREND_BADGE_CLASS[synthesis.development_trend]}>
                        {tEnum("developmentTrend", synthesis.development_trend)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={ACTION_BADGE_CLASS[synthesis.recommended_action]}>
                        {tEnum("recommendedAction", synthesis.recommended_action)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <ExpandChevron open={isOpen} />
                    </TableCell>
                  </ExpandableTableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
