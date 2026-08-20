# AGENTS.md

Context file for AI agents working on this repo. Read this first — it's
denser and more current than README.md/ARCHITECTURE.md, which are written
for humans and don't cover "where exactly do I put a new file".

## What this project is

Local-first Kotlin/Spring Boot pipeline that discovers new residential
real-estate investments in the Poznań metro area from three source
categories (developer sites, official discovery/BIP registers, aggregator
portals), deterministically diffs/persists them to SQLite, and exposes a
Next.js dashboard reading the same DB file directly. One-shot execution
(`./gradlew bootRun` = scan, report, exit — not a server).

Full narrative docs: `README.md` (user-facing), `docs/ARCHITECTURE.md`
(design), `docs/ADR-*.md` (why decisions were made). This file is the
"how do I actually make a change" cheat sheet.

## Versions (don't guess — check before assuming an API exists)

| | |
|---|---|
| JDK | 21 (toolchain-pinned in `build.gradle.kts`) |
| Kotlin | 2.2.10 |
| Spring Boot | 3.5.4 |
| Jsoup | 1.21.1 |
| SQLite JDBC | `org.xerial:sqlite-jdbc:3.50.3.0` |
| Flyway | via Spring Boot dependency management |
| Kotest/JUnit5 | 6.0.3 |
| Node | `>=22.5.0` (required for built-in `node:sqlite`) |
| Next.js | 16.3.1, React 19.2.8 |
| Tailwind | v4 |

## Commands

```bash
./gradlew bootRun            # one real scan against live sources, then exit
./gradlew test                # all Kotlin tests, NO network access (fixture-based)
./gradlew verifySources        # hits real source URLs, prints health, NEVER writes trusted state
./gradlew captureFixtures      # downloads current HTML into src/test/resources/fixtures/ — review before committing

cd frontend
npm install
npm run dev                    # http://localhost:3000, requires ../investment-monitor.db to exist
npm run build                  # production build; also the fastest way to typecheck everything
npx eslint .                   # 3 pre-existing errors are known/accepted (see "Known lint noise" below)
```

Both processes (Kotlin scan + Next dev server) can write to
`investment-monitor.db` concurrently — SQLite `busy_timeout=5000` is set
on both sides for exactly this reason. Don't remove it.

## Repo layout

