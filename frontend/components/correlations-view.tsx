"use client";

import { useState } from "react";
import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { formatRelativeTime } from "@/lib/utils";
import type { CorrelationRow } from "@/lib/types";

const COLUMNS_COUNT = 5;

const CONFIDENCE_STYLES: Record<string, string> = {
  HIGH: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  MEDIUM: "border-amber-500/30 text-amber-500 dark:text-amber-400",
  LOW: "border-muted-foreground/30 text-muted-foreground",
};

export function CorrelationsView({ correlations }: { correlations: CorrelationRow[] }) {
  const { t, locale } = useI18n();
  const [expandedId, setExpandedId] = useState<number | null>(null);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("correlations.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("correlations.subtitle")}</p>
      </div>

      {correlations.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("correlations.noResults")}
        </p>
      ) : (
        <div className="rounded-xl border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("correlations.signal")}</TableHead>
                <TableHead>{t("correlations.investment")}</TableHead>
                <TableHead>{t("correlations.confidence")}</TableHead>
                <TableHead className="hidden md:table-cell">{t("history.started")}</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {correlations.map((correlation) => {
                const isOpen = expandedId === correlation.id;
                return (
                  <ExpandableTableRow
                    key={correlation.id}
                    isOpen={isOpen}
                    onToggle={() => setExpandedId(isOpen ? null : correlation.id)}
                    columnsCount={COLUMNS_COUNT}
                    data={correlation}
                  >
                    <TableCell className="max-w-sm">
                      <span className="line-clamp-2 text-sm" title={correlation.signal_title}>
                        {correlation.signal_title}
                      </span>
                    </TableCell>
                    <TableCell>
                      <Link
                        href={`/investments/${correlation.investment_id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="font-medium hover:underline"
                      >
                        {correlation.investment_name}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={CONFIDENCE_STYLES[correlation.confidence]}>
                        {correlation.confidence}
                      </Badge>
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {formatRelativeTime(correlation.created_at, locale)}
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
