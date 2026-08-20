"use client";

import { useEffect, useMemo, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useI18n } from "@/lib/i18n";
import { useInvestmentFilters } from "@/components/investments/use-investment-filters";
import { useDuplicateGrouping } from "@/components/investments/use-duplicate-grouping";
import { InvestmentFiltersBar } from "@/components/investments/investment-filters-bar";
import { InvestmentTable } from "@/components/investments/investment-table";
import type { InvestmentDuplicateRow, InvestmentWithState } from "@/lib/types";

interface InvestmentsViewProps {
  investments: InvestmentWithState[];
  duplicates?: InvestmentDuplicateRow[];
  /** True if the server-side query hit `DEFAULT_INVESTMENT_LIMIT` (see lib/queries.ts) and older rows were omitted. */
  possiblyTruncated?: boolean;
}

/**
 * Top-level orchestrator for the investments table page. Composition root
 * only: filter/sort state lives in `useInvestmentFilters`, duplicate
 * grouping lives in `useDuplicateGrouping`, and rendering is split between
 * `InvestmentFiltersBar` and `InvestmentTable`. Previously a single
 * ~650-line component - see AGENTS.md frontend review ("God Component"
 * finding) for the rationale behind this split.
 */
export function InvestmentsView({ investments, duplicates = [], possiblyTruncated = false }: InvestmentsViewProps) {
  const { t } = useI18n();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const initialAggregatorOnly = searchParams.get("aggregatorOnly") === "1";

  const filters = useInvestmentFilters({ investments, initialAggregatorOnly });
  const { representativeIdFor, siblingsFor } = useDuplicateGrouping(investments, duplicates);

  // Keep `?aggregatorOnly=1` in sync with the toggle so the dashboard's
  // "Aggregator-only discoveries" deep link (the only place this flag is
  // ever linked to) round-trips correctly: toggling it off here should
  // also drop it from the URL, not just from local state. Skips the
  // initial mount so we don't rewrite a URL that already matches.
  const isFirstRender = useRef(true);
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    const params = new URLSearchParams(searchParams.toString());
    if (filters.showAggregatorOnly) {
      params.set("aggregatorOnly", "1");
    } else {
      params.delete("aggregatorOnly");
    }
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.showAggregatorOnly]);

  const visibleCount = useMemo(() => {
    const filteredIds = new Set(filters.filtered.map((i) => i.id));
    return filters.filtered.filter((investment) => {
      const representativeId = representativeIdFor(investment);
      return representativeId === investment.id || !filteredIds.has(representativeId);
    }).length;
  }, [filters.filtered, representativeIdFor]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("investments.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("investments.subtitle")}</p>
        {possiblyTruncated ? (
          <p className="mt-1 text-xs text-amber-500 dark:text-amber-400">
            {t("investments.truncatedNotice").replace("{count}", String(investments.length))}
          </p>
        ) : null}
      </div>

      <InvestmentFiltersBar filters={filters} visibleCount={visibleCount} totalCount={investments.length} />

      <InvestmentTable
        investments={filters.filtered}
        representativeIdFor={representativeIdFor}
        siblingsFor={siblingsFor}
      />
    </div>
  );
}
