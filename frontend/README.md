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
npm run dev   # http://localhost:3000
```

Requires the pipeline to have run at least once (`../gradlew bootRun` from
the repo root) so `investment-monitor.db` exists.

For a production-style run instead of the dev server:

```bash
npm run build
npm run start   # http://localhost:3000
```

## What it does

- **Dashboard** — last scan summary, source health, discovery signal count, recently detected investments.
- **Investments** — searchable/filterable list with thumbnails, detail view with notes, archiving, provenance (evidence) and correlated discovery signals.
- **Signals** — discovery signals (municipal zoning/planning evidence) from all discovery sources, filterable by type.
- **Correlations** — deterministic links between discovery signals and investments that likely describe the same project.
- **History** — timeline of every monitoring run.
- **Sources** — health status per monitored source, grouped by category (developer/discovery/aggregator).

Triggering a scan from the UI runs `./gradlew bootRun` in the repo root as a
subprocess and refreshes the page once it completes.

## Notes

- Reads/writes SQLite directly via Node's built-in `node:sqlite` — see `lib/db.ts`.
- `user_note` / `investment_state` tables (notes, archiving) are Flyway-managed
  by the Kotlin project (`V3__user_state.sql`) but only ever used by this frontend.
- No authentication — designed to run on `localhost` for a single user.
- UI language (EN/PL) is a client-side toggle stored in `localStorage`, not
  URL-based routing.
