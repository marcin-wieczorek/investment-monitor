import type { InvestmentWithState } from "@/lib/types";

/** Lower = more authoritative (see SourceCategory.kt: DEVELOPER > DISCOVERY > AGGREGATOR). */
const CATEGORY_PRIORITY: Record<string, number> = { DEVELOPER: 0, DISCOVERY: 1, AGGREGATOR: 2 };

function categoryPriority(investment: InvestmentWithState): number {
  return investment.source_category ? CATEGORY_PRIORITY[investment.source_category] ?? 3 : 3;
}

/** Deterministic tie-break for picking which side of a duplicate pair is shown as the representative row. */
export function isMoreAuthoritative(a: InvestmentWithState, b: InvestmentWithState): boolean {
  const pa = categoryPriority(a);
  const pb = categoryPriority(b);
  if (pa !== pb) return pa < pb;
  if (a.first_seen_at !== b.first_seen_at) return a.first_seen_at < b.first_seen_at;
  return a.id < b.id;
}

export interface DuplicateLink {
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
export function buildDuplicateLinks(
  duplicates: Array<{ investment_id_a: number; investment_id_b: number; confidence: DuplicateLink["confidence"] }>
): Map<number, DuplicateLink[]> {
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
