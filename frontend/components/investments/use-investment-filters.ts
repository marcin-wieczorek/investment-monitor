"use client";

import { useMemo, useState } from "react";
import { normalizeToGmina } from "@/lib/location-groups";
import { inRange, numericBounds, sortValue, type SortField } from "@/lib/investment-filters";
import type { InvestmentWithState } from "@/lib/types";

export const ALL = "__all__";

interface UseInvestmentFiltersOptions {
  investments: InvestmentWithState[];
  initialAggregatorOnly?: boolean;
}

/**
 * Owns every piece of client-side filter/sort state for the investments
 * table (search text, dropdown filters, range sliders, sort field/direction)
 * plus the derived option lists and the final filtered+sorted result.
 * Extracted out of `InvestmentsView` so the 650-line component isn't also
 * responsible for filter state management (see AGENTS.md frontend review -
 * "God Component" finding).
 */
export function useInvestmentFilters({ investments, initialAggregatorOnly = false }: UseInvestmentFiltersOptions) {
  const [search, setSearch] = useState("");
  const [developer, setDeveloper] = useState<string>(ALL);
  const [showArchived, setShowArchived] = useState(false);
  const [showWatchedOnly, setShowWatchedOnly] = useState(false);
  const [showAggregatorOnly, setShowAggregatorOnly] = useState(initialAggregatorOnly);
  const [sortField, setSortField] = useState<SortField>("first_seen_at");
  const [sortDesc, setSortDesc] = useState(true);

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
    () =>
      Array.from(
        new Set(investments.map((i) => normalizeToGmina(i.location)).filter((v): v is string => v != null))
      ).sort(),
    [investments]
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
      if (location !== ALL && normalizeToGmina(investment.location) !== location) return false;
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

  return {
    // simple filters
    search,
    setSearch,
    developer,
    setDeveloper,
    showArchived,
    setShowArchived,
    showWatchedOnly,
    setShowWatchedOnly,
    showAggregatorOnly,
    setShowAggregatorOnly,
    sourceCategory,
    setSourceCategory,
    propertyType,
    setPropertyType,
    status,
    setStatus,
    location,
    setLocation,
    // sort
    sortField,
    setSortField,
    sortDesc,
    setSortDesc,
    // option lists
    developers,
    propertyTypes,
    statuses,
    locations,
    // ranges
    houseAreaBounds,
    houseAreaRange,
    setHouseAreaRange,
    plotAreaBounds,
    plotAreaRange,
    setPlotAreaRange,
    priceBounds,
    priceRange,
    setPriceRange,
    // result
    filtered,
  };
}

export type InvestmentFiltersState = ReturnType<typeof useInvestmentFilters>;
