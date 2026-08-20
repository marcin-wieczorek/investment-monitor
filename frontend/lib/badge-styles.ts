/**
 * Centralized Tailwind class mappings for domain enum -> badge color.
 * Previously duplicated across investments-view.tsx, investment-detail-view.tsx,
 * correlations-view.tsx, and map-view.tsx - kept in one place so a color
 * change (or a new confidence/category value) only needs to happen once.
 *
 * Class strings are written out in full (not built via template literals)
 * because Tailwind's JIT compiler only picks up statically-analyzable
 * class names.
 */

export type Confidence = "HIGH" | "MEDIUM" | "LOW";
export type SourceCategory = "DEVELOPER" | "DISCOVERY" | "AGGREGATOR";

/** Badge `border`/`text` classes for a HIGH/MEDIUM/LOW confidence value (duplicate/correlation matches). */
export const CONFIDENCE_BADGE_CLASS: Record<Confidence, string> = {
  HIGH: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  MEDIUM: "border-amber-500/30 text-amber-500 dark:text-amber-400",
  LOW: "border-border text-muted-foreground",
};

/** Badge `border`/`text` classes for a source category (DEVELOPER/DISCOVERY/AGGREGATOR). */
export const SOURCE_CATEGORY_BADGE_CLASS: Record<SourceCategory, string> = {
  DEVELOPER: "border-blue-500/30 text-blue-500 dark:text-blue-400",
  DISCOVERY: "border-purple-500/30 text-purple-500 dark:text-purple-400",
  AGGREGATOR: "border-orange-500/30 text-orange-500 dark:text-orange-400",
};

/** Solid `bg-*` classes for a source category (map legend/marker dots). */
export const SOURCE_CATEGORY_DOT_CLASS: Record<SourceCategory, string> = {
  DEVELOPER: "bg-blue-500",
  DISCOVERY: "bg-purple-500",
  AGGREGATOR: "bg-orange-500",
};

export type DevelopmentTrend = "ACCELERATING" | "STABLE" | "SLOWING" | "MINIMAL";
export type RecommendedAction = "WATCH_CLOSELY" | "MONITOR" | "LOW_PRIORITY";
export type ActivityLevel = "HIGH" | "MEDIUM" | "LOW";

/** Badge `border`/`text` classes for a location's development trend (see `/locations`). */
export const TREND_BADGE_CLASS: Record<DevelopmentTrend, string> = {
  ACCELERATING: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  STABLE: "border-blue-500/30 text-blue-500 dark:text-blue-400",
  SLOWING: "border-amber-500/30 text-amber-500 dark:text-amber-400",
  MINIMAL: "border-border text-muted-foreground",
};

/** Badge `border`/`text` classes for a recommended-action label (see `/locations`). */
export const ACTION_BADGE_CLASS: Record<RecommendedAction, string> = {
  WATCH_CLOSELY: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  MONITOR: "border-blue-500/30 text-blue-500 dark:text-blue-400",
  LOW_PRIORITY: "border-border text-muted-foreground",
};

/** Badge `border`/`text` classes for HIGH/MEDIUM/LOW activity or relevance levels (see `/locations`). */
export const ACTIVITY_LEVEL_BADGE_CLASS: Record<ActivityLevel, string> = {
  HIGH: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  MEDIUM: "border-amber-500/30 text-amber-500 dark:text-amber-400",
  LOW: "border-border text-muted-foreground",
};

/** Solid `bg-*` classes for HIGH/MEDIUM/LOW activity level (map heat indicator dots). */
export const ACTIVITY_LEVEL_DOT_CLASS: Record<ActivityLevel, string> = {
  HIGH: "bg-emerald-500",
  MEDIUM: "bg-amber-500",
  LOW: "bg-muted-foreground",
};
