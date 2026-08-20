import type { InvestmentWithState } from "@/lib/types";

export type SortField = "first_seen_at" | "price_min" | "house_area_max" | "plot_area_max" | "overall_score";

export function sortValue(investment: InvestmentWithState, field: SortField): number {
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

export function numericBounds(values: Array<number | null>): [number, number] {
  const known = values.filter((v): v is number => v != null);
  if (known.length === 0) return [0, 0];
  return [Math.floor(Math.min(...known)), Math.ceil(Math.max(...known))];
}

export function inRange(
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