```
src/main/kotlin/pl/marcinwieczorek/investmentmonitor/
  domain/          Investment, InvestmentSignal, SourceEvidence, Correlation,
                   LocationProfile, LocationCatalog, ReferenceInvestmentProfile,
                   SourceCategory — pure data classes/enums, no framework deps
  source/          InvestmentSource (developer), DiscoverySource, AggregatorSource
                   interfaces + SourceRegistry. Per-source subpackages:
                     source/discovery/SwarzedzWz{Source,Parser}.kt
                     source/aggregator/RynekPierwotny{Source,Parser}.kt
                     source/detail/TercjaDetailParser.kt (per-investment detail page)
                   Chronos/Greenbud sources+parsers live directly in source/
  scraping/        PageFetcher (fun interface), JsoupPageFetcher (impl),
                   ArchivingPageFetcher (@Primary decorator — transparently
                   archives every fetch, wraps JsoupPageFetcher by concrete type,
                   and transparently routes to PlaywrightPageFetcher for
                   registry-flagged hosts — see ADR-007), PlaywrightPageFetcher
                   (opt-in headless-browser impl, disabled by default),
                   ScrapingConfig (@Configuration — derives browserRequiredHosts
                   from DiscoverySourceRegistry/DeveloperRegistry)
  detection/       ChangeDetector — canonical-key diff (NEW/CHANGED/UNCHANGED/REMOVED)
  validation/      SourceValidator — fail-closed drop-threshold + empty-result rejection
  correlation/     InvestmentCorrelator — deterministic signal<->investment linking;
                   InvestmentDeduplicator — deterministic investment<->investment
                   cross-source duplicate matching (never LLM-driven, same rationale)
  analysis/        InvestmentAnalyzer interface, DefaultInvestmentAnalyzer (default),
                   DeterministicScorer, LocationProfiles (data), ReferenceProfiles (data)
  llm/             OllamaClient (JDK HttpClient, no Spring MVC dep), OllamaInvestmentAnalyzer,
                   InvestmentPromptBuilder, LlmInvestmentInterpretation (response DTO)
  archival/        RawHtmlArchiver — raw/<date>/<host>/<hash>.html, retention-based cleanup
  persistence/     One {Name}Repository interface + Jdbc{Name}Repository impl per aggregate:
                   Investment, Signal, SourceSnapshot, MonitoringRun, Evidence,
                   Correlation, InvestmentDuplicate, LlmAnalysis, UserPreferences
                   (generic key-value store, currently just the scoring profile)
  monitoring/      MonitoringService (the orchestrator — read this first for the
                   full pipeline), SourceCommitService (transactional per-source
                   commit — a separate bean so @Transactional isn't bypassed by
                   Spring's self-invocation limitation), EvidenceRecordingService
                   (per-fact provenance), CrossSourceEnrichmentService (gap-filling
                   from HIGH-confidence duplicates), AggregatorDiscoveryService
                   (aggregator-only detection + unknown-developer candidates),
                   ScanRunner (ApplicationRunner, makes bootRun one-shot),
                   RescoreService + RescoreRunner (recomputes investment_score for every
                   known investment against the current scoring profile, no live fetch —
                   activated via --investment-monitor.mode=rescore, mutually exclusive
                   with ScanRunner)
  reporting/       ScanReport (data), ScanReportRenderer (plain-text report)
  tools/           SourceVerificationCli, FixtureCaptureCli — plain `main()`, NOT
                   Spring-managed, construct sources manually
src/main/resources/
  application.yml
  db/migration/V1..V14__*.sql       Flyway, sequential, never edit an already-applied one
src/test/kotlin/...                Mirrors main/ package structure
src/test/resources/fixtures/<source>/*.html   Real captured HTML, reviewed before commit
  testsupport/  TestInvestments.kt (testInvestment()), TestSignals.kt (testSignal())
                — factory functions with sensible defaults, use these in new tests

frontend/
  app/                  Next.js App Router pages, one dir per route
    api/                 Route handlers (mutations + scan trigger only — pages
                         read the DB directly via lib/queries.ts, no fetch())
    map/                 /map page — investment location overview
    settings/            /settings page — configurable scoring reference profile
  components/
    layout/               app-shell.tsx / app-sidebar.tsx / app-header.tsx
    ui/                   shadcn-on-@base-ui/react primitives — don't hand-roll these
    charts/               ApexCharts wrappers (dynamic import, ssr:false)
    map/                   Leaflet map (dynamic import, ssr:false — touches window/document)
    settings-view.tsx      scoring profile form (property types, tiers, area/price
                           ranges, large-plot toggle) — saves via PUT /api/preferences,
                           which also triggers a rescore-all (see lib/rescore.ts)
    <name>-view.tsx        "use client" page content components, one per route
  lib/
    db.ts                 node:sqlite singleton, WAL + busy_timeout
    queries.ts             ALL SQL lives here — pages call these, never inline SQL in a page
    rescore.ts              triggerRescore() — shells out to `./gradlew bootRun
                            --args=--investment-monitor.mode=rescore` (same
                            child_process pattern as /api/scan), used by
                            PUT /api/preferences and POST /api/rescore
    types.ts               Row interfaces mirroring exact DB columns (snake_case!);
                           also ScoringProfile + DEFAULT_SCORING_PROFILE (client-safe,
                           mirrors ReferenceProfiles.POZNAN_HOUSE_SEEKER on the Kotlin
                           side — keep in sync if that default ever changes)
    i18n.tsx                React Context, en/pl, see messages/*.json; useI18n() exposes
                            both t(key) and tEnum(category, value) — use tEnum for any raw
                            backend enum value (status, signal_type, confidence, ...) so it
                            never regresses to a literal i18n key if a translation is missing
    location-coordinates.ts  Static lat/lng lookup for every LocationCatalog.kt location name
                            (frontend-only reference data, same rationale as LocationCatalog
                            itself — no live geocoding API call)
    sidebar-context.tsx     collapsed/expanded state, localStorage-persisted
    constants.ts            NEW_THRESHOLD_MS, STALE_THRESHOLD_MS
    utils.ts                cn(), formatRelativeTime(), formatArea(), dataCompleteness()
  messages/en.json, pl.json    flat-ish nested i18n dicts, keep both in sync

docs/
  ARCHITECTURE.md   design overview, "what's implemented vs not"
  SOURCES.md        source standard + "investigated but not implemented" table
  DISCOVERY.md      discovery-source-specific notes (signal types, location extraction)
  LLM.md            Ollama setup/config/failure-handling
  SOURCE-VERIFICATION.md   fixture-test vs verifySources workflow
  ADR-00N-*.md      one decision each, Status/Context/Decision/Consequences format
```

