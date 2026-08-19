"use client";

import Link from "next/link";
import { Clock, Sparkles, Activity, Building2, Radar } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { StatCard } from "@/components/stat-card";
import { RecentInvestmentsTable } from "@/components/recent-investments-table";
import { NewInvestmentsChart } from "@/components/charts/new-investments-chart";
import { ScanSuccessChart } from "@/components/charts/scan-success-chart";
import { formatRelativeTime } from "@/lib/utils";
import { STALE_THRESHOLD_MS } from "@/lib/constants";
import type { InvestmentWithState, MonitoringRunRow, SourceSnapshotRow } from "@/lib/types";

interface DashboardViewProps {
  recentInvestments: InvestmentWithState[];
  sources: SourceSnapshotRow[];
  runs: MonitoringRunRow[];
  totalInvestments: number;
  totalSignals: number;
}

export function DashboardView({
  recentInvestments,
  sources,
  runs,
  totalInvestments,
  totalSignals,
}: DashboardViewProps) {
  const { t, locale } = useI18n();
  const latestRun = runs[0];

  const healthySources = sources.filter(
    (s) => Date.now() - new Date(s.captured_at).getTime() < STALE_THRESHOLD_MS
  ).length;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("dashboard.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("dashboard.subtitle")}</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <StatCard
          icon={Building2}
          label={t("dashboard.totalInvestments")}
          value={totalInvestments}
        />
        <StatCard
          icon={Radar}
          label={t("dashboard.totalSignals")}
          value={totalSignals}
        />
        <StatCard
          icon={Clock}
          label={t("dashboard.lastScan")}
          value={
            latestRun ? formatRelativeTime(latestRun.started_at, locale) : t("dashboard.neverRun")
          }
        />
        <StatCard
          icon={Sparkles}
          label={t("dashboard.newInvestments")}
          value={latestRun?.new_investments ?? 0}
          tone={latestRun && latestRun.new_investments > 0 ? "success" : "default"}
        />
        <StatCard
          icon={Activity}
          label={t("dashboard.sourcesHealthy")}
          value={`${healthySources}/${sources.length}`}
          tone={healthySources === sources.length ? "success" : "warning"}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
          <h2 className="mb-4 text-sm font-medium text-muted-foreground">
            {t("dashboard.newInvestmentsChart")}
          </h2>
          <NewInvestmentsChart runs={runs} />
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
          <h2 className="mb-4 text-sm font-medium text-muted-foreground">
            {t("dashboard.scanSuccessChart")}
          </h2>
          <ScanSuccessChart runs={runs} />
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-medium text-muted-foreground">
            {t("dashboard.recentlyDetected")}
          </h2>
          <Link
            href="/investments"
            className="text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
          >
            {t("dashboard.viewAll")}
          </Link>
        </div>
        <RecentInvestmentsTable investments={recentInvestments} />
      </div>
    </div>
  );
}
