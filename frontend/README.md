# Investment Monitor — Frontend

A minimal Next.js dashboard over the SQLite database written by the Kotlin
monitoring pipeline. Reads the database directly via Node's built-in
`node:sqlite` (no native addon to compile/ship) and can trigger a new scan
on demand.

## Setup

Requires Node 22.5+ (for `node:sqlite`).

```bash
npm install
cp .env.local.example .env.local   # optional, defaults to ../investment-monitor.db
npm run dev
```

Requires the pipeline to have run at least once (`../gradlew bootRun` from
the repo root) so `investment-monitor.db` exists.

## What it does

- **Dashboard** — last scan summary, source health, recently detected investments.
- **Investments** — searchable/filterable list with thumbnails, detail view with notes and archiving.
- **History** — timeline of every monitoring run.
- **Sources** — health status per monitored developer.

Triggering a scan from the UI runs `./gradlew bootRun` in the repo root as a
subprocess and refreshes the page once it completes.

## Notes

- Reads/writes SQLite directly via Node's built-in `node:sqlite` — see `lib/db.ts`.
- `user_note` / `investment_state` tables (notes, archiving) are Flyway-managed
  by the Kotlin project (`V3__user_state.sql`) but only ever used by this frontend.
- No authentication — designed to run on `localhost` for a single user.
- UI language (EN/PL) is a client-side toggle stored in `localStorage`, not
  URL-based routing.
