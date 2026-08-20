"use client";

import { useRouter } from "next/navigation";
import { ChevronDown, Loader2, PlayCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";
import { useScanFinishEffect, useScanPoll } from "@/lib/use-scan-poll";

interface ScanButtonProps {
  size?: "sm" | "default" | "icon";
  className?: string;
  iconOnly?: boolean;
}

export function ScanButton({ size = "default", className, iconOnly = false }: ScanButtonProps) {
  const { t } = useI18n();
  const router = useRouter();
  const state = useScanPoll();
  const isScanning = state?.inProgress ?? false;

  // The scan runs as a detached process tracked entirely through polled
  // server state (see lib/use-scan-poll.ts) - this component only needs to
  // know when a run it doesn't necessarily even own transitions to done, so
  // the dashboard picks up fresh data without the button blocking the page.
  useScanFinishEffect(state, (finished) => {
    if (finished.ok) {
      router.refresh();
    }
  });

  async function runScan() {
    if (isScanning) return;
    try {
      await fetch("/api/scan", { method: "POST" });
    } catch {
      // Surfaced on the next progress poll instead (state.error / state.ok).
    }
  }

  const hasResult = !isScanning && state?.phase === "done" && state.ok !== null;

  if (iconOnly) {
    return (
      <Button
        onClick={runScan}
        disabled={isScanning}
        size="icon"
        className={className}
        aria-label={t("scan.runScan")}
        title={t("scan.runScan")}
      >
        {isScanning ? (
          <Loader2 className="size-4 animate-spin" />
        ) : (
          <PlayCircle className="size-4" />
        )}
      </Button>
    );
  }

  return (
    <div className={cn("flex items-center gap-1", className)}>
      <Button onClick={runScan} disabled={isScanning} size={size} className="flex-1">
        {isScanning ? (
          <Loader2 className="size-4 animate-spin" />
        ) : (
          <PlayCircle className="size-4" />
        )}
        {isScanning ? t("scan.scanning") : t("scan.runScan")}
      </Button>

      {hasResult ? (
        <Dialog>
          <DialogTrigger
            render={
              <Button
                variant="ghost"
                size={size === "sm" ? "icon-sm" : "icon"}
                aria-label={t("scan.viewDetails")}
              >
                <ChevronDown className={state.ok ? "text-emerald-500" : "text-rose-500"} />
              </Button>
            }
          />
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>{state.ok ? t("scan.scanComplete") : t("scan.scanFailed")}</DialogTitle>
            </DialogHeader>
            <pre className="max-h-[60vh] overflow-auto rounded-md bg-muted p-4 text-xs">
              {JSON.stringify({ ok: state.ok, output: state.output, error: state.error }, null, 2)}
            </pre>
          </DialogContent>
        </Dialog>
      ) : null}
    </div>
  );
}
