"use client";

import { useState } from "react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { NewInvestmentsChart } from "@/components/charts/new-investments-chart";
import { formatRelativeTime } from "@/lib/utils";
import type { MonitoringRunRow } from "@/lib/types";

const COLUMNS_COUNT = 6;

export function HistoryView({ runs }: { runs: MonitoringRunRow[] }) {
  const { t, tEnum, locale } = useI18n();
  const [expandedId, setExpandedId] = useState<number | null>(null);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("history.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("history.subtitle")}</p>
      </div>

      {runs.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("history.noRuns")}
        </p>
      ) : (
        <>
          <div className="rounded-2xl border border-border bg-card p-5 md:p-6">
            <h2 className="mb-4 text-sm font-medium text-muted-foreground">
              {t("dashboard.newInvestmentsChart")}
            </h2>
            <NewInvestmentsChart runs={runs} />
          </div>

          <div className="rounded-xl border border-border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("history.status")}</TableHead>
                  <TableHead>{t("history.started")}</TableHead>
                  <TableHead className="hidden sm:table-cell">{t("history.sourcesChecked")}</TableHead>
                  <TableHead className="hidden sm:table-cell">{t("history.sourcesFailed")}</TableHead>
                  <TableHead>{t("history.newInvestments")}</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {runs.map((run) => {
                  const isOpen = expandedId === run.id;
                  return (
                    <ExpandableTableRow
                      key={run.id}
                      isOpen={isOpen}
                      onToggle={() => setExpandedId(isOpen ? null : run.id)}
                      columnsCount={COLUMNS_COUNT}
                      data={run}
                    >
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={
                            run.status === "SUCCESS"
                              ? "border-emerald-500/30 text-emerald-500 dark:text-emerald-400"
                              : "border-amber-500/30 text-amber-500 dark:text-amber-400"
                          }
                        >
                          {tEnum("runStatus", run.status)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatRelativeTime(run.started_at, locale)}
                      </TableCell>
                      <TableCell className="hidden font-mono tabular-nums text-muted-foreground sm:table-cell">
                        {run.sources_checked}
                      </TableCell>
                      <TableCell className="hidden font-mono tabular-nums text-muted-foreground sm:table-cell">
                        {run.sources_failed}
                      </TableCell>
                      <TableCell className="font-mono tabular-nums">{run.new_investments}</TableCell>
                      <TableCell>
                        <ExpandChevron open={isOpen} />
                      </TableCell>
                    </ExpandableTableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  );
}
