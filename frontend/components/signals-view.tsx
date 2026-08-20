"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { formatRelativeTime, cn } from "@/lib/utils";
import type { InvestmentSignalRow } from "@/lib/types";

interface SignalsViewProps {
  signals: InvestmentSignalRow[];
}

const ALL_TYPES = "__all__";
const COLUMNS_COUNT = 5;

export function SignalsView({ signals }: SignalsViewProps) {
  const { t, tEnum, locale } = useI18n();
  const [search, setSearch] = useState("");
  const [signalType, setSignalType] = useState<string>(ALL_TYPES);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const signalTypes = useMemo(
    () => Array.from(new Set(signals.map((s) => s.signal_type))).sort(),
    [signals]
  );

  // Signals sharing the same source and case reference are different filing
  // stages of the same underlying case (e.g. "wszczęcie postępowania" ->
  // "decyzja końcowa") - group them client-side (no scan-time matching
  // needed, unlike investment<->signal correlation: this is a plain
  // reference-equality group-by within a single source's own numbering
  // scheme, not a fuzzy/feature-based match). Never group across different
  // sources, since a case reference is only unique within its own
  // municipality's numbering scheme.
  const caseGroups = useMemo(() => {
    const groups = new Map<string, InvestmentSignalRow[]>();
    signals.forEach((signal) => {
      if (!signal.reference) return;
      const key = `${signal.source}:${signal.reference}`;
      const existing = groups.get(key);
      if (existing) existing.push(signal);
      else groups.set(key, [signal]);
    });
    return groups;
  }, [signals]);

  const filtered = useMemo(() => {
    return signals.filter((signal) => {
      if (signalType !== ALL_TYPES && signal.signal_type !== signalType) return false;
      if (search) {
        const haystack = `${signal.title} ${signal.location ?? ""} ${signal.municipality}`.toLowerCase();
        if (!haystack.includes(search.toLowerCase())) return false;
      }
      return true;
    });
  }, [signals, search, signalType]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("signals.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("signals.subtitle")}</p>
      </div>

      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-border bg-card p-3">
        <div className="relative max-w-xs flex-1 min-w-[180px]">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t("investments.searchLocation")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-8"
          />
        </div>
        <Select value={signalType} onValueChange={(value) => setSignalType(value ?? ALL_TYPES)}>
          <SelectTrigger className="w-56">
            <SelectValue>{(value: string) => (value === ALL_TYPES ? t("signals.type") : tEnum("signalType", value))}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_TYPES}>{t("signals.type")}</SelectItem>
            {signalTypes.map((type) => (
              <SelectItem key={type} value={type}>
                {tEnum("signalType", type)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <span className="ml-auto text-xs text-muted-foreground">
          {filtered.length} / {signals.length}
        </span>
      </div>

      {filtered.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("signals.noResults")}
        </p>
      ) : (
        <div className="rounded-xl border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("signals.type")}</TableHead>
                <TableHead>{t("signals.municipality")}</TableHead>
                <TableHead className="hidden md:table-cell">{t("signals.location")}</TableHead>
                <TableHead>{t("signals.detected")}</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((signal) => {
                const isOpen = expandedId === signal.id;
                const caseGroup = signal.reference ? caseGroups.get(`${signal.source}:${signal.reference}`) : undefined;
                const relatedSignals = caseGroup?.filter((s) => s.id !== signal.id) ?? [];

                return (
                  <ExpandableTableRow
                    key={signal.id}
                    isOpen={isOpen}
                    onToggle={() => setExpandedId(isOpen ? null : signal.id)}
                    columnsCount={COLUMNS_COUNT}
                    data={signal}
                    expandedExtra={
                      relatedSignals.length > 0 ? (
                        <div className="border-b border-border px-4 py-3">
                          <h3 className="mb-2 text-xs font-medium text-muted-foreground">
                            {t("signals.caseHistory").replace("{count}", String(caseGroup!.length))}
                          </h3>
                          <ul className="space-y-1.5">
                            {[...caseGroup!]
                              .sort((a, b) => new Date(a.detected_at).getTime() - new Date(b.detected_at).getTime())
                              .map((stage) => (
                                <li
                                  key={stage.id}
                                  className={cn(
                                    "flex items-center gap-2 text-xs",
                                    stage.id === signal.id ? "text-foreground" : "text-muted-foreground"
                                  )}
                                >
                                  <Badge variant="outline" className="shrink-0 text-[10px]">
                                    {formatRelativeTime(stage.detected_at, locale)}
                                  </Badge>
                                  <span className="truncate" title={stage.title}>
                                    {stage.title}
                                  </span>
                                </li>
                              ))}
                          </ul>
                        </div>
                      ) : undefined
                    }
                  >
                    <TableCell>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Badge variant="outline" className="w-fit">
                            {tEnum("signalType", signal.signal_type)}
                          </Badge>
                          {relatedSignals.length > 0 ? (
                            <Badge variant="secondary" className="w-fit text-[10px]">
                              {t("signals.stages").replace("{count}", String(caseGroup!.length))}
                            </Badge>
                          ) : null}
                        </div>
                        <span className="max-w-md truncate text-sm" title={signal.title}>
                          {signal.title}
                        </span>
                        {signal.reference ? (
                          <span className="font-mono text-xs text-muted-foreground">{signal.reference}</span>
                        ) : null}
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{signal.municipality}</TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {signal.location ?? "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatRelativeTime(signal.detected_at, locale)}
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
