import { NextResponse } from "next/server";
import { execFile } from "node:child_process";
import path from "node:path";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

const REPO_ROOT = path.resolve(process.cwd(), "..");
const SCAN_TIMEOUT_MS = 120_000;

// Module-level guard: two concurrent `bootRun` processes would both write to
// the same SQLite file, risking SQLITE_BUSY errors or interleaved writes.
// Since this route only ever runs within a single Next.js server process,
// a simple in-memory flag is enough (no separate lock service needed for a
// single-user, localhost-only tool).
let scanInProgress = false;

export async function POST() {
  if (scanInProgress) {
    return NextResponse.json({ ok: false, error: "A scan is already in progress" }, { status: 409 });
  }

  scanInProgress = true;
  try {
    const { stdout } = await execFileAsync("./gradlew", ["bootRun", "--console=plain"], {
      cwd: REPO_ROOT,
      timeout: SCAN_TIMEOUT_MS,
      maxBuffer: 10 * 1024 * 1024,
    });

    return NextResponse.json({ ok: true, output: stdout });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    const stdout = (error as { stdout?: string }).stdout ?? "";
    return NextResponse.json({ ok: false, error: message, output: stdout }, { status: 500 });
  } finally {
    scanInProgress = false;
  }
}
