"use client";

import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import type { ScanState } from "@/lib/scan-state";

const POLL_INTERVAL_MS = 1500;

const ScanPollContext = createContext<ScanState | null>(null);

/**
 * Single shared poll loop for `/api/scan/progress`, mounted once at the
 * app root (see `components/providers.tsx`). `ScanButton` and
 * `ScanProgress` are always mounted together in `AppSidebar` - previously
 * each called its own `useScanPoll()` and ran an independent interval,
 * doubling the request rate for no benefit since they read the exact same
 * server state. A single provider fixes that while keeping the two
 * components fully decoupled from each other (neither needs to know the
 * other exists).
 */
export function ScanPollProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<ScanState | null>(null);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      try {
        const res = await fetch("/api/scan/progress", { cache: "no-store" });
        const data = (await res.json()) as ScanState;
        if (!cancelled) setState(data);
      } catch {
        // Transient network hiccup - keep polling, next tick will retry.
      }
      if (!cancelled) {
        timer = setTimeout(poll, POLL_INTERVAL_MS);
      }
    }

    poll();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, []);

  return <ScanPollContext.Provider value={state}>{children}</ScanPollContext.Provider>;
}

/** Reads the shared scan progress state polled by `ScanPollProvider`. */
export function useScanPoll(): ScanState | null {
  return useContext(ScanPollContext);
}

/** Fires `onFinish` exactly once per transition from in-progress to finished. */
export function useScanFinishEffect(state: ScanState | null, onFinish: (state: ScanState) => void): void {
  const wasInProgress = useRef(false);

  useEffect(() => {
    if (!state) return;
    if (wasInProgress.current && !state.inProgress) {
      onFinish(state);
    }
    wasInProgress.current = state.inProgress;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state?.inProgress]);
}
