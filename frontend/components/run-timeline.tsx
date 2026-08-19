"use client";

import { useI18n } from "@/lib/i18n";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatRelativeTime } from "@/lib/utils";
import type { MonitoringRunRow } from "@/lib/types";

export function RunTimeline({ runs }: { runs: MonitoringRunRow[] }) {
  const { t, locale } = useI18n();

  if (runs.length === 0) {
    return (
      <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
        {t("history.noRuns")}
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {runs.map((run) => (
        <Card key={run.id}>
          <CardContent className="flex flex-wrap items-center justify-between gap-3 py-4">
            <div className="flex items-center gap-3">
              <Badge
                variant="outline"
                className={
                  run.status === "SUCCESS"
                    ? "border-emerald-500/30 text-emerald-500 dark:text-emerald-400"
                    : "border-amber-500/30 text-amber-500 dark:text-amber-400"
                }
              >
                {run.status}
              </Badge>
              <span className="text-sm text-muted-foreground">
                {formatRelativeTime(run.started_at, locale)}
              </span>
            </div>
            <div className="flex gap-4 font-mono text-sm tabular-nums text-muted-foreground">
              <span>
                {t("history.sourcesChecked")}: {run.sources_checked}
              </span>
              <span>
                {t("history.sourcesFailed")}: {run.sources_failed}
              </span>
              <span className="text-foreground">
                {t("history.newInvestments")}: {run.new_investments}
              </span>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
