"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { ArrowDown, ArrowUp, Building2, Search } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { formatArea, formatRelativeTime } from "@/lib/utils";
import { NEW_THRESHOLD_MS } from "@/lib/constants";
import type { InvestmentWithState } from "@/lib/types";

interface InvestmentsViewProps {
  investments: InvestmentWithState[];
}

const ALL_DEVELOPERS = "__all__";
const COLUMNS_COUNT = 5;

export function InvestmentsView({ investments }: InvestmentsViewProps) {
  const { t, locale } = useI18n();
  const [search, setSearch] = useState("");
  const [developer, setDeveloper] = useState<string>(ALL_DEVELOPERS);
  const [showArchived, setShowArchived] = useState(false);
  const [sortDesc, setSortDesc] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const developers = useMemo(
    () => Array.from(new Set(investments.map((i) => i.developer))).sort(),
    [investments]
  );

  const filtered = useMemo(() => {
    const result = investments.filter((investment) => {
      if (!showArchived && investment.archived) return false;
      if (developer !== ALL_DEVELOPERS && investment.developer !== developer) return false;
      if (search) {
        const haystack = `${investment.name} ${investment.location ?? ""}`.toLowerCase();
        if (!haystack.includes(search.toLowerCase())) return false;
      }
      return true;
    });
    result.sort((a, b) => {
      const diff = new Date(a.first_seen_at).getTime() - new Date(b.first_seen_at).getTime();
      return sortDesc ? -diff : diff;
    });
    return result;
  }, [investments, search, developer, showArchived, sortDesc]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("investments.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("investments.subtitle")}</p>
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
        <Select value={developer} onValueChange={(value) => setDeveloper(value ?? ALL_DEVELOPERS)}>
          <SelectTrigger className="w-48">
            <SelectValue>
              {(value: string) => (value === ALL_DEVELOPERS ? t("investments.allDevelopers") : value)}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_DEVELOPERS}>{t("investments.allDevelopers")}</SelectItem>
            {developers.map((dev) => (
              <SelectItem key={dev} value={dev}>
                {dev}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Switch checked={showArchived} onCheckedChange={setShowArchived} />
          {t("investments.showArchived")}
        </label>
        <span className="ml-auto text-xs text-muted-foreground">
          {filtered.length} / {investments.length}
        </span>
      </div>

      {filtered.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("investments.noResults")}
        </p>
      ) : (
        <div className="rounded-xl border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("investments.title")}</TableHead>
                <TableHead className="hidden md:table-cell">{t("sources.title")}</TableHead>
                <TableHead className="hidden lg:table-cell">{t("investments.houseArea")}</TableHead>
                <TableHead>
                  <button
                    type="button"
                    onClick={() => setSortDesc((prev) => !prev)}
                    className="inline-flex items-center gap-1 hover:text-foreground"
                  >
                    {t("investments.firstSeen")}
                    {sortDesc ? <ArrowDown className="size-3.5" /> : <ArrowUp className="size-3.5" />}
                  </button>
                </TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((investment) => {
                const isNew =
                  Date.now() - new Date(investment.first_seen_at).getTime() < NEW_THRESHOLD_MS;
                const houseArea = formatArea(investment.house_area_min, investment.house_area_max, t);
                const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max, t);
                const isOpen = expandedId === investment.id;

                return (
                  <ExpandableTableRow
                    key={investment.id}
                    isOpen={isOpen}
                    onToggle={() => setExpandedId(isOpen ? null : investment.id)}
                    columnsCount={COLUMNS_COUNT}
                    data={investment}
                  >
                    <TableCell>
                      <Link
                        href={`/investments/${investment.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="flex items-center gap-3 whitespace-normal"
                      >
                        <div className="relative size-10 shrink-0 overflow-hidden rounded-lg bg-muted">
                          {investment.image_url ? (
                            <Image
                              src={investment.image_url}
                              alt={investment.name}
                              fill
                              sizes="40px"
                              className="object-cover"
                              unoptimized
                            />
                          ) : (
                            <div className="flex h-full items-center justify-center">
                              <Building2 className="size-4 text-muted-foreground" />
                            </div>
                          )}
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="truncate font-medium hover:underline">
                              {investment.name}
                            </span>
                            {isNew ? (
                              <Badge className="bg-emerald-500 text-white hover:bg-emerald-500">
                                {t("investments.new")}
                              </Badge>
                            ) : null}
                            {investment.archived ? (
                              <Badge variant="secondary">{t("investments.archive")}</Badge>
                            ) : null}
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {investment.location ?? t("investments.unknownLocation")}
                          </span>
                        </div>
                      </Link>
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {investment.developer}
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground lg:table-cell">
                      {houseArea ?? plotArea ?? "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatRelativeTime(investment.first_seen_at, locale)}
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