## The pipeline, end to end (read `MonitoringService.scan()` for ground truth)

```
SourceRegistry.{developerSources,discoverySources,aggregatorSources}()
  -> per-source: fetch() -> SourceValidator.validate() -> ChangeDetector.detect()
     -> (developer only) InvestmentDetailEnricher.enrich() + InvestmentAnalyzer.analyze()
     -> commit (upsert + SourceSnapshot) IFF fetch succeeded AND validation passed
  -> InvestmentCorrelator.correlate(allInvestments, allSignals) over the FULL current
     set (not just this run's new items) -> persisted as Correlation rows
  -> InvestmentDeduplicator.findDuplicates(allInvestments) over the FULL current set
     -> persisted as InvestmentDuplicate rows; HIGH-confidence pairs also trigger
     cross-source enrichment (borrow missing price/area/propertyType from a
     confirmed duplicate on another source, never overwrite an existing fact)
  -> aggregator-only-discovery heuristic (new aggregator investment whose location
     isn't covered by any developer source yet)
  -> RawHtmlArchiver.cleanup() (retention enforcement, once per scan)
  -> ScanReport -> ScanReportRenderer.render() -> logged
```

Identity for both `Investment` and `InvestmentSignal` is
`source:normalized-url` (lowercased, trailing slash stripped) —
`canonicalKey` computed property on both domain classes. **Never** change
this scheme without reading ADR-002 first; the entire diff/persistence
model depends on it being stable.

## Adding a new developer source

1. `src/main/kotlin/.../source/{Name}Source.kt` implementing `InvestmentSource`
   (`val id: String`, `fun fetch(): List<Investment>`).
2. `src/main/kotlin/.../source/{Name}Parser.kt` — plain class, `fun parse(html, baseUri): List<Investment>`,
   Jsoup-based, selectors verified against **real captured HTML only**.
3. Capture a fixture: add the source to `FixtureCaptureCli.kt`'s `targets` map,
   run `./gradlew captureFixtures`, inspect the output before using it.
4. `src/test/kotlin/.../source/{Name}ParserTest.kt` — asserts every field the
   page actually publishes (location, area, price, image) against the fixture.
   Leave unpublished fields `null` in the parser — never guess.
5. Add to `SourceVerificationCli.kt`'s source lists.
6. Spring auto-discovers it via `@Component` + constructor injection — no
   manual wiring needed, `SourceRegistry` picks it up automatically.
7. Update `docs/SOURCES.md` monitored-sources table and `README.md`.

## Adding a new discovery or aggregator source

Same shape as above but implement `DiscoverySource` (returns
`InvestmentSignal`, needs `municipality`) or `AggregatorSource` (returns
`Investment`, category = AGGREGATOR). **Before writing any selector**:
verify with `curl`/browser devtools that the content is server-rendered
HTML — many modern municipal/portal sites are JS-rendered SPAs with no
public API (see `docs/SOURCES.md` "Investigated but not implemented" for
two real examples of this exact trap: Kleszczewo BIP, Komorniki BIP,
Otodom). If you can't get real HTML without executing JS or fighting a
WAF, don't fake it — document it as PLANNED in `docs/SOURCES.md` instead.

Prefer anchoring selectors on stable attributes (`data-testid`, semantic
tags, explicit case-reference regexes) over CSS classes — see
`RynekPierwotnyParser`'s KDoc for why (content-hashed CSS-in-JS classes
regenerate every deploy).

## Adding a new frontend page

