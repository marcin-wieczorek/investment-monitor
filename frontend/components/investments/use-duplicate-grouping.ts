"use client";

import { useMemo } from "react";
import { buildDuplicateLinks, isMoreAuthoritative, type DuplicateLink } from "@/lib/investment-duplicates";
import type { InvestmentDuplicateRow, InvestmentWithState } from "@/lib/types";

export type { DuplicateLink };

/**
 * Cross-source duplicate grouping for the investments table: which
 * investments are known duplicates of each other, and which single row
 * should represent the whole group (the "representative"). Extracted out
 * of `InvestmentsView` alongside `useInvestmentFilters` (see AGENTS.md
 * frontend review - "God Component" finding).
 */
export function useDuplicateGrouping(investments: InvestmentWithState[], duplicates: InvestmentDuplicateRow[]) {
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

  function siblingsFor(investment: InvestmentWithState): Array<{ investment: InvestmentWithState; confidence: DuplicateLink["confidence"] }> {
    const links = duplicateLinks.get(investment.id) ?? [];
    return links
      .map((link) => ({ investment: investmentById.get(link.investmentId), confidence: link.confidence }))
      .filter(
        (s): s is { investment: InvestmentWithState; confidence: DuplicateLink["confidence"] } => s.investment != null
      );
  }

  return { investmentById, duplicateLinks, representativeIdFor, siblingsFor };
}
