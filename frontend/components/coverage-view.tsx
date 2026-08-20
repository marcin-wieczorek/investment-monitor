"use client";

import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { StatCard } from "@/components/stat-card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { MunicipalityRegistryRow } from "@/lib/types";

interface CoverageViewProps {
  municipalities: MunicipalityRegistryRow[];
}

const STATUS_BADGE: Record<string, string> = {
  IMPLEMENTED: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  NOT_IMPLEMENTED: "border-border text-muted-foreground",
  BLOCKED: "border-red-500/30 text-red-500 dark:text-red-400",
  DISABLED: "border-border text-muted-foreground",
};

function CoverageBadge({ status, reason }: { status: string; reason?: string | null }) {
  const { t } = useI18n();
  return (
    <Badge
      variant="outline"
      className={cn("text-[10px] uppercase", STATUS_BADGE[status], reason && "cursor-help decoration-dotted underline underline-offset-4")}
      title={reason ?? undefined}
    >
      {t(`coverage.status.${status}` as "coverage.status.IMPLEMENTED")}
    </Badge>
  );
}

export function CoverageView({ municipalities }: CoverageViewProps) {
  const { t } = useI18n();

  const developerCovered = municipalities.filter((m) => m.developer_coverage === "IMPLEMENTED").length;
  const discoveryCovered = municipalities.filter((m) => m.discovery_coverage === "IMPLEMENTED").length;
  const aggregatorCovered = municipalities.filter((m) => m.aggregator_coverage === "IMPLEMENTED").length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("coverage.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("coverage.subtitle")}</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          label={t("sources.developer")}
          value={`${developerCovered} / ${municipalities.length}`}
        />
        <StatCard
          label={t("sources.discovery")}
          value={`${discoveryCovered} / ${municipalities.length}`}
        />
        <StatCard
          label={t("sources.aggregator")}
          value={`${aggregatorCovered} / ${municipalities.length}`}
        />
      </div>

      <div className="rounded-xl border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("coverage.municipality")}</TableHead>
              <TableHead className="hidden md:table-cell">{t("coverage.powiat")}</TableHead>
              <TableHead>{t("sources.developer")}</TableHead>
              <TableHead>{t("sources.discovery")}</TableHead>
              <TableHead>{t("sources.aggregator")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {municipalities.map((m) => (
              <TableRow key={m.id}>
                <TableCell className="font-medium">{m.name}</TableCell>
                <TableCell className="hidden text-muted-foreground md:table-cell">{m.powiat}</TableCell>
                <TableCell>
                  <CoverageBadge status={m.developer_coverage} />
                </TableCell>
                <TableCell>
                  <CoverageBadge status={m.discovery_coverage} reason={m.discovery_blocked_reason} />
                </TableCell>
                <TableCell>
                  <CoverageBadge status={m.aggregator_coverage} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
