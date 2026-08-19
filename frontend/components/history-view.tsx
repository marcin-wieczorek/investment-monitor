"use client";

import { useI18n } from "@/lib/i18n";
import { RunTimeline } from "@/components/run-timeline";
import type { MonitoringRunRow } from "@/lib/types";

export function HistoryView({ runs }: { runs: MonitoringRunRow[] }) {
  const { t } = useI18n();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("history.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("history.subtitle")}</p>
      </div>
      <RunTimeline runs={runs} />
    </div>
  );
}
