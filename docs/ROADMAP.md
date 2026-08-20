# Roadmap — next phases

Context file for continuing the evolution described in `AGENTS.md` (the
original full-scope prompt). Read `AGENTS.md` first for the complete
vision; this file tracks **what's already done** vs **what's next**,
concretely, so a future session can pick up without re-deriving the plan.

Last updated: after the session that added developer/municipality
registries, 16 developer + 4 discovery adapters, deterministic scoring
pipeline, discovery lead time, and price/watchlist UI (commit
`b35fa1f`).

## Already implemented (do not redo)

- Domain foundation: `Developer`, `DeveloperCandidate`, `Municipality` +
  registries (`registry/DeveloperRegistry.kt`, `registry/MunicipalityRegistry.kt`,
  `registry/DiscoverySourceRegistry.kt`), mirrored into SQLite via
  `V5__developer_municipality_registry.sql`.
- 18 developer adapters (Chronos, Greenbud + 16 new: ATAL, Agrobex,
  Spravia, Duda, Develia, Jakon Inwest, ROBYG, Linea, Murapol, Ataner,
  Konimpex, Pekabex, EBF, GGW, JakśBud, UWI), 5 discovery adapters
  (Swarzędz + Czerwonak, Tarnowo Podgórne, Suchy Las, Poznań ULICP), 1
  aggregator (RynekPierwotny).
- Deterministic scoring always runs (`DefaultInvestmentAnalyzer` +
  `DeterministicAnalysisSupport`), persisted via `InvestmentScoreRepository`
  / `investment_score` table (`V6__investment_score_watchlist.sql`).
- Discovery lead time: `CorrelationRepository.findAllWithLeadTime()`,
  rendered in the console report and on `/correlations`/dashboard.
- Watchlist: `investment_state.watched` column, `PUT /api/investments/[id]/watch`.
- Frontend: `/developers`, `/coverage` pages; investments list with
  source-category badges, advanced filter panel (range sliders for
  area/price), score badge column, price column; investment detail with
  scoring breakdown, price/ratio badges, watch button.

See `docs/ARCHITECTURE.md` "Implemented (phase 5/6, ...)" sections and
`docs/SOURCES.md` for the authoritative, detailed record.

## Next phases (in priority order)

### Phase D — Per-field provenance

**Problem**: `SourceEvidence` schema supports per-field records
(`fieldName`/`fieldValue`), but `MonitoringService.recordInvestmentEvidence()`
/ `recordSignalEvidence()` always write a single record per entity per
scan (`fieldName="investment"`/`"signal"`), not one per actual fact.

**Plan**:
1. `MonitoringService.recordInvestmentEvidence()` — instead of one
   `SourceEvidence` row, write one row per non-null `Investment` field
   (`name`, `location`, `propertyType`, `units`, `houseArea`, `plotArea`,
   `price`, `status`, `imageUrl`), each with its own `fieldValue`.
2. Same for `recordSignalEvidence()` — one row per non-null
   `InvestmentSignal` field (`title`, `location`, `signalType`,
   `reference`, `detectedAt`).
3. Frontend `investment-detail-view.tsx` — group evidence rows by
   `field_name` so multi-source confirmation of the same fact becomes
   visible (e.g. "price confirmed by both developer site and aggregator").
4. Tests: `MonitoringServiceTest` — assert evidence contains distinct
   `field_name` values matching the investment's non-null fields, not a
   single "investment" placeholder.

### Phase E — Remaining Tier B `CANDIDATE` developers

7 developers have a verified website and `CANDIDATE` status but no
adapter yet (see `registry/DeveloperRegistry.kt`):

| ID | Name | Website |
|---|---|---|
| `cordia` | Cordia | https://cordiapolska.pl |
| `ronson` | Ronson | https://ronson.pl |
| `sivanet` | SIVANET | https://sivanet.pl |
| `mj` | MJ Deweloper | https://mjdeweloper.pl |
| `area` | Area Development | https://areadevelopment.pl |
| `inwestycje_wielkopolski` | Inwestycje Wielkopolski | https://inwestycjewielkopolski.pl |
| `vastbouw` | Vastbouw | https://vastbouw.pl |

**Per developer** (same workflow as the 16 already implemented):
1. `curl` the real investment listing page, verify it's server-rendered
   (not a JS SPA) - **do not assume**, some may turn out unimplementable
   like Nickel/Archicom/PWD.
2. Capture a fixture (`FixtureCaptureCli.kt` + `./gradlew captureFixtures`),
   inspect the HTML by hand before writing selectors.
3. `{Name}Source.kt` (`InvestmentSource`) + `{Name}Parser.kt` (Jsoup,
   anchored on stable attributes, leave unpublished fields null).
