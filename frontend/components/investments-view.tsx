"use client";

import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import { ArrowDown, ArrowUp, Building2, ChevronDown, ChevronUp, Search, SlidersHorizontal } from "lucide-react";
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
import { Button } from "@/components/ui/button";
import { RangeSlider } from "@/components/range-slider";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { cn, dataCompleteness, formatArea, formatPrice, formatRelativeTime, LOW_COMPLETENESS_THRESHOLD } from "@/lib/utils";
import { NEW_THRESHOLD_MS } from "@/lib/constants";
import type { InvestmentDuplicateRow, InvestmentWithState } from "@/lib/types";

interface InvestmentsViewProps {
  investments: InvestmentWithState[];
  duplicates?: InvestmentDuplicateRow[];
}

const ALL = "__all__";
const COLUMNS_COUNT = 8;

type SortField = "first_seen_at" | "price_min" | "house_area_max" | "plot_area_max" | "overall_score";

const SOURCE_CATEGORY_BADGE: Record<string, string> = {
  DEVELOPER: "border-blue-500/30 text-blue-500 dark:text-blue-400",
  DISCOVERY: "border-purple-500/30 text-purple-500 dark:text-purple-400",
  AGGREGATOR: "border-orange-500/30 text-orange-500 dark:text-orange-400",
};

/** Lower = more authoritative (see SourceCategory.kt: DEVELOPER > DISCOVERY > AGGREGATOR). */
const CATEGORY_PRIORITY: Record<string, number> = { DEVELOPER: 0, DISCOVERY: 1, AGGREGATOR: 2 };

function categoryPriority(investment: InvestmentWithState): number {
  return investment.source_category ? CATEGORY_PRIORITY[investment.source_category] ?? 3 : 3;
}

/** Deterministic tie-break for picking which side of a duplicate pair is shown as the representative row. */
function isMoreAuthoritative(a: InvestmentWithState, b: InvestmentWithState): boolean {
  const pa = categoryPriority(a);
  const pb = categoryPriority(b);
  if (pa !== pb) return pa < pb;
  if (a.first_seen_at !== b.first_seen_at) return a.first_seen_at < b.first_seen_at;
  return a.id < b.id;
}

interface DuplicateLink {
  investmentId: number;
  confidence: "HIGH" | "MEDIUM" | "LOW";
}

/**
 * Direct (non-transitive) duplicate links per investment id, HIGH/MEDIUM
 * confidence only - LOW-confidence pairs are surfaced nowhere in this
 * view, matching InvestmentDeduplicator's conservative, fail-closed
 * design (a weak name-overlap-only match should never silently merge two
 * rows that might really be different projects).
 */
function buildDuplicateLinks(duplicates: InvestmentDuplicateRow[]): Map<number, DuplicateLink[]> {
  const map = new Map<number, DuplicateLink[]>();
  const add = (id: number, otherId: number, confidence: DuplicateLink["confidence"]) => {
    const list = map.get(id) ?? [];
    list.push({ investmentId: otherId, confidence });
    map.set(id, list);
  };
  duplicates
    .filter((d) => d.confidence !== "LOW")
    .forEach((d) => {
      add(d.investment_id_a, d.investment_id_b, d.confidence);
      add(d.investment_id_b, d.investment_id_a, d.confidence);
    });
  return map;
}

function scoreBadgeClass(score: number): string {
  if (score >= 0.66) return "border-emerald-500/30 text-emerald-500 dark:text-emerald-400";
  if (score >= 0.4) return "border-amber-500/30 text-amber-500 dark:text-amber-400";
  return "border-rose-500/30 text-rose-500 dark:text-rose-400";
}

