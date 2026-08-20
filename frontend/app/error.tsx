"use client";

import { useEffect } from "react";
import { AlertTriangle, RotateCcw, LayoutDashboard } from "lucide-react";
import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { Button } from "@/components/ui/button";

/**
 * Segment-level error boundary (Next.js App Router convention). Catches
 * render/data-fetch errors thrown by any page under this layout (e.g. a
 * `node:sqlite` failure in `lib/queries.ts` if `investment-monitor.db` is
 * missing, locked, or has an unexpected schema) so the user sees a
 * recoverable screen instead of a raw stack trace. Rendered *inside*
 * `AppShell`/`Providers`, so `useI18n()` is safe to use here - unlike
 * `global-error.tsx`, which replaces the entire root layout.
 */
export default function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const { t } = useI18n();

  useEffect(() => {
    // Errors caught here never reach the server console otherwise - log
    // them so a failure is at least visible when running `next dev`/`start`.
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-6 text-center">
      <AlertTriangle className="size-10 text-destructive" />
      <div className="space-y-1">
        <h1 className="text-xl font-semibold tracking-tight">{t("error.title")}</h1>
        <p className="max-w-md text-sm text-muted-foreground">{t("error.description")}</p>
      </div>

      <div className="flex items-center gap-2">
        <Button onClick={reset}>
          <RotateCcw className="size-4" />
          {t("error.retry")}
        </Button>
        <Button variant="outline" render={<Link href="/" />}>
          <LayoutDashboard className="size-4" />
          {t("error.goHome")}
        </Button>
      </div>

      {process.env.NODE_ENV === "development" ? (
        <details className="mt-2 w-full max-w-xl rounded-md border border-border bg-muted/30 p-3 text-left text-xs text-muted-foreground">
          <summary className="cursor-pointer font-medium">{t("error.detailsLabel")}</summary>
          <pre className="mt-2 overflow-auto whitespace-pre-wrap">{error.stack ?? error.message}</pre>
        </details>
      ) : null}
    </div>
  );
}
