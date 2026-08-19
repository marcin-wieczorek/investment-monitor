"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, PlayCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useI18n } from "@/lib/i18n";

export function ScanButton() {
  const { t } = useI18n();
  const router = useRouter();
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function runScan() {
    setIsScanning(true);
    setError(null);
    try {
      const response = await fetch("/api/scan", { method: "POST" });
      const data = await response.json();
      if (!response.ok || !data.ok) {
        throw new Error(data.error ?? "Scan failed");
      }
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsScanning(false);
    }
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <Button onClick={runScan} disabled={isScanning}>
        {isScanning ? (
          <Loader2 className="size-4 animate-spin" />
        ) : (
          <PlayCircle className="size-4" />
        )}
        {isScanning ? t("scan.scanning") : t("scan.runScan")}
      </Button>
      {error ? <p className="text-xs text-rose-500">{error}</p> : null}
    </div>
  );
}
