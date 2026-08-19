import { NextResponse } from "next/server";
import { execFile } from "node:child_process";
import path from "node:path";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

const REPO_ROOT = path.resolve(process.cwd(), "..");
const SCAN_TIMEOUT_MS = 120_000;

export async function POST() {
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
  }
}
