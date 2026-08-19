import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatRelativeTime(iso: string | null | undefined, locale: string): string {
  if (!iso) return "—";
  const date = new Date(iso);
  const diffMs = date.getTime() - Date.now();
  const diffMinutes = Math.round(diffMs / 60_000);

  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });

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
  t: (key: string) => string
): string | null {
  if (min == null && max == null) return null;
  if (min != null && max != null) {
    return min === max ? `${formatNumber(min)} m²` : `${formatNumber(min)}–${formatNumber(max)} m²`;
  }
  if (max != null) return `${t("investments.areaUpTo")} ${formatNumber(max)} m²`;
  return `${t("investments.areaFrom")} ${formatNumber(min!)} m²`;
}

function formatNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/\.?0+$/, "");
}
