"use client";

import Link from "next/link";
import { Clock, Sparkles, Activity, Building2, Radar, Users, Map, TrendingUp, Search } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { StatCard } from "@/components/stat-card";
import { RecentInvestmentsTable } from "@/components/recent-investments-table";
import { NewInvestmentsChart } from "@/components/charts/new-investments-chart";
import { ScanSuccessChart } from "@/components/charts/scan-success-chart";
import { LeadTimeTrendChart } from "@/components/charts/lead-time-trend-chart";
import { CoverageBreakdown } from "@/components/coverage-breakdown";
import { SourcesNeedingAttention } from "@/components/sources-needing-attention";
import { formatRelativeTime } from "@/lib/utils";
import { STALE_THRESHOLD_MS } from "@/lib/constants";
import type {
  CorrelationRow,
  DeveloperRegistryRow,
  InvestmentWithState,
  MonitoringRunRow,
  MunicipalityRegistryRow,
  SourceSnapshotRow,
} from "@/lib/types";

interface DashboardViewProps {
  recentInvestments: InvestmentWithState[];
  sources: SourceSnapshotRow[];
  runs: MonitoringRunRow[];
  totalInvestments: number;
  totalSignals: number;
  developers: DeveloperRegistryRow[];
  municipalities: MunicipalityRegistryRow[];
  avgLeadTimeDays: number | null;
  aggregatorOnlyCount: number;
  correlations: CorrelationRow[];
}

export function DashboardView({
  recentInvestments,
  sources,
  runs,
  totalInvestments,
  totalSignals,
  developers,
  municipalities,
  avgLeadTimeDays,
  aggregatorOnlyCount,
  correlations,
}: DashboardViewProps) {
  const { t, locale } = useI18n();
  const latestRun = runs[0];

  const healthySources = sources.filter(
    (s) => Date.now() - new Date(s.captured_at).getTime() < STALE_THRESHOLD_MS
  ).length;

  const monitoredDevelopers = developers.filter((d) => d.status === "MONITORED").length;
  const developerCoverage = municipalities.filter((m) => m.developer_coverage === "IMPLEMENTED").length;

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

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Link href="/developers" className="block">
          <StatCard
            icon={Users}
            label={t("developers.monitored")}
            value={`${monitoredDevelopers} / ${developers.length}`}
          />
        </Link>
        <Link href="/coverage" className="block">
          <StatCard
            icon={Map}
            label={t("sources.developer")}
            value={`${developerCoverage} / ${municipalities.length}`}
          />
        </Link>
        <Link href="/correlations" className="block">
          <StatCard
            icon={TrendingUp}
            label={t("dashboard.avgLeadTime")}
            value={avgLeadTimeDays == null ? t("dashboard.noData") : t("dashboard.leadTimeDays").replace("{days}", Math.round(avgLeadTimeDays).toString())}
            tone={avgLeadTimeDays != null && avgLeadTimeDays > 0 ? "success" : "default"}
          />
        </Link>
        <Link href="/investments?aggregatorOnly=1" className="block">
          <StatCard
            icon={Search}
            label={t("dashboard.aggregatorOnlyDiscoveries")}
            value={aggregatorOnlyCount}
            tone={aggregatorOnlyCount > 0 ? "warning" : "default"}
          />
        </Link>
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
        <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
          <h2 className="mb-4 text-sm font-medium text-muted-foreground">
            {t("dashboard.leadTimeTrendChart")}
          </h2>
          <LeadTimeTrendChart correlations={correlations} />
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
          <h2 className="mb-4 text-sm font-medium text-muted-foreground">
            {t("dashboard.coverageBreakdown")}
          </h2>
          <CoverageBreakdown developers={developers} municipalities={municipalities} />
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
        <h2 className="mb-4 text-sm font-medium text-muted-foreground">
          {t("dashboard.sourcesNeedingAttention")}
        </h2>
        <SourcesNeedingAttention sources={sources} developers={developers} municipalities={municipalities} />
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
