"use client";

import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { StatCard } from "@/components/stat-card";
import { InvestmentCard } from "@/components/investment-card";
import { ScanButton } from "@/components/scan-button";
import { formatRelativeTime } from "@/lib/utils";
import type { InvestmentWithState, MonitoringRunRow, SourceSnapshotRow } from "@/lib/types";

interface DashboardViewProps {
  recentInvestments: InvestmentWithState[];
  sources: SourceSnapshotRow[];
  latestRun: MonitoringRunRow | undefined;
}

export function DashboardView({ recentInvestments, sources, latestRun }: DashboardViewProps) {
  const { t, locale } = useI18n();

  const healthySources = sources.filter(
    (s) => Date.now() - new Date(s.captured_at).getTime() < 24 * 60 * 60 * 1000
  ).length;

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{t("dashboard.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("dashboard.subtitle")}</p>
        </div>
        <ScanButton />
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          label={t("dashboard.lastScan")}
          value={
            latestRun ? formatRelativeTime(latestRun.started_at, locale) : t("dashboard.neverRun")
          }
        />
        <StatCard
          label={t("dashboard.newInvestments")}
          value={latestRun?.new_investments ?? 0}
          tone={latestRun && latestRun.new_investments > 0 ? "success" : "default"}
        />
        <StatCard
          label={t("dashboard.sourcesHealthy")}
          value={`${healthySources}/${sources.length}`}
          tone={healthySources === sources.length ? "success" : "warning"}
        />
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
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

        {recentInvestments.length === 0 ? (
          <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
            {t("dashboard.noInvestmentsYet")}
          </p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {recentInvestments.map((investment) => (
              <InvestmentCard key={investment.id} investment={investment} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
