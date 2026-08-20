"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp, ChevronDown, ChevronUp, Search, SlidersHorizontal } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { RangeSlider } from "@/components/range-slider";
import { ALL, type InvestmentFiltersState } from "@/components/investments/use-investment-filters";

interface InvestmentFiltersBarProps {
  filters: InvestmentFiltersState;
  visibleCount: number;
  totalCount: number;
}

/**
 * All filter/sort UI for the investments table: search box, developer
 * select, archived/watched toggles, sort controls, and the collapsible
 * "advanced filters" panel (source category, property type, status,
 * location, and the three range sliders). Purely presentational - all
 * state lives in `useInvestmentFilters` and is passed in as `filters`.
 */
export function InvestmentFiltersBar({ filters, visibleCount, totalCount }: InvestmentFiltersBarProps) {
  const { t, tEnum } = useI18n();
  const [filtersOpen, setFiltersOpen] = useState(false);

  return (
    <div className="space-y-3 rounded-xl border border-border bg-card p-3">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative max-w-xs flex-1 min-w-[180px]">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t("investments.searchLocation")}
            value={filters.search}
            onChange={(e) => filters.setSearch(e.target.value)}
            className="pl-8"
          />
        </div>
        <Select value={filters.developer} onValueChange={(value) => filters.setDeveloper(value ?? ALL)}>
          <SelectTrigger className="w-48">
            <SelectValue>{(value: string) => (value === ALL ? t("investments.allDevelopers") : value)}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>{t("investments.allDevelopers")}</SelectItem>
            {filters.developers.map((dev) => (
              <SelectItem key={dev} value={dev}>
                {dev}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Switch checked={filters.showArchived} onCheckedChange={filters.setShowArchived} />
          {t("investments.showArchived")}
        </label>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Switch checked={filters.showWatchedOnly} onCheckedChange={filters.setShowWatchedOnly} />
          {t("investments.watchedOnly")}
        </label>

        <div className="flex items-center gap-1.5">
          <span className="text-xs text-muted-foreground">{t("filters.sortBy")}</span>
          <Select
            value={filters.sortField}
            onValueChange={(value) => filters.setSortField((value as typeof filters.sortField) ?? "first_seen_at")}
          >
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
            onClick={() => filters.setSortDesc((prev) => !prev)}
            title={filters.sortDesc ? t("filters.sortDescending") : t("filters.sortAscending")}
            aria-label={filters.sortDesc ? t("filters.sortDescending") : t("filters.sortAscending")}
          >
            {filters.sortDesc ? <ArrowDown className="size-3.5" /> : <ArrowUp className="size-3.5" />}
          </Button>
        </div>

        <Button variant="outline" size="sm" className="ml-auto" onClick={() => setFiltersOpen((prev) => !prev)}>
          <SlidersHorizontal className="size-3.5" />
          {t("filters.advanced")}
          {filtersOpen ? <ChevronUp className="size-3.5" /> : <ChevronDown className="size-3.5" />}
        </Button>
        <span className="text-xs text-muted-foreground">
          {visibleCount} / {totalCount}
        </span>
      </div>

      {filtersOpen ? (
        <div className="space-y-4 border-t border-border pt-3">
          <div className="flex flex-wrap items-center gap-3">
            <Select value={filters.sourceCategory} onValueChange={(value) => filters.setSourceCategory(value ?? ALL)}>
              <SelectTrigger className="w-40">
                <SelectValue>
                  {(value: string) =>
                    value === ALL ? t("investments.allSources") : t(`sources.${value.toLowerCase()}` as "sources.developer")
                  }
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>{t("investments.allSources")}</SelectItem>
                <SelectItem value="DEVELOPER">{t("sources.developer")}</SelectItem>
                <SelectItem value="DISCOVERY">{t("sources.discovery")}</SelectItem>
                <SelectItem value="AGGREGATOR">{t("sources.aggregator")}</SelectItem>
              </SelectContent>
            </Select>

            <label className="flex items-center gap-2 text-sm text-muted-foreground" title={t("investments.aggregatorOnlyTooltip")}>
              <Switch checked={filters.showAggregatorOnly} onCheckedChange={filters.setShowAggregatorOnly} />
              {t("investments.aggregatorOnly")}
            </label>

            <Select value={filters.propertyType} onValueChange={(value) => filters.setPropertyType(value ?? ALL)}>
              <SelectTrigger className="w-44">
                <SelectValue>
                  {(value: string) => (value === ALL ? t("investments.allTypes") : tEnum("propertyType", value))}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>{t("investments.allTypes")}</SelectItem>
                {filters.propertyTypes.map((pt) => (
                  <SelectItem key={pt} value={pt}>
                    {tEnum("propertyType", pt)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select value={filters.status} onValueChange={(value) => filters.setStatus(value ?? ALL)}>
              <SelectTrigger className="w-44">
                <SelectValue>
                  {(value: string) => (value === ALL ? t("investments.allStatuses") : tEnum("investmentStatus", value))}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>{t("investments.allStatuses")}</SelectItem>
                {filters.statuses.map((s) => (
                  <SelectItem key={s} value={s}>
                    {tEnum("investmentStatus", s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select value={filters.location} onValueChange={(value) => filters.setLocation(value ?? ALL)}>
              <SelectTrigger className="w-44">
                <SelectValue>{(value: string) => (value === ALL ? t("investments.allLocations") : value)}</SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>{t("investments.allLocations")}</SelectItem>
                {filters.locations.map((loc) => (
                  <SelectItem key={loc} value={loc}>
                    {loc}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-wrap gap-6">
            {filters.houseAreaBounds[1] > filters.houseAreaBounds[0] ? (
              <RangeSlider
                label={t("investments.houseArea")}
                min={filters.houseAreaBounds[0]}
                max={filters.houseAreaBounds[1]}
                value={filters.houseAreaRange}
                onChange={filters.setHouseAreaRange}
                formatValue={(v) => `${v} m²`}
                fromLabel={t("filters.from")}
                toLabel={t("filters.to")}
              />
            ) : null}
            {filters.plotAreaBounds[1] > filters.plotAreaBounds[0] ? (
              <RangeSlider
                label={t("investments.plotArea")}
                min={filters.plotAreaBounds[0]}
                max={filters.plotAreaBounds[1]}
                value={filters.plotAreaRange}
                onChange={filters.setPlotAreaRange}
                formatValue={(v) => `${v} m²`}
                fromLabel={t("filters.from")}
                toLabel={t("filters.to")}
              />
            ) : null}
            {filters.priceBounds[1] > filters.priceBounds[0] ? (
              <RangeSlider
                label={t("investments.price")}
                min={filters.priceBounds[0]}
                max={filters.priceBounds[1]}
                step={10000}
                value={filters.priceRange}
                onChange={filters.setPriceRange}
                formatValue={(v) => `${(v / 1000).toFixed(0)}k`}
                fromLabel={t("filters.from")}
                toLabel={t("filters.to")}
              />
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