4. `{Name}ParserTest.kt` - fixture-based, asserts every published field.
5. Register in `SourceVerificationCli.kt`.
6. Update `registry/DeveloperRegistry.kt` - flip status to `MONITORED`,
   set `adapterSourceId`.
7. Update `V5__developer_municipality_registry.sql`? **No** - existing
   migrations are never edited once applied; add a new
   `V7__promote_tier_b_candidates.sql` that `UPDATE`s the relevant rows
   (or just update the Kotlin registry object + accept the migration's
   static snapshot is slightly stale until the next full registry
   migration - confirm approach before implementing, since the DB is the
   frontend's source of truth for `/developers`).
8. Update `docs/SOURCES.md` "Implemented developer sources" list.

Villa (Tier A) and Cavallia/BTM/Constructa Plus/Virke/SGI/FB Antczak
(Tier B) remain `BLOCKED`/`CANDIDATE` with no known working URL - do not
re-investigate unless new information surfaces.

### Phase F — Candidate status mutation + aggregator-only discovery view

**Problem**: `DeveloperCandidate` rows are visible on `/developers` but
read-only; aggregator-only discoveries are only visible in the console
scan report.

**Plan**:
1. `PUT /api/developers/candidates/[id]/status` route - body
   `{ status: "ACCEPTED" | "REJECTED" | "IMPLEMENTED" | "BLOCKED" }`,
   calls a new `updateDeveloperCandidateStatus()` query wrapping
   `DeveloperCandidateRepository.updateStatus` (needs a thin query
   function in `lib/queries.ts` similar to `setWatched`/`setArchived` -
   currently the frontend has no write path into `developer_candidate`
   at all, only Kotlin's `JdbcDeveloperCandidateRepository` does).
2. `developers-view.tsx` - Accept/Reject/Block buttons per candidate row,
   `router.refresh()` after mutation (same pattern as archive/watch).
3. New query: `listAggregatorOnlyInvestments()` or reuse `listInvestments`
   with a `sourceCategory: "AGGREGATOR"` filter plus "no developer source
   covers this location" logic (mirrors
   `MonitoringService.findAggregatorOnlyDiscoveries()` - decide whether to
   replicate the location-coverage check in SQL/JS or persist a flag on
   the investment row at scan time instead of recomputing it ad hoc).
4. Dashboard section or `/investments` filter surfacing aggregator-only
   discoveries explicitly (not just visible via manual source-category
   filtering).

### Phase G — Dashboard enhancements

1. "Discovery lead time trend" chart (`ApexCharts` line/area, x-axis =
   correlation date, y-axis = lead time days) - mirrors
   `new-investments-chart.tsx`/`scan-success-chart.tsx` pattern.
2. "Coverage progress" mini-chart or stat breakdown (Tier A vs Tier B
   monitored count, municipality coverage by category) beyond the single
   stat cards that exist today.
3. "Sources needing attention" section - sources with `BLOCKED` status
   (from `DiscoverySourceRegistry`/`DeveloperRegistry`) or stale
   `source_snapshot` rows, surfaced together rather than requiring a
   visit to `/sources` + `/coverage` + `/developers` separately.

## Lower-priority / explicitly deferred (see AGENTS.md + docs/ARCHITECTURE.md)

- Additional discovery sources beyond the 5 implemented municipalities -
  see `registry/DiscoverySourceRegistry.kt` for the full blocked/
  not-implemented list and documented reasons per municipality.
- Otodom aggregator (requires headless browser, deliberately out of
  scope).
- Fuzzy developer-candidate deduplication (currently exact-name match
  only via `DeveloperRegistry.findByName`/`DeveloperCandidateRepository.findByName`).
- Signal-to-signal correlation (grouping filing stages of the same
  zoning case by shared `reference`).

## Working conventions to preserve

- Never invent a URL/selector - verify against real HTML first (`curl`
  the actual page, inspect it, only then write a parser). Document
  anything unimplementable as `BLOCKED`/`PLANNED` with a real reason,
  never silently skip it or fake an adapter.
- Every new parser needs a captured fixture + a test asserting every
  field it actually publishes, leaving unpublished fields `null`.
- Flyway migrations are append-only - never edit `V1`-`V6`, add `V7+`.
- Run `./gradlew test` (backend) and `npm run build` (frontend
  typecheck) before considering a phase done; a live `./gradlew bootRun`
  smoke-test is worth doing when touching `MonitoringService`/adapters
  end-to-end, then delete the generated `investment-monitor.db*` files
  (gitignored, but avoid leaving them lying around locally) before
  committing.
