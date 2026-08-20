/**
 * Cheap dev-only guard against `lib/types.ts` silently drifting from the
 * actual SQLite schema. `node:sqlite` gives back `unknown` (see every
 * `as unknown as T` cast in `lib/queries.ts`), so a renamed/dropped column
 * on the Kotlin/Flyway side would otherwise only surface as `undefined`
 * showing up in the UI - no compile error, no runtime error, just quietly
 * wrong data. This does not replace real schema validation (e.g. zod) -
 * it is a lightweight tripwire, intentionally with zero new dependencies,
 * that logs a loud warning in `next dev` the first time a query's shape
 * doesn't match what `lib/types.ts` expects.
 */

const isDev = process.env.NODE_ENV !== "production";

export function checkRowShape<T extends object>(
  rows: readonly unknown[],
  expectedKeys: readonly (keyof T & string)[],
  context: string
): void {
  if (!isDev || rows.length === 0) return;
  const row = rows[0] as Record<string, unknown>;
  const actualKeys = new Set(Object.keys(row));
  const missing = expectedKeys.filter((key) => !actualKeys.has(key));
  if (missing.length > 0) {
    console.warn(
      `[dev-only] ${context}: query result is missing expected field(s): ${missing.join(", ")}. ` +
        "lib/types.ts may be out of sync with the SQLite schema (check recent Flyway migrations)."
    );
  }
}
