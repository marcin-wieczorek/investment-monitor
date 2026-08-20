#!/usr/bin/env node
/**
 * Single-command launcher for the whole app: starts the Next.js frontend
 * dev server and, once it's actually serving requests, triggers a scan via
 * POST /api/scan (the same non-blocking endpoint the sidebar's "Run scan"
 * button uses - see frontend/app/api/scan/route.ts) so the scan progress
 * bar starts moving immediately without a second manual step.
 *
 * Usage: npm start (from the repo root)
 *
 * Deliberately dependency-free (no `concurrently`, no root node_modules) -
 * this project already requires Node >=22.5.0 for `node:sqlite`, and that
 * version has everything needed here: top-level await, global fetch,
 * child_process.
 */
import { spawn, execSync } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const FRONTEND_DIR = path.join(REPO_ROOT, "frontend");
const FRONTEND_URL = "http://localhost:3000";
const SCAN_URL = `${FRONTEND_URL}/api/scan`;
const READY_TIMEOUT_MS = 60_000;
const READY_POLL_INTERVAL_MS = 500;

function log(message) {
  console.log(`[start] ${message}`);
}

if (!existsSync(path.join(FRONTEND_DIR, "node_modules"))) {
  log("Installing frontend dependencies (first run only)...");
  execSync("npm install", { cwd: FRONTEND_DIR, stdio: "inherit" });
}

log("Starting frontend dev server...");
const frontend = spawn("npm", ["run", "dev"], {
  cwd: FRONTEND_DIR,
  stdio: "inherit",
  shell: process.platform === "win32",
});

let shuttingDown = false;
function shutdown() {
  if (shuttingDown) return;
  shuttingDown = true;
  log("Shutting down...");
  frontend.kill();
}
process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);

frontend.on("exit", (code) => {
  if (!shuttingDown) {
    log(`Frontend dev server exited unexpectedly (code ${code}).`);
    process.exit(code ?? 1);
  }
});

async function waitUntilReady(url, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      await fetch(url, { method: "GET" });
      return true;
    } catch {
      // Not accepting connections yet - keep polling.
    }
    await new Promise((resolve) => setTimeout(resolve, READY_POLL_INTERVAL_MS));
  }
  return false;
}

const ready = await waitUntilReady(FRONTEND_URL, READY_TIMEOUT_MS);
if (!ready) {
  log(`Frontend did not become ready within ${READY_TIMEOUT_MS / 1000}s - skipping auto-scan.`);
  log("The dev server is still running; trigger a scan manually from the UI once it's up.");
} else {
  log("Frontend ready. Triggering a scan...");
  try {
    const response = await fetch(SCAN_URL, { method: "POST" });
    const data = await response.json();
    if (response.status === 202 && data.started) {
      log("Scan started - watch its progress in the sidebar.");
    } else if (response.status === 409) {
      log("A scan was already in progress.");
    } else {
      log(`Unexpected scan response (${response.status}): ${JSON.stringify(data)}`);
    }
  } catch (error) {
    log(`Could not trigger the scan automatically: ${error.message}`);
    log("You can still trigger one manually from the UI.");
  }
}

log(`App running at ${FRONTEND_URL} - press Ctrl+C to stop.`);
