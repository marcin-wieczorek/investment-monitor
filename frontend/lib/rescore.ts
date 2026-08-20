import { execFile } from "node:child_process";
import path from "node:path";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

const REPO_ROOT = path.resolve(process.cwd(), "..");
const RESCORE_TIMEOUT_MS = 60_000;

// Same single-process in-memory guard as /api/scan (see that route for
// the full rationale) - two concurrent bootRun processes would both
// write to the same SQLite file.
let rescoreInProgress = false;

export interface RescoreResult {
  ok: boolean;
  output?: string;
  error?: string;
}

/**
 * Recomputes `investment_score` for every existing investment against the
 * currently stored scoring preferences, without fetching any live source
 * (see `RescoreRunner`/`RescoreService` on the Kotlin side). Boots the
 * same Spring application as a normal scan but in `--investment-monitor.mode=rescore`,
 * which disables `ScanRunner` and only runs `RescoreRunner`.
 */
export async function triggerRescore(): Promise<RescoreResult> {
  if (rescoreInProgress) {
    return { ok: false, error: "A rescore is already in progress" };
  }

  rescoreInProgress = true;
  try {
    const { stdout } = await execFileAsync(
      "./gradlew",
      ["bootRun", "--args=--investment-monitor.mode=rescore", "--console=plain"],
      { cwd: REPO_ROOT, timeout: RESCORE_TIMEOUT_MS, maxBuffer: 10 * 1024 * 1024 }
    );
    return { ok: true, output: stdout };
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    const stdout = (error as { stdout?: string }).stdout ?? "";
    return { ok: false, error: message, output: stdout };
  } finally {
    rescoreInProgress = false;
  }
}
