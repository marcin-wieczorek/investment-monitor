/**
 * Server-side, in-memory scan progress state shared between the two scan
 * API routes (`app/api/scan/route.ts` starts the scan and updates this
 * object as it parses `bootRun` stdout, `app/api/scan/progress/route.ts`
 * reads it for the client to poll). A plain module-level singleton is
 * enough here: this is a single-user, localhost-only tool running in a
 * single Next.js server process (same rationale as the `scanInProgress`
 * guard this replaces - see git history).
 */
export interface ScanState {
  /** True from the moment a scan is accepted until it finishes (success or failure). */
  inProgress: boolean;
  /** 1-based index of the source currently being scanned, 0 before the first one starts. */
  current: number;
  /** Total number of sources this scan will process (developer + discovery + aggregator). */
  total: number;
  /** id of the source currently being scanned, if known. */
  currentSource: string | null;
  phase: "idle" | "starting" | "scanning" | "done";
  /** Null while in progress; set once the scan finishes. */
  ok: boolean | null;
  error: string | null;
  /** Full captured stdout, surfaced once the scan finishes (same shape the UI already expects). */
  output: string;
}

export const initialScanState: ScanState = {
  inProgress: false,
  current: 0,
  total: 0,
  currentSource: null,
  phase: "idle",
  ok: null,
  error: null,
  output: "",
};

// Module-level singleton - mutated in place by the POST route, read by the GET route.
export const scanState: ScanState = { ...initialScanState };

export function resetScanState(): void {
  Object.assign(scanState, initialScanState);
}
