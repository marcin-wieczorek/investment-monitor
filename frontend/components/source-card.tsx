"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useI18n } from "@/lib/i18n";
import { formatRelativeTime } from "@/lib/utils";
import type { SourceSnapshotRow } from "@/lib/types";

const STALE_THRESHOLD_MS = 24 * 60 * 60 * 1000;

export function SourceCard({ source }: { source: SourceSnapshotRow }) {
  const { t, locale } = useI18n();

  const capturedAt = new Date(source.captured_at).getTime();
  const isHealthy = Date.now() - capturedAt < STALE_THRESHOLD_MS;

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="font-mono text-sm font-medium">{source.source}</CardTitle>
        <Badge
          variant="outline"
          className={
            isHealthy
              ? "border-emerald-500/30 text-emerald-500 dark:text-emerald-400"
              : "border-amber-500/30 text-amber-500 dark:text-amber-400"
          }
        >
          {isHealthy ? t("sources.healthy") : t("sources.stale")}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-1 text-sm text-muted-foreground">
        <div className="flex justify-between">
          <span>{t("sources.investmentCount")}</span>
          <span className="font-mono tabular-nums text-foreground">
            {source.investment_count}
          </span>
        </div>
        <div className="flex justify-between">
          <span>{t("sources.lastCaptured")}</span>
          <span>{formatRelativeTime(source.captured_at, locale)}</span>
        </div>
      </CardContent>
    </Card>
  );
}
