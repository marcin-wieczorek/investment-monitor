"use client";

import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import { useScanPoll } from "@/lib/use-scan-poll";

interface ScanProgressProps {
  /** Compact rendering for the collapsed sidebar (icon-only width, no text). */
  collapsed?: boolean;
}

/**
 * Independent scan progress indicator - polls its own state (see
 * lib/use-scan-poll.ts) and renders regardless of what triggered the scan
 * or whether ScanButton is even mounted. Never blocks the page: no overlay,
 * no disabled state on anything outside itself.
 */
export function ScanProgress({ collapsed = false }: ScanProgressProps) {
  const { t } = useI18n();
  const state = useScanPoll();

  if (!state || state.phase === "idle") return null;

  if (state.inProgress) {
    const hasTotal = state.total > 0;
    const percent = hasTotal ? Math.round((state.current / state.total) * 100) : 8;

    return (
      <div className={cn("mt-2 space-y-1", collapsed && "flex flex-col items-center")}>
        {!collapsed ? (
          <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
            <span className="truncate">{state.currentSource ?? t("scan.starting")}</span>
            {hasTotal ? (
              <span className="shrink-0 tabular-nums">
                {t("scan.progressLabel").replace("{current}", String(state.current)).replace("{total}", String(state.total))}
              </span>
            ) : null}
          </div>
        ) : null}
        <div
          className={cn(
            "h-1.5 overflow-hidden rounded-full bg-muted",
            collapsed ? "w-8" : "w-full"
          )}
          role="progressbar"
          aria-valuenow={hasTotal ? percent : undefined}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={t("scan.scanning")}
        >
          <div
            className={cn("h-full rounded-full bg-primary", hasTotal && "transition-all duration-300")}
            style={{ width: `${percent}%` }}
          />
        </div>
      </div>
    );
  }

  if (collapsed || state.phase !== "done" || state.ok === null) return null;

  return (
    <div className={cn("mt-2 text-xs", state.ok ? "text-emerald-500" : "text-rose-500")}>
      {state.ok ? t("scan.scanComplete") : t("scan.scanFailed")}
    </div>
  );
}
