import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import type { MessageKey } from "@/lib/i18n"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// Intl.RelativeTimeFormat construction is non-trivial - cache one instance
// per locale instead of allocating a new one on every call (tables can call
// this hundreds of times per render).
const relativeTimeFormatters = new Map<string, Intl.RelativeTimeFormat>();

function getRelativeTimeFormatter(locale: string): Intl.RelativeTimeFormat {
  let rtf = relativeTimeFormatters.get(locale);
  if (!rtf) {
    rtf = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });
    relativeTimeFormatters.set(locale, rtf);
  }
  return rtf;
}

export function formatRelativeTime(iso: string | null | undefined, locale: string): string {
  if (!iso) return "—";
  const date = new Date(iso);
  const diffMs = date.getTime() - Date.now();
  const diffMinutes = Math.round(diffMs / 60_000);

  const rtf = getRelativeTimeFormatter(locale);

  const absMinutes = Math.abs(diffMinutes);
  if (absMinutes < 60) return rtf.format(diffMinutes, "minute");

  const diffHours = Math.round(diffMinutes / 60);
  if (Math.abs(diffHours) < 24) return rtf.format(diffHours, "hour");

  const diffDays = Math.round(diffHours / 24);
  return rtf.format(diffDays, "day");
}

export function formatArea(
  min: number | null,
  max: number | null,
  t: (key: MessageKey) => string,
  locale = "pl-PL"
): string | null {
  if (min == null && max == null) return null;
  if (min != null && max != null) {
    return min === max
      ? `${formatNumber(min, locale)} m²`
      : `${formatNumber(min, locale)}–${formatNumber(max, locale)} m²`;
  }
  if (max != null) return `${t("investments.rangeUpTo")} ${formatNumber(max, locale)} m²`;
  return `${t("investments.rangeFrom")} ${formatNumber(min!, locale)} m²`;
}

export function formatPrice(
  min: number | null,
  max: number | null,
  t: (key: MessageKey) => string,
  locale = "pl-PL"
): string | null {
  if (min == null && max == null) return null;
  if (min != null && max != null) {
    return min === max
      ? `${formatNumber(min, locale)} zł`
      : `${formatNumber(min, locale)}–${formatNumber(max, locale)} zł`;
  }
  if (max != null) return `${t("investments.rangeUpTo")} ${formatNumber(max, locale)} zł`;
  return `${t("investments.rangeFrom")} ${formatNumber(min!, locale)} zł`;
}

/**
 * Formats a number for display. Defaults to `pl-PL` grouping (the domain
 * is Polish real estate - prices are in zł regardless of UI language) but
 * accepts an explicit locale so callers with a known UI locale can opt in.
 */
function formatNumber(value: number, locale = "pl-PL"): string {
  return Number.isInteger(value) ? value.toLocaleString(locale) : value.toFixed(2).replace(/\.?0+$/, "");
}

/** The 6 fields whose presence determines how trustworthy `overall_score` actually is (see DeterministicScorer). */
const COMPLETENESS_FIELD_COUNT = 6;

interface CompletenessFields {
  property_type: string | null;
  house_area_min: number | null;
  house_area_max: number | null;
  plot_area_min: number | null;
  plot_area_max: number | null;
  price_min: number | null;
  price_max: number | null;
  units: number | null;
  status: string | null;
}

/**
 * Fraction (0-1) of the 6 key facts (property type, house area, plot area,
 * price, unit count, sale status) actually published for this investment.
 *
 * Most developer list pages publish only name/location/image - a score
 * computed from 1 present field out of 5 weighted components looks like a
 * confident percentage but really only reflects a single dimension (see
 * AGENTS.md scoring completeness gap / docs/DEEP-ANALYSIS.md). The
 * frontend uses this to decide when NOT to show `overall_score` as a
 * trustworthy percentage.
 */
export function dataCompleteness(investment: CompletenessFields): number {
  let present = 0;
  if (investment.property_type != null) present++;
  if (investment.house_area_min != null || investment.house_area_max != null) present++;
  if (investment.plot_area_min != null || investment.plot_area_max != null) present++;
  if (investment.price_min != null || investment.price_max != null) present++;
  if (investment.units != null) present++;
  if (investment.status != null) present++;
  return present / COMPLETENESS_FIELD_COUNT;
}

/** Below this fraction, `overall_score` is computed from too little data to display as a trustworthy percentage. */
export const LOW_COMPLETENESS_THRESHOLD = 2 / COMPLETENESS_FIELD_COUNT;
