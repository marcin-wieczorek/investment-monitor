"use client";

import { useState } from "react";
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

interface ScanResult {
  ok: boolean;
  output?: string;
  error?: string;
}

interface ScanButtonProps {
  size?: "sm" | "default" | "icon";
  className?: string;
  iconOnly?: boolean;
}

export function ScanButton({ size = "default", className, iconOnly = false }: ScanButtonProps) {
  const { t } = useI18n();
  const router = useRouter();
  const [isScanning, setIsScanning] = useState(false);
  const [lastResult, setLastResult] = useState<ScanResult | null>(null);

  async function runScan() {
    setIsScanning(true);
    try {
      const response = await fetch("/api/scan", { method: "POST" });
      const data = (await response.json()) as ScanResult;
      setLastResult(data);
      if (data.ok) {
        router.refresh();
      }
    } catch (err) {
      setLastResult({ ok: false, error: err instanceof Error ? err.message : String(err) });
    } finally {
      setIsScanning(false);
    }
  }

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

      {lastResult ? (
        <Dialog>
          <DialogTrigger
            render={
              <Button
                variant="ghost"
                size={size === "sm" ? "icon-sm" : "icon"}
                aria-label={t("scan.viewDetails")}
              >
                <ChevronDown
                  className={lastResult.ok ? "text-emerald-500" : "text-rose-500"}
                />
              </Button>
            }
          />
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>
                {lastResult.ok ? t("scan.scanComplete") : t("scan.scanFailed")}
              </DialogTitle>
            </DialogHeader>
            <pre className="max-h-[60vh] overflow-auto rounded-md bg-muted p-4 text-xs">
              {JSON.stringify(lastResult, null, 2)}
            </pre>
          </DialogContent>
        </Dialog>
      ) : null}
    </div>
  );
}
