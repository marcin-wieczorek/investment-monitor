"use client";

import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { formatRelativeTime } from "@/lib/utils";
import { STALE_THRESHOLD_MS } from "@/lib/constants";
import type { SourceSnapshotRow } from "@/lib/types";

const COLUMNS_COUNT = 4;

export function SourcesView({ sources }: { sources: SourceSnapshotRow[] }) {
  const { t, locale } = useI18n();
  const [expandedSource, setExpandedSource] = useState<string | null>(null);

  const staleCount = sources.filter(
    (s) => Date.now() - new Date(s.captured_at).getTime() >= STALE_THRESHOLD_MS
  ).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("sources.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("sources.subtitle")}</p>
      </div>

      {staleCount > 0 ? (
        <div className="flex items-center gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-600 dark:text-amber-400">
          <AlertTriangle className="size-5 shrink-0" />
          <span>
            {staleCount} {t("sources.staleWarning")}
          </span>
        </div>
      ) : null}

      {sources.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("sources.neverScanned")}
        </p>
      ) : (
        <div className="rounded-xl border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("sources.title")}</TableHead>
                <TableHead>{t("sources.investmentCount")}</TableHead>
                <TableHead>{t("sources.lastCaptured")}</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {sources.map((source) => {
                const isHealthy =
                  Date.now() - new Date(source.captured_at).getTime() < STALE_THRESHOLD_MS;
                const isOpen = expandedSource === source.source;

                return (
                  <ExpandableTableRow
                    key={source.source}
                    isOpen={isOpen}
                    onToggle={() => setExpandedSource(isOpen ? null : source.source)}
                    columnsCount={COLUMNS_COUNT}
                    data={source}
                  >
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-medium">{source.source}</span>
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
                      </div>
                    </TableCell>
                    <TableCell className="font-mono tabular-nums text-muted-foreground">
                      {source.investment_count}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatRelativeTime(source.captured_at, locale)}
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