1. `app/<name>/page.tsx` — server component, `export const dynamic = "force-dynamic"`,
   calls functions from `lib/queries.ts` directly (no fetch, no API route
   needed unless it's a mutation).
2. New queries go in `lib/queries.ts`. **`node:sqlite` returns
   `[Object: null prototype]` rows** — always spread (`{ ...row }`) before
   passing to a Client Component, or RSC serialization breaks silently.
3. New row shapes go in `lib/types.ts`, exact snake_case column names.
4. `components/<name>-view.tsx` — `"use client"`, receives data as props.
5. Add nav entry to `components/layout/app-sidebar.tsx` `navItems`.
6. Add breadcrumb entry to `components/layout/app-header.tsx` `breadcrumbKeys`.
7. Add every new string to **both** `messages/en.json` and `messages/pl.json`
   — same nesting, keep them in lockstep or `useI18n()` silently falls
   back to the English string.
8. `npm run build` to typecheck before considering it done.

## Database

SQLite file `investment-monitor.db` in repo root (gitignored). Flyway
migrations in `src/main/resources/db/migration/`, currently V1–V14. To add
a column/table: new `V15__description.sql` — **never edit an already-
applied migration**, Flyway checksums them.

Tables: `investment`, `source_snapshot` (+`source_category`),
`monitoring_run`, `user_note`, `investment_state` (frontend-only, notes/
archive), `investment_signal`, `source_evidence`, `correlation`,
`investment_duplicate`, `llm_analysis`, `developer_registry`, `developer_candidate`,
`municipality_registry`, `investment_score` (has both `investment_canonical_key`,
its original key, and a nullable `investment_id` FK added in V14 - nullable
because scoring for a newly-discovered investment happens before that
investment's row exists yet, see `JdbcInvestmentScoreRepository`),
`user_preferences`
(generic key-value store, currently just `key="scoring.profile"` - this one
actually is read at scan/rescore time, see
`UserPreferencesRepository`). The previously-unused `location_profile`
table (declared in V4, never populated at runtime) was dropped in V13.

## Conventions

- **Git author**: `Marcin Wieczorek <kontakt@marcinwieczorek.pl>`. Repo:
  `github.com/marcin-wieczorek/investment-monitor`, branch `main`.
- **Commit messages**: conventional-ish (`feat:`, `fix:`, `docs:`,
  `refactor:`), body explains *why* not just *what*, matches the style of
  existing `git log`.
- **Kotlin**: idiomatic, small interfaces, no speculative abstraction, no
  universal/generic scraper — one parser per site, verified against real
  HTML. `@Component`-annotated classes are auto-wired by Spring; plain
  classes (parsers) are instantiated directly by their `Source` wrapper.
  Prefer `runCatching` + explicit fallback over try/catch when failure is
  an expected, handled case (see `MonitoringService`, `OllamaClient`).
- **Tests**: Kotest matchers (`shouldBe`, `shouldHaveSize`) + JUnit5
  `@Test`. Fixture-based parser tests must not touch the network.
  Integration-style tests (e.g. `MonitoringServiceTest`) use hand-written
  in-memory fake repositories (see that file for the pattern), not
  Spring's test context or a real DB.
- **Frontend**: shadcn/ui components live in `components/ui/` and are
  built on `@base-ui/react` (not Radix) — use the `render` prop, not
  `asChild`. Tailwind v4. i18n via custom React Context
  (`lib/i18n.tsx`), not next-intl. Dark mode via `next-themes`, default
  dark.

## Known lint noise (pre-existing, not regressions)

`npx eslint .` in `frontend/` reports 3 errors
(`react-hooks/set-state-in-effect` in `theme-toggle.tsx`, `lib/i18n.tsx`,
`lib/sidebar-context.tsx`) that predate recent work and follow an
established pattern (hydration-safe mount-flag / localStorage-read
effects). Don't "fix" these as a drive-by unless specifically asked —
they're a deliberate, accepted tradeoff, and undated with them touching
unrelated files just adds diff noise.

## Deterministic core — do not make this LLM-dependent

Identity (`canonicalKey`), diffing (`ChangeDetector`), validation
(`SourceValidator`), cross-source correlation/deduplication
(`InvestmentCorrelator`, `InvestmentDeduplicator`), persistence, and the
numeric investment score (`DeterministicScorer`) must stay pure/deterministic. The LLM
(`OllamaInvestmentAnalyzer`, disabled by default via
`investment-monitor.llm.enabled=false`) only ever contributes
`priority`/`reason` text, and only when it returns well-formed JSON —
every failure mode falls back to a value derived purely from
`DeterministicScorer`. If you're tempted to have the LLM decide "is this
new" or "what's the price", stop — read ADR-002 and ADR-005 first.
