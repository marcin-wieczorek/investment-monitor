import { NextResponse } from "next/server";
import { spawn } from "node:child_process";
import path from "node:path";
import { scanState, resetScanState } from "@/lib/scan-state";

const REPO_ROOT = path.resolve(process.cwd(), "..");
const SCAN_TIMEOUT_MS = 120_000;

// Matches the "Scanning source [n/total]: 'sourceId'" line added in
// MonitoringService.scan() specifically so the frontend can track progress
// without guessing from unrelated log output.
const PROGRESS_LINE = /Scanning source \[(\d+)\/(\d+)\]: '([^']+)'/;
const SCAN_FINISHED_LINE = /Scan finished: status=(\S+)/;

export async function POST() {
  if (scanState.inProgress) {
    return NextResponse.json({ ok: false, error: "A scan is already in progress" }, { status: 409 });
  }

  resetScanState();
  scanState.inProgress = true;
  scanState.phase = "starting";

  const child = spawn("./gradlew", ["bootRun", "--console=plain"], {
    cwd: REPO_ROOT,
  });

  let output = "";
  let stderrOutput = "";

  const timeout = setTimeout(() => {
    child.kill();
  }, SCAN_TIMEOUT_MS);

  child.stdout?.on("data", (chunk: Buffer) => {
    const text = chunk.toString();
    output += text;
    for (const line of text.split("\n")) {
      const progressMatch = PROGRESS_LINE.exec(line);
      if (progressMatch) {
        scanState.phase = "scanning";
        scanState.current = Number(progressMatch[1]);
        scanState.total = Number(progressMatch[2]);
        scanState.currentSource = progressMatch[3];
        continue;
      }
      if (SCAN_FINISHED_LINE.test(line)) {
        scanState.phase = "done";
      }
    }
  });

  child.stderr?.on("data", (chunk: Buffer) => {
    stderrOutput += chunk.toString();
  });

  child.on("close", (code) => {
    clearTimeout(timeout);
    scanState.inProgress = false;
    scanState.phase = "done";
    scanState.ok = code === 0;
    scanState.output = output;
    scanState.error = code === 0 ? null : stderrOutput || `Process exited with code ${code}`;
  });

  child.on("error", (error) => {
    clearTimeout(timeout);
    scanState.inProgress = false;
    scanState.phase = "done";
    scanState.ok = false;
    scanState.output = output;
    scanState.error = error.message;
  });

  return NextResponse.json({ started: true }, { status: 202 });
}
