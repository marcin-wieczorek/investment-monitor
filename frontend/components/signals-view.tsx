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
import { formatRelativeTime } from "@/lib/utils";
import type { InvestmentSignalRow } from "@/lib/types";

interface SignalsViewProps {
  signals: InvestmentSignalRow[];
}

const ALL_TYPES = "__all__";
const COLUMNS_COUNT = 5;

export function SignalsView({ signals }: SignalsViewProps) {
  const { t, locale } = useI18n();
  const [search, setSearch] = useState("");
  const [signalType, setSignalType] = useState<string>(ALL_TYPES);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const signalTypes = useMemo(
    () => Array.from(new Set(signals.map((s) => s.signal_type))).sort(),
    [signals]
  );

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
            <SelectValue>{(value: string) => (value === ALL_TYPES ? t("signals.type") : value)}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_TYPES}>{t("signals.type")}</SelectItem>
            {signalTypes.map((type) => (
              <SelectItem key={type} value={type}>
                {type}
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
                return (
                  <ExpandableTableRow
                    key={signal.id}
                    isOpen={isOpen}
                    onToggle={() => setExpandedId(isOpen ? null : signal.id)}
                    columnsCount={COLUMNS_COUNT}
                    data={signal}
                  >
                    <TableCell>
                      <div className="flex flex-col gap-1">
                        <Badge variant="outline" className="w-fit">
                          {signal.signal_type}
                        </Badge>
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
