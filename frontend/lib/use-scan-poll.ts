"use client";

import { useEffect, useRef, useState } from "react";
import type { ScanState } from "@/lib/scan-state";

const POLL_INTERVAL_MS = 1500;

/**
 * Polls the scan progress endpoint on its own independent interval.
 * Multiple components can call this hook simultaneously (each runs its own
 * poll loop) - deliberately simple for a single-user, localhost-only tool,
 * and keeps the scan button and the progress indicator fully decoupled:
 * neither needs to know the other exists, and either can be removed
 * without breaking the other.
 */
export function useScanPoll(): ScanState | null {
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

  return state;
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
