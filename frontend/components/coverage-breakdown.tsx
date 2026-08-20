"use client";

import { useI18n } from "@/lib/i18n";
import type { DeveloperRegistryRow, MunicipalityRegistryRow } from "@/lib/types";

interface CoverageBreakdownProps {
  developers: DeveloperRegistryRow[];
  municipalities: MunicipalityRegistryRow[];
}

function ProgressRow({ label, count, total }: { label: string; count: number; total: number }) {
  const pct = total === 0 ? 0 : Math.round((count / total) * 100);
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-mono tabular-nums text-muted-foreground">
          {count} / {total}
        </span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-emerald-500" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function CoverageBreakdown({ developers, municipalities }: CoverageBreakdownProps) {
  const { t } = useI18n();

  const tierA = developers.filter((d) => d.tier === "A");
  const tierB = developers.filter((d) => d.tier === "B");
  const tierAMonitored = tierA.filter((d) => d.status === "MONITORED").length;
  const tierBMonitored = tierB.filter((d) => d.status === "MONITORED").length;

  const developerCoverage = municipalities.filter((m) => m.developer_coverage === "IMPLEMENTED").length;
  const discoveryCoverage = municipalities.filter((m) => m.discovery_coverage === "IMPLEMENTED").length;
  const aggregatorCoverage = municipalities.filter((m) => m.aggregator_coverage === "IMPLEMENTED").length;

  return (
    <div className="space-y-4">
      <ProgressRow label={t("dashboard.tierAMonitored")} count={tierAMonitored} total={tierA.length} />
      <ProgressRow label={t("dashboard.tierBMonitored")} count={tierBMonitored} total={tierB.length} />
      <ProgressRow label={t("sources.developer")} count={developerCoverage} total={municipalities.length} />
      <ProgressRow label={t("sources.discovery")} count={discoveryCoverage} total={municipalities.length} />
      <ProgressRow label={t("sources.aggregator")} count={aggregatorCoverage} total={municipalities.length} />
    </div>
  );
}