function ScoreBadge({ investment, t }: { investment: InvestmentWithState; t: (key: string) => string }) {
  const score = investment.overall_score;
  if (score == null) {
    return <span className="text-xs text-muted-foreground">—</span>;
  }
  const completeness = dataCompleteness(investment);
  if (completeness < LOW_COMPLETENESS_THRESHOLD) {
    return (
      <Badge
        variant="outline"
        className="border-border text-muted-foreground"
        title={t("investments.insufficientDataTooltip")}
      >
        {t("investments.insufficientData")}
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className={cn("font-mono tabular-nums", scoreBadgeClass(score))}>
      {Math.round(score * 100)}%
    </Badge>
  );
}

function numericBounds(values: Array<number | null>): [number, number] {
  const known = values.filter((v): v is number => v != null);
  if (known.length === 0) return [0, 0];
  return [Math.floor(Math.min(...known)), Math.ceil(Math.max(...known))];
}

export function InvestmentsView({ investments, duplicates = [] }: InvestmentsViewProps) {
  const { t, tEnum, locale } = useI18n();
  const searchParams = useSearchParams();
  const [search, setSearch] = useState("");
  const [developer, setDeveloper] = useState<string>(ALL);
  const [showArchived, setShowArchived] = useState(false);
  const [showWatchedOnly, setShowWatchedOnly] = useState(false);
  const [showAggregatorOnly, setShowAggregatorOnly] = useState(() => searchParams.get("aggregatorOnly") === "1");
  const [sortField, setSortField] = useState<SortField>("first_seen_at");
  const [sortDesc, setSortDesc] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const [sourceCategory, setSourceCategory] = useState<string>(ALL);
  const [propertyType, setPropertyType] = useState<string>(ALL);
  const [status, setStatus] = useState<string>(ALL);
  const [location, setLocation] = useState<string>(ALL);

  const houseAreaBounds = useMemo(
    () => numericBounds(investments.flatMap((i) => [i.house_area_min, i.house_area_max])),
    [investments]
  );
  const plotAreaBounds = useMemo(
    () => numericBounds(investments.flatMap((i) => [i.plot_area_min, i.plot_area_max])),
    [investments]
  );
  const priceBounds = useMemo(
    () => numericBounds(investments.flatMap((i) => [i.price_min, i.price_max])),
    [investments]
  );

  const [houseAreaRange, setHouseAreaRange] = useState<[number, number]>(houseAreaBounds);
  const [plotAreaRange, setPlotAreaRange] = useState<[number, number]>(plotAreaBounds);
  const [priceRange, setPriceRange] = useState<[number, number]>(priceBounds);

  const developers = useMemo(
    () => Array.from(new Set(investments.map((i) => i.developer))).sort(),
    [investments]
  );
  const propertyTypes = useMemo(
    () => Array.from(new Set(investments.map((i) => i.property_type).filter((v): v is string => v != null))).sort(),
    [investments]
  );
  const statuses = useMemo(
    () => Array.from(new Set(investments.map((i) => i.status).filter((v): v is string => v != null))).sort(),
    [investments]
  );
  const locations = useMemo(
    () => Array.from(new Set(investments.map((i) => i.location).filter((v): v is string => v != null))).sort(),
    [investments]
  );

  const investmentById = useMemo(() => new Map(investments.map((i) => [i.id, i])), [investments]);
  const duplicateLinks = useMemo(() => buildDuplicateLinks(duplicates), [duplicates]);

  /** The id of the most authoritative investment among this row and its direct duplicate links - itself if it has none, or isn't beaten by any of them. */
  const representativeIdFor = useMemo(
    () => (investment: InvestmentWithState): number => {
      const links = duplicateLinks.get(investment.id) ?? [];
      let best = investment;
      links.forEach((link) => {
        const other = investmentById.get(link.investmentId);
        if (other && isMoreAuthoritative(other, best)) best = other;
      });
      return best.id;
    },
    [duplicateLinks, investmentById]
  );

  const filtered = useMemo(() => {
    const result = investments.filter((investment) => {
      if (!showArchived && investment.archived) return false;
      if (showWatchedOnly && !investment.watched) return false;
      if (showAggregatorOnly && !investment.aggregator_only_discovery) return false;
      if (developer !== ALL && investment.developer !== developer) return false;
      if (sourceCategory !== ALL && investment.source_category !== sourceCategory) return false;
      if (propertyType !== ALL && investment.property_type !== propertyType) return false;
      if (status !== ALL && investment.status !== status) return false;
      if (location !== ALL && investment.location !== location) return false;
      if (search) {
        const haystack = `${investment.name} ${investment.location ?? ""}`.toLowerCase();
        if (!haystack.includes(search.toLowerCase())) return false;
      }
      if (!inRange(investment.house_area_min, investment.house_area_max, houseAreaRange, houseAreaBounds)) return false;
      if (!inRange(investment.plot_area_min, investment.plot_area_max, plotAreaRange, plotAreaBounds)) return false;
      if (!inRange(investment.price_min, investment.price_max, priceRange, priceBounds)) return false;
      return true;
    });
    result.sort((a, b) => {
      const diff = sortValue(a, sortField) - sortValue(b, sortField);
      return sortDesc ? -diff : diff;
    });
    return result;
  }, [
    investments,
    search,
    developer,
    showArchived,
    showWatchedOnly,
    showAggregatorOnly,
    sourceCategory,
    propertyType,
    status,
    location,
    houseAreaRange,
    plotAreaRange,
    priceRange,
    houseAreaBounds,
    plotAreaBounds,
    priceBounds,
    sortField,
    sortDesc,
  ]);

  const visibleCount = useMemo(() => {
    const filteredIds = new Set(filtered.map((i) => i.id));
    return filtered.filter((investment) => {
      const representativeId = representativeIdFor(investment);
      return representativeId === investment.id || !filteredIds.has(representativeId);
    }).length;
  }, [filtered, representativeIdFor]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("investments.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("investments.subtitle")}</p>
      </div>

      <div className="space-y-3 rounded-xl border border-border bg-card p-3">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative max-w-xs flex-1 min-w-[180px]">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t("investments.searchLocation")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-8"
            />
          </div>
          <Select value={developer} onValueChange={(value) => setDeveloper(value ?? ALL)}>
            <SelectTrigger className="w-48">
              <SelectValue>{(value: string) => (value === ALL ? t("investments.allDevelopers") : value)}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{t("investments.allDevelopers")}</SelectItem>
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
          <label className="flex items-center gap-2 text-sm text-muted-foreground">
            <Switch checked={showWatchedOnly} onCheckedChange={setShowWatchedOnly} />
            {t("investments.watchedOnly")}
          </label>

          <div className="flex items-center gap-1.5">
            <span className="text-xs text-muted-foreground">{t("filters.sortBy")}</span>
            <Select value={sortField} onValueChange={(value) => setSortField((value as SortField) ?? "first_seen_at")}>
              <SelectTrigger className="w-40">
                <SelectValue>{(value: string) => t(`filters.sort.${value}` as "filters.sort.first_seen_at")}</SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="first_seen_at">{t("filters.sort.first_seen_at")}</SelectItem>
                <SelectItem value="price_min">{t("filters.sort.price_min")}</SelectItem>
                <SelectItem value="house_area_max">{t("filters.sort.house_area_max")}</SelectItem>
                <SelectItem value="plot_area_max">{t("filters.sort.plot_area_max")}</SelectItem>
                <SelectItem value="overall_score">{t("filters.sort.overall_score")}</SelectItem>
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              size="icon-sm"
              onClick={() => setSortDesc((prev) => !prev)}
              title={sortDesc ? t("filters.sortDescending") : t("filters.sortAscending")}
              aria-label={sortDesc ? t("filters.sortDescending") : t("filters.sortAscending")}
            >
              {sortDesc ? <ArrowDown className="size-3.5" /> : <ArrowUp className="size-3.5" />}
            </Button>
          </div>

          <Button
            variant="outline"
            size="sm"
            className="ml-auto"
            onClick={() => setFiltersOpen((prev) => !prev)}
          >
            <SlidersHorizontal className="size-3.5" />
            {t("filters.advanced")}
            {filtersOpen ? <ChevronUp className="size-3.5" /> : <ChevronDown className="size-3.5" />}
          </Button>
          <span className="text-xs text-muted-foreground">
            {visibleCount} / {investments.length}
          </span>
        </div>

        {filtersOpen ? (
          <div className="space-y-4 border-t border-border pt-3">
            <div className="flex flex-wrap items-center gap-3">
              <Select value={sourceCategory} onValueChange={(value) => setSourceCategory(value ?? ALL)}>
                <SelectTrigger className="w-40">
                  <SelectValue>{(value: string) => (value === ALL ? t("investments.allSources") : t(`sources.${value.toLowerCase()}` as "sources.developer"))}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t("investments.allSources")}</SelectItem>
                  <SelectItem value="DEVELOPER">{t("sources.developer")}</SelectItem>
                  <SelectItem value="DISCOVERY">{t("sources.discovery")}</SelectItem>
                  <SelectItem value="AGGREGATOR">{t("sources.aggregator")}</SelectItem>
                </SelectContent>
              </Select>

              <label
                className="flex items-center gap-2 text-sm text-muted-foreground"
                title={t("investments.aggregatorOnlyTooltip")}
              >
                <Switch checked={showAggregatorOnly} onCheckedChange={setShowAggregatorOnly} />
                {t("investments.aggregatorOnly")}
              </label>

              <Select value={propertyType} onValueChange={(value) => setPropertyType(value ?? ALL)}>
                <SelectTrigger className="w-44">
                  <SelectValue>{(value: string) => (value === ALL ? t("investments.allTypes") : tEnum("propertyType", value))}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t("investments.allTypes")}</SelectItem>
                  {propertyTypes.map((pt) => (
                    <SelectItem key={pt} value={pt}>
                      {tEnum("propertyType", pt)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={status} onValueChange={(value) => setStatus(value ?? ALL)}>
                <SelectTrigger className="w-44">
                  <SelectValue>{(value: string) => (value === ALL ? t("investments.allStatuses") : tEnum("investmentStatus", value))}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t("investments.allStatuses")}</SelectItem>
                  {statuses.map((s) => (
                    <SelectItem key={s} value={s}>
                      {tEnum("investmentStatus", s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={location} onValueChange={(value) => setLocation(value ?? ALL)}>
                <SelectTrigger className="w-44">
                  <SelectValue>{(value: string) => (value === ALL ? t("investments.allLocations") : value)}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>{t("investments.allLocations")}</SelectItem>
                  {locations.map((loc) => (
                    <SelectItem key={loc} value={loc}>
                      {loc}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-wrap gap-6">
              {houseAreaBounds[1] > houseAreaBounds[0] ? (
                <RangeSlider
                  label={t("investments.houseArea")}
                  min={houseAreaBounds[0]}
                  max={houseAreaBounds[1]}
                  value={houseAreaRange}
                  onChange={setHouseAreaRange}
                  formatValue={(v) => `${v} m²`}
                  fromLabel={t("filters.from")}
                  toLabel={t("filters.to")}
                />
              ) : null}
              {plotAreaBounds[1] > plotAreaBounds[0] ? (
                <RangeSlider
                  label={t("investments.plotArea")}
                  min={plotAreaBounds[0]}
                  max={plotAreaBounds[1]}
                  value={plotAreaRange}
                  onChange={setPlotAreaRange}
                  formatValue={(v) => `${v} m²`}
                  fromLabel={t("filters.from")}
                  toLabel={t("filters.to")}
                />
              ) : null}
              {priceBounds[1] > priceBounds[0] ? (
                <RangeSlider
                  label={t("investments.price")}
                  min={priceBounds[0]}
                  max={priceBounds[1]}
                  step={10000}
                  value={priceRange}
                  onChange={setPriceRange}
                  formatValue={(v) => `${(v / 1000).toFixed(0)}k`}
                  fromLabel={t("filters.from")}
                  toLabel={t("filters.to")}
                />
              ) : null}
            </div>
          </div>
        ) : null}
      </div>

      {filtered.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("investments.noResults")}
        </p>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("investments.title")}</TableHead>
                <TableHead className="hidden md:table-cell">{t("investments.source")}</TableHead>
                <TableHead className="hidden md:table-cell">{t("sources.title")}</TableHead>
                <TableHead className="hidden lg:table-cell">{t("investments.houseArea")}</TableHead>
                <TableHead className="hidden lg:table-cell">{t("investments.price")}</TableHead>
                <TableHead>{t("investments.score")}</TableHead>
                <TableHead>{t("investments.firstSeen")}</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {(() => {
                const filteredIds = new Set(filtered.map((i) => i.id));
                return filtered.map((investment) => {
                  const isNew =
                    Date.now() - new Date(investment.first_seen_at).getTime() < NEW_THRESHOLD_MS;
                  const houseArea = formatArea(investment.house_area_min, investment.house_area_max, t);
                  const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max, t);
                  const price = formatPrice(investment.price_min, investment.price_max, t);
                  const isOpen = expandedId === investment.id;

                  const representativeId = representativeIdFor(investment);
                  if (representativeId !== investment.id && filteredIds.has(representativeId)) {
                    // A more authoritative duplicate of this row is also visible - suppress this row,
                    // its data still surfaces in the representative row's expanded "other sources" panel.
                    return null;
                  }

                  const links = duplicateLinks.get(investment.id) ?? [];
                  const siblings = links
                    .map((link) => ({ investment: investmentById.get(link.investmentId), confidence: link.confidence }))
                    .filter((s): s is { investment: InvestmentWithState; confidence: DuplicateLink["confidence"] } => s.investment != null);

                  return (
                    <ExpandableTableRow
                      key={investment.id}
                      isOpen={isOpen}
                      onToggle={() => setExpandedId(isOpen ? null : investment.id)}
                      columnsCount={COLUMNS_COUNT}
                      data={investment}
                      expandedExtra={
                        siblings.length > 0 ? (
                          <div className="border-b border-border px-4 py-3">
                            <h3 className="mb-2 text-xs font-medium text-muted-foreground">
                              {t("investments.otherSources")}
                            </h3>
                            <ul className="space-y-1.5">
                              {siblings.map(({ investment: sibling, confidence }) => (
                                <li key={sibling.id} className="flex items-center gap-2 text-xs">
                                  <Badge
                                    variant="outline"
                                    className={cn(
                                      "shrink-0 text-[10px]",
                                      confidence === "HIGH"
                                        ? "border-emerald-500/30 text-emerald-500 dark:text-emerald-400"
                                        : "border-amber-500/30 text-amber-500 dark:text-amber-400"
                                    )}
                                  >
                                    {tEnum("confidence", confidence)}
                                  </Badge>
                                  <span className="font-mono text-muted-foreground">{sibling.source}</span>
                                  <Link
                                    href={`/investments/${sibling.id}`}
                                    className="truncate hover:underline"
                                  >
                                    {sibling.name}
                                  </Link>
                                </li>
                              ))}
                            </ul>
                          </div>
                        ) : undefined
                      }
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
                              {investment.watched ? (
                                <Badge variant="secondary" className="border-amber-500/30 text-amber-500 dark:text-amber-400">
                                  {t("investments.watched")}
                                </Badge>
                              ) : null}
                              {investment.aggregator_only_discovery ? (
                                <Badge variant="outline" className="border-orange-500/30 text-orange-500 dark:text-orange-400">
                                  {t("investments.aggregatorOnly")}
                                </Badge>
                              ) : null}
                              {siblings.length > 0 ? (
                                <Badge variant="secondary" className="w-fit text-[10px]">
                                  {t("investments.confirmedBySources").replace("{count}", String(siblings.length + 1))}
                                </Badge>
                              ) : null}
                            </div>
                            <span className="text-xs text-muted-foreground">
                              {investment.location ?? t("investments.unknownLocation")}
                            </span>
                          </div>
                        </Link>
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        <div className="flex flex-col gap-1">
                          <span className="font-mono text-xs text-muted-foreground">{investment.source}</span>
                          {investment.source_category ? (
                            <Badge
                              variant="outline"
                              className={cn("w-fit text-[10px] uppercase", SOURCE_CATEGORY_BADGE[investment.source_category])}
                            >
                              {t(`sources.${investment.source_category.toLowerCase()}` as "sources.developer")}
                            </Badge>
                          ) : null}
                        </div>
                      </TableCell>
                      <TableCell className="hidden text-muted-foreground md:table-cell">
                        {investment.developer}
                      </TableCell>
                      <TableCell className="hidden text-muted-foreground lg:table-cell">
                        {houseArea ?? plotArea ?? "—"}
                      </TableCell>
                      <TableCell className="hidden text-muted-foreground lg:table-cell">
                        {price ?? "—"}
                      </TableCell>
                      <TableCell>
                        <ScoreBadge investment={investment} t={t} />
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatRelativeTime(investment.first_seen_at, locale)}
                      </TableCell>
                      <TableCell>
                        <ExpandChevron open={isOpen} />
                      </TableCell>
                    </ExpandableTableRow>
                  );
                });
              })()}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}

function sortValue(investment: InvestmentWithState, field: SortField): number {
  switch (field) {
    case "price_min":
      return investment.price_min ?? -Infinity;
    case "house_area_max":
      return investment.house_area_max ?? -Infinity;
    case "plot_area_max":
      return investment.plot_area_max ?? -Infinity;
    case "overall_score":
      return investment.overall_score ?? -Infinity;
    default:
      return new Date(investment.first_seen_at).getTime();
  }
}

function inRange(
  min: number | null,
  max: number | null,
  selected: [number, number],
  bounds: [number, number]
): boolean {
  // No data to compare against, or the slider hasn't been narrowed from
  // its full bounds - never exclude an investment on this dimension.
  if (min == null && max == null) return true;
  if (selected[0] <= bounds[0] && selected[1] >= bounds[1]) return true;

  const effectiveMin = min ?? max!;
  const effectiveMax = max ?? min!;
  return effectiveMax >= selected[0] && effectiveMin <= selected[1];
}
