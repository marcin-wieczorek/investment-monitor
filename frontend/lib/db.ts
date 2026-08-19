import path from "node:path";
import { DatabaseSync } from "node:sqlite";

declare global {
  // eslint-disable-next-line no-var
  var __investmentMonitorDb: DatabaseSync | undefined;
}

function resolveDatabasePath(): string {
  const configured = process.env.DATABASE_PATH;
  if (configured) {
    return path.isAbsolute(configured)
      ? configured
      : path.resolve(process.cwd(), /* turbopackIgnore: true */ configured);
  }
  // Default: the SQLite file the Kotlin pipeline writes to, one level up
  // from this frontend project (repo root).
  return path.resolve(process.cwd(), "..", "investment-monitor.db");
}

function createConnection(): DatabaseSync {
  const db = new DatabaseSync(resolveDatabasePath());
  db.exec("PRAGMA journal_mode = WAL");
  // The Kotlin pipeline writes to this same file from a separate process.
  // A busy timeout makes SQLite retry on a transient lock instead of
  // immediately throwing SQLITE_BUSY.
  db.exec("PRAGMA busy_timeout = 5000");
  return db;
}

/**
 * Singleton connection to the shared SQLite database written by the Kotlin
 * monitoring pipeline. Uses Node's built-in `node:sqlite` (no native addon
 * to compile/ship) - requires Node 22.5+. Reused across hot reloads in
 * development so we don't exhaust file handles.
 */
export function getDb(): DatabaseSync {
  if (!global.__investmentMonitorDb) {
    global.__investmentMonitorDb = createConnection();
  }
  return global.__investmentMonitorDb;
}
