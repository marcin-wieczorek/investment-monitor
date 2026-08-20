# Architecture

```
sources (developer / discovery / aggregator)
    -> fetch -> parser -> validation -> deterministic diff
    -> enrich (new only) -> correlate -> deduplicate -> cross-source enrich
    -> analyze (new only, optional local LLM)
    -> report -> trusted snapshot
```

## Source categories

Three independent source categories, each with its own contract (see
`source/InvestmentSource.kt`, `source/DiscoverySource.kt`,
`source/AggregatorSource.kt`), collected by `SourceRegistry`:

- **Developer** (`InvestmentSource`) - a developer's own site. Ground
  truth for its own investments.
- **Discovery** (`DiscoverySource`) - official/public sources that reveal
  planned residential development before a marketable investment
  necessarily exists (municipal zoning-conditions registers, planning
  decisions, ...). Returns `InvestmentSignal`, not `Investment` - a
  signal is evidence, not a claim that a specific project exists (see
  `docs/DISCOVERY.md`).
- **Aggregator** (`AggregatorSource`) - third-party portals (RynekPierwotny,
  Otodom, ...). A completeness/cross-check layer only.

**Source precedence for factual authority**: developer > discovery >
aggregator. An aggregator listing is never treated as the source of truth
for an investment's identity or fields when a first-party developer
source already covers it.

## Implemented (phase 1-3, developer pipeline)

- `chronos` + `greenbud` developer sources, `SourceValidator` (fail-closed
  drop-threshold + empty-result rejection), `ChangeDetector` (canonical-key
  diff, including `REMOVED`), SQLite persistence via JdbcTemplate,
  `MonitoringService` + `ScanRunner` one-shot orchestration,
  `ScanReportRenderer` plain-text report, a Next.js dashboard reading the
  same SQLite file.

## Implemented (phase 4, three source categories + discovery + aggregator)

- **`SourceRegistry`** groups all `InvestmentSource`/`DiscoverySource`/
  `AggregatorSource` beans by category (`source/SourceRegistry.kt`).
- **Discovery**: `SwarzedzWzSource` + `SwarzedzWzParser` parse Gmina
  Swarzędz's real "warunki zabudowy" (zoning conditions) register -
  verified against live HTML, ~280 real decision documents, including
  genuine large-scale residential cases (e.g. a 224-house development in
  Kruszewnia). See `docs/DISCOVERY.md` for what else was investigated and
  why it isn't implemented yet.
- **Aggregator**: `RynekPierwotnySource` + `RynekPierwotnyParser` parse
  RynekPierwotny's server-rendered "new houses, 4+ rooms, Wielkopolskie"
  listing (paginated - fetches every page until one yields no new
  offers), anchored on the site's own `data-testid` attributes (stable
  across deploys) rather than its content-hashed CSS classes.
- **`InvestmentSignal`** (`domain/InvestmentSignal.kt`) models discovery
  evidence: municipality, location, `SignalType`, title, reference,
  detection date, source URL, raw facts. Identity follows the same
  `source:normalized-url` canonical-key scheme as `Investment`
  (see ADR-002), so the same deterministic-diff philosophy applies.
- **`SourceEvidence`** (`domain/SourceEvidence.kt`) is an append-only
  provenance record: which source produced a fact, when, from which URL,
  and how (`PARSER`/`LLM`/`MANUAL`). Recorded per investment/signal at
  commit time (not yet per individual field - see "Known scope
  limitations" below).
- **`InvestmentCorrelator`** (`correlation/InvestmentCorrelator.kt`)
  deterministically links discovery signals to investments: same
  recognized location (never the signal's own municipality - too coarse
  to be meaningful) plus a residential-construction keyword filter (to
  exclude unrelated permits like retaining walls or transformer
  stations), with confidence raised to `HIGH` when the developer's name
  also appears in the signal's text. Never LLM-driven.
- **Cross-category orchestration**: `MonitoringService` now scans all
  three categories per run, runs correlation over the full current
  investment/signal set (not just this run's new items), and flags
  aggregator-only discoveries (new aggregator listings whose location
  isn't covered by any developer source yet).
- **Raw HTML archival**: `ArchivingPageFetcher` transparently wraps every
  page fetch (via the existing `PageFetcher` interface - no source code
  changed) and archives it under `raw/<date>/<host>/<hash>.html`, with
  configurable retention (`RawHtmlArchiver.cleanup()`, called once per
  scan).
- **`ReferenceInvestmentProfile`** (`domain/ReferenceInvestmentProfile.kt`)
  generalizes what used to be a hard-coded "similarity to Tercja" concept
  into an explicit, editable profile (`analysis/ReferenceProfiles.kt`).
- **`LocationProfile` data** (`analysis/LocationProfiles.kt`) for every
  municipality/village in the target geographic scope (see
  `domain/LocationCatalog.kt`), explicit domain data an analyzer may
  interpret but never silently overwrite.
- **`DeterministicScorer`** (`analysis/DeterministicScorer.kt`) numerically
  compares an investment against a reference profile and location
  profile: property-type match, location-tier match, house/plot area fit
  (range-overlap scoring), price fit, and an explicit **large-plot
  bonus** - a plot larger than the reference profile's preferred range
  is rewarded, never penalized (see "Large plots" below).
- **Local LLM (Ollama)**: `OllamaClient` (plain JDK `HttpClient`, no
  Spring MVC dependency) + `OllamaInvestmentAnalyzer`
  (`ConditionalOnProperty`-gated on `investment-monitor.llm.enabled`,
  default `false`). The LLM only supplies `priority`/`reason` - the
  deterministic score always comes from `DeterministicScorer`. Any
  failure (unreachable, timeout, malformed JSON) falls back to a
  deterministic priority derived purely from the numeric score, so a
  missing/misconfigured local LLM never breaks a scan. See
  `docs/LLM.md`.
- **Extended report**: `ScanReportRenderer` now renders NEW/CHANGED
  investments, NEW DISCOVERY SIGNALS, CORRELATED SIGNALS, AGGREGATOR-ONLY
  DISCOVERIES and PARSER/SOURCE FAILURES sections, plus an explicit
  `★ LARGE PLOT` marker and a deterministic `STATUS:` line even when
  nothing changed (`NO NEW INVESTMENTS`).

## Large plots

Large plot area is treated as a **first-class positive feature**, not a
filter criterion:

- `DeterministicScorer` gives a plot larger than the reference profile's
  preferred range a bonus (`ScoringResult.largePlotBonus`), on top of
  (not instead of) its range-fit score.
- `ScanReportRenderer` explicitly flags any new/changed investment whose
  plot area is ≥ 500 m² with `★ LARGE PLOT`, independent of whether an
  LLM is configured.
- Nothing in the pipeline ever rejects or down-ranks an investment purely
  for having a large plot.

## Geographic scope

`domain/LocationCatalog.kt` holds the explicit, extensible list of
municipalities/villages in scope (the Poznań metropolitan area, plus
villages within Gmina Swarzędz observed directly via discovery signals).
This is domain data, not hard-coded parsing logic - new locations are
added to the catalog, not scattered across parsers.

## Known scope limitations (deliberately not built yet)

Signal-to-signal correlation (grouping filing stages of the same zoning
case by shared `reference`) is implemented client-side in `/signals`
(see "Implemented (phase 8, ...)" below) rather than as a first-class
`Correlation` type - a plain reference-equality group-by needs no
scan-time matching, unlike deterministic investment<->signal correlation.

## Not yet implemented

- Additional discovery sources beyond the twelve municipalities implemented
  so far - see `registry/DiscoverySourceRegistry.kt` for the full,
  per-municipality investigation record (which BIPs are `BLOCKED`/
  `NOT_IMPLEMENTED` and why; Dopiewo and Skoki are the closest to real -
  both migrated to a real server-rendered platform but content wasn't
  parseable yet as of the last check).
- Otodom aggregator (requires JS execution to read; deliberately not
  implemented with a headless browser to keep this a lightweight,
  local-first CLI tool - see `docs/DISCOVERY.md`).
- Additional detail parsers for developer sites beyond the single
  Tercja/Chronos case - most developer list pages genuinely don't publish
  price or plot area at all (verified against real fixture HTML, not
  assumed); their detail pages would need to be captured and investigated
  per-site, and several are JS-rendered flat-finder widgets that would
  need the same no-headless-browser tradeoff as Otodom above. See
  `DeterministicScorer`'s data-completeness gap in "Implemented (phase 9,
  ...)" below for the current state.
- Automated test coverage for `InvestmentDetailEnricher` and
  `PolishAreaFormat` edge cases (still covered only indirectly).
- Per-investment geocoding for the `/map` page - it uses a static
  location-name -> centroid lookup (`frontend/lib/location-coordinates.ts`),
  good enough for a metro-area overview but not precise per-address
  placement.

## Implemented (phase 5, developer/municipality registries + broader coverage)

- **`Developer`/`DeveloperCandidate`/`Municipality`** (`domain/`) make the
  developer and geographic-coverage concepts first-class, independent of
  whether a working source adapter exists yet - see [`DeveloperStatus`]
  and [`MunicipalitySourceStatus`] for the explicit lifecycle states that
  keep a developer/municipality visible instead of silently disappearing
  when unimplemented.
- **`registry.DeveloperRegistry`** (`registry/DeveloperRegistry.kt`) lists
  every Tier A/B developer from AGENTS.md sections 3/4 with a manually
  verified website (or `null` + `BLOCKED`/`CANDIDATE` status when no
  working URL could be found - never an invented one). Mirrored into the
  `developer_registry` table by `V5__developer_municipality_registry.sql`
  so the frontend can read it directly.
- **`registry.MunicipalityRegistry`** lists all 22 target Metropolia
  Poznań municipalities (`domain/LocationCatalog.kt` gained the 8 that
  were previously missing: Buk, Oborniki, Pobiedziska, Puszczykowo, Skoki,
  Stęszew, Szamotuły, Śrem) with per-category (`developer`/`discovery`/
  `aggregator`) coverage status.
- **`registry.DiscoverySourceRegistry`** records the detailed municipal
  BIP (Biuletyn Informacji Publicznej) investigation outcome (URL looked at, status, and - when blocked - a
  specific documented reason), which is more detail than
  `MunicipalityRegistry`'s coverage status alone and is essential context
  for continuing discovery-source work later.
- **17 new developer adapters** and **4 new discovery adapters** - see
  `docs/SOURCES.md` "Implemented developer sources"/"Implemented discovery
  sources" for the full list, and `registry/DeveloperRegistry.kt`/
  `registry/DiscoverySourceRegistry.kt` for which developers/municipalities
  were investigated and found unimplementable (JS SPA, anti-bot, no
  register, ...).
- **`DeveloperCandidateRepository`**: `MonitoringService` now records a
  `DeveloperCandidate` whenever an aggregator-only discovery names a
  developer not present in `DeveloperRegistry` - the feedback loop
  described in AGENTS.md sections 6/33, always requiring human review
  before a candidate becomes a real adapter.
- **Frontend**: `/developers` (Tier A/B registry + discovered candidates)
  and `/coverage` (per-municipality source coverage matrix) pages; the
  investments list now shows a source-category badge per row and a
  collapsible "advanced filters" panel (source category, property type,
  status, location, plus range sliders for house area/plot area/price
  built on Base UI's `Slider` primitive).

## Implemented (phase 6, deterministic scoring pipeline + discovery lead time + watchlist)

- **Deterministic scoring always runs, LLM or not**: `DefaultInvestmentAnalyzer`
  replaces the old `NoOpInvestmentAnalyzer` - "no LLM configured" no longer
  means "no scoring happens". It calls `DeterministicScorer` directly
  against `ReferenceProfiles.DEFAULT` and a `LocationProfile` resolved via
  `LocationCatalog.findIn()` + `LocationProfiles.find()`, exactly the same
  deterministic path `OllamaInvestmentAnalyzer` uses for its own
  score/fallback. The priority/location-score/describe-score conversions
  are shared via `DeterministicAnalysisSupport` so both analyzers describe
  an identical score identically.
- **Scoring is persisted**: `MonitoringService` calls `DeterministicScorer`
  once per newly detected investment (independent of which `InvestmentAnalyzer`
  is active) and saves the full `ScoringResult` - including
  `plotToHouseRatio`, the large-plot bonus flag, and every component score -
  via `InvestmentScoreRepository`/`investment_score` table
  (`V6__investment_score_watchlist.sql`), keyed by canonical key like
  `llm_analysis`. This makes the "how similar is this to what I'm looking
  for" number queryable and displayable, not just logged once in a report.
- **Discovery lead time**: `CorrelationRepository.findAllWithLeadTime()`
  joins `correlation`/`investment`/`investment_signal` to compute
  `julianday(investment.first_seen_at) - julianday(signal.first_seen_at)`
  in SQL - a positive value means the discovery signal was detected before
  the developer published the investment (the core "early detection" KPI,
  AGENTS.md section 28). `ScanReport.leadTimes` carries this into a new
  `DISCOVERY LEAD TIME` report section (`ScanReportRenderer`).
- **Watchlist**: `investment_state` gained a `watched` column (alongside
  the existing `archived`), with its own upsert query (`setWatched`) and
  API route (`PUT /api/investments/[id]/watch`), independent of archiving.
- **Frontend**: investments list gained a colour-coded score badge column
  (green ≥66%, amber ≥40%, red below), a price column, sorting by score,
  and a "watched only" filter; the investment detail page gained a full
  scoring breakdown (progress bars per component, property-type/location-tier/
  large-plot-bonus badges), a price badge, a plot-to-house-ratio badge, and
  a watch/unwatch button; `/correlations` gained a lead-time badge per row
  ("+N days before developer"); the dashboard gained an average discovery
  lead time stat card.

## Implemented (phase 7, per-field provenance + remaining Tier B developers + candidate review workflow + dashboard enhancements)

- **Per-field provenance**: `SourceEvidence` is now recorded once per
  actual non-null fact (`name`/`location`/`propertyType`/`units`/
  `houseArea`/`plotArea`/`price`/`status`/`imageUrl` for investments;
  `title`/`signalType`/`detectedAt`/`location`/`reference` for signals) -
  see `MonitoringService.recordInvestmentEvidence`/`recordSignalEvidence` -
  rather than one `"investment"`/`"signal"` placeholder row per commit.
  `investment-detail-view.tsx` groups evidence by `field_name` and shows a
  "confirmed by N sources" badge when 2+ distinct `source_id`s independently
  publish the same fact.
- **26 developer adapters total**: the 7 remaining Tier B `CANDIDATE`
  developers with a verified URL (Cordia, Ronson, SIVANET, MJ Deweloper,
  Area Development, Inwestycje Wielkopolski, Vastbouw) are now `MONITORED`
  (`V7__promote_tier_b_candidates.sql`) - see `docs/SOURCES.md`
  "Implemented developer sources" for per-developer notes on what each
  site actually publishes.
- **Developer candidate review workflow**: `PUT /api/developers/candidates/[id]/status`
  wraps `DeveloperCandidateRepository.updateStatus`; `/developers` gained
  Accept/Reject/Block buttons per `NEW`/`REVIEW_REQUIRED` candidate row,
  closing the human-review feedback loop that was previously display-only.
- **Persisted aggregator-only discovery flag**: `investment.aggregator_only_discovery`
  (`V8__aggregator_only_discovery_flag.sql`) is recomputed by
  `MonitoringService.updateAggregatorOnlyDiscoveryFlags` for every current
  aggregator investment on every scan (not just this run's new ones, unlike
  the console-report-only `findAggregatorOnlyDiscoveries`), reusing the
  same `LocationCatalog` matching so the location-coverage logic stays
  single-sourced in the deterministic Kotlin core rather than re-derived in
  SQL/JS. The dashboard gained an "Aggregator-only discoveries" stat card
  linking to `/investments?aggregatorOnly=1`, and `/investments` gained a
  matching toggle filter and row badge.
- **Dashboard enhancements**: a "Discovery lead time trend" line chart
  (`charts/lead-time-trend-chart.tsx`, x-axis = correlation `created_at`,
  y-axis = `lead_time_days`, same ApexCharts pattern as
  `new-investments-chart.tsx`/`scan-success-chart.tsx`); a "Coverage
  breakdown" panel (`coverage-breakdown.tsx`) showing Tier A/B monitored
  counts and developer/discovery/aggregator municipality coverage as
  progress bars; a "Sources needing attention" panel
  (`sources-needing-attention.tsx`) surfacing stale `source_snapshot`
  rows, `BLOCKED` developers and municipalities with any `BLOCKED`
  coverage category together, each linking to its full page (`/sources`,
  `/developers`, `/coverage`) instead of duplicating it. No new backend
  queries were needed - `listDevelopers`/`listMunicipalities`/`listSources`/
  `listCorrelations` already exposed everything required.

## Implemented (phase 8, deferred-work cleanup: more discovery sources + fuzzy dedup + signal correlation)

- **2 more discovery sources**: `srem-wz` (Gmina Śrem BIP, two-step fetch
  since its register is split one page per calendar year rather than a
  single evergreen feed) and `murowana-goslina-obwieszczenia` (Gmina
  Murowana Goślina BIP, mixes zoning-conditions and public-purpose siting
  decisions on one feed). All previously `BLOCKED`/`NOT_IMPLEMENTED`
  municipalities were re-verified live rather than assumed still blocked;
  several turned out to have migrated to real server-rendered platforms
  (Kleszczewo, Dopiewo, Skoki) but no adapter could be built for them yet
  - see `docs/SOURCES.md` "Investigated but not implemented" for the
  updated per-municipality findings.
- **`LocationCatalog.findIn` bugfix**: found while building the two
  sources above - it used `\b` word boundaries, which Java defines using
  ASCII `[a-zA-Z0-9_]` only, so it silently failed to match most of its
  own catalog (`Poznań`, `Śrem`, `Łowęcin`, ...) whenever adjacent to
  punctuation/whitespace in real text, since Polish diacritics aren't
  ASCII word characters. Fixed with explicit `\p{L}`/`\p{N}` Unicode
  lookarounds. Affects every call site: aggregator-only-discovery
  location matching, developer-candidate municipality assignment, and
  every discovery parser's `location` field.
- **Fuzzy developer-candidate deduplication**: `domain.DeveloperNameMatcher`
  strips common Polish legal-entity suffixes (`Sp. z o.o.`, `S.A.`,
  `Sp. k.`, spelled-out forms, ...) before comparing names, used by both
  `DeveloperRegistry.findByName` and `DeveloperCandidateRepository.findByName`
  so "ABC Development" and "ABC Development Sp. z o.o." are recognized as
  the same developer instead of producing a duplicate candidate.
- **Signal-to-signal correlation**: `/signals` groups signals client-side
  by `source:reference` - multiple filing stages of the same case share
  one case reference within a single source's own numbering scheme.
  Shows a "N stages" badge per row and, when expanded, a chronological
  case-history list (`ExpandableTableRow` gained an optional
  `expandedExtra` slot for this). No scan-time/backend work needed - this
  is a plain reference-equality group-by over already-fetched data, not a
  fuzzy/feature-based match like investment<->signal correlation.
- Otodom explicitly stayed out of scope per the project's no-headless-browser
  constraint - re-confirmed, not re-implemented.

## Implemented (phase 9, cross-source deduplication + scoring data gaps + investment map + filter clarity)

- **Cross-source deduplication**: `InvestmentDeduplicator`
  (`correlation/InvestmentDeduplicator.kt`) deterministically links
  investments from *different* sources that likely describe the same
  project - e.g. "Tercja" on Chronos's own site and "Osiedle Tercja |
  Chronos" on RynekPierwotny. `canonicalKey` (`source:url`) never merges
  these on its own, so without this they'd sit as unrelated rows
  everywhere in the frontend. Matching requires a shared recognized
  `LocationCatalog` location, then developer-name match (via
  `DeveloperNameMatcher`) and/or investment-name token overlap ->
  HIGH/MEDIUM/LOW confidence, same never-LLM-driven philosophy as
  `InvestmentCorrelator`. Never compares two investments from the same
  source (canonical-key identity is already ground truth there). Results
  persist to `investment_duplicate` (`V10__investment_duplicate.sql`).
  `/investments` groups HIGH/MEDIUM duplicate rows under the most
  authoritative source (`DEVELOPER` > `DISCOVERY` > `AGGREGATOR`, tie-break
  by earliest `first_seen_at`) with a "confirmed by N sources" badge and
  an expandable list of the other sources; LOW-confidence pairs are
  intentionally never auto-merged in the UI. `/investments/[id]` shows a
  "related listings from other sources" section for every confidence
  level.
- **Cross-source enrichment**: `MonitoringService.runCrossSourceEnrichment`
  runs after deduplication, HIGH-confidence pairs only, and borrows
  missing `price`/`houseArea`/`plotArea`/`propertyType` from the sibling
  investment on the other source - never overwrites a fact a source
  already published itself (developer authority is preserved, only gaps
  are filled). Records full `SourceEvidence` provenance attributing the
  borrowed fact to the partner source, and re-runs `DeterministicScorer`
  immediately so the improved completeness is reflected without waiting
  for the next scan.
- **Scoring data-completeness gap addressed**: most developer list pages
  never publish price/plot area/property type (verified per-fixture, not
  assumed), so `DeterministicScorer` was often computing a confident-
  looking percentage from a single dimension. `InvestmentStatus` widened
  (`LAST_UNITS`, `READY_FOR_HANDOVER`, `UNDER_CONSTRUCTION`) to capture
  readiness states four parsers (Agrobex, Develia, Linea, JakonInwest)
  actually publish per-card but previously discarded - each selector
  verified against real fixture HTML with BeautifulSoup before writing
  Kotlin, not guessed (two candidates investigated and dropped: Area and
  Konimpex's apparent status text turned out to be either constant across
  every card or unscoped to a specific investment - no real signal, so
  nothing was added there). `frontend/lib/utils.ts` gained
  `dataCompleteness()` (0-1 fraction of 6 key fields present) - the
  investments list now shows "Not enough data" instead of a misleading
  score percentage below a 2-of-6 threshold, and the detail page shows an
  explicit N/6 badge next to the scoring breakdown.
- **Investment map**: a new `/map` page (Leaflet + `react-leaflet`,
  OpenStreetMap tiles, no API key - dynamically imported with `ssr:false`,
  same pattern as the ApexCharts dashboard charts since `MapContainer`
  touches `window`/`document`). Pins are grouped per-location (not per-
  investment - there's no per-investment geocoding and ~50 known
  locations don't need a marker-cluster plugin), coloured by the most
  authoritative source category present at that location. Coordinates
  come from a static, curated lookup
  (`frontend/lib/location-coordinates.ts`) covering every name in
  `LocationCatalog.kt` - same rationale as that file: no live geocoding
  API call for something this project can hardcode once and review. The
  `location_profile` table was investigated as a home for these
  coordinates but turned out to be dead code (created in `V4`, never
  populated or read by any repository) - reanimating an unused table was
  worse than a frontend-only lookup for what is architecturally reference
  data anyway.
- **UI clarity fixes on `/investments`**: the sort-field dropdown was
  previously hidden inside the collapsed "Advanced filters" panel with
  only a lone direction-toggle arrow on the "First seen" column header as
  a visible (and misleading - it looked like it changed *what* you sort
  by, but only ever flipped direction) hint. Moved to an always-visible
  "Sort by" control in the quick-filter bar. The "Aggregator-only
  discovery" toggle previously sat in the quick-filter bar right next to
  source-category filtering in the advanced panel, both visually implying
  "only aggregator stuff" while filtering completely different fields
  (`aggregator_only_discovery` vs `source_category`) - moved next to the
  source-category select where it belongs, with an explicit tooltip.
  Added `overflow-x-auto` to the results table so a cramped viewport
  scrolls horizontally instead of pushing the expand-row chevron off
  screen.
- **Enum translations**: raw backend enum values (`signal_type`,
  investment `status`, `property_type`, correlation/duplicate
  `confidence`, monitoring run `status`) were previously shown verbatim
  in the UI (e.g. `"WZ_DECISION"`, `"READY_FOR_HANDOVER"`). `useI18n()`
  gained `tEnum(category, value)`, translating via
  `enum.<category>.<value>` in `messages/{en,pl}.json` with a fallback to
  the raw value (never the dotted i18n key) if a translation is missing,
  so a newly-added Kotlin enum constant never regresses to showing a
  literal key.

## Implemented (phase 10, score explanation + configurable scoring preferences)

- **Score tooltip**: an info icon next to the "Score" column header on
  `/investments` and the "Deterministic scoring breakdown" heading on
  `/investments/[id]` explains what the percentage measures (weighted
  match: property type 25%, location tier 15%, plot area fit 25%, house
  area fit 20%, price fit 15%, plus a +10% large-plot bonus) and points to
  `/settings` to change the profile it's compared against - previously a
  bare percentage with no explanation of where it came from.
- **Configurable scoring reference profile**: `ReferenceProfiles.DEFAULT`
  (`POZNAN_HOUSE_SEEKER`) was the only profile `DeterministicScorer` could
  ever compare against, hard-coded in Kotlin with no way to change it
  short of editing source. `UserPreferencesRepository` (a generic
  key-value store, `user_preferences` table, `V11__user_preferences.sql`)
  now persists a single JSON-encoded `ReferenceInvestmentProfile` under
  `key="scoring.profile"`; `effectiveScoringProfile()` returns the stored
  profile or falls back to `ReferenceProfiles.DEFAULT` when nothing has
  been saved yet - same "never silently skip scoring" rationale as
  `DefaultInvestmentAnalyzer`. Both `DefaultInvestmentAnalyzer` and
  `MonitoringService` (new-investment scoring and cross-source
  enrichment re-scoring) now read through this repository instead of
  `ReferenceProfiles.DEFAULT` directly. One global profile only (no
  multi-profile support) - YAGNI.
- **Immediate rescore-all, no live fetch**: `RescoreService.rescoreAll()`
  recomputes `investment_score` for every currently known investment
  against the current profile using only already-persisted facts (no
  source fetch, no validation, no correlation/dedup pass - deliberately
  narrower than `MonitoringService.scan()`). Activated via
  `--investment-monitor.mode=rescore`, which (via `@ConditionalOnProperty`)
  disables `ScanRunner` and enables `RescoreRunner` instead - the two are
  mutually exclusive on a single `bootRun` invocation. The frontend's
  `PUT /api/preferences` (saves the profile, then shells out to
  `./gradlew bootRun --args=--investment-monitor.mode=rescore` via
  `lib/rescore.ts`, same `child_process` pattern as `/api/scan`) and a
  standalone `POST /api/rescore` both trigger this, so a user editing
  `/settings` sees updated scores without waiting for or triggering a full
  scan.
- **`/settings` page**: property-type/location-tier toggles, house/plot
  area and price min-max inputs, and a large-plot-preferred switch,
  backed by `GET/PUT /api/preferences`. `frontend/lib/types.ts` gained
  `ScoringProfile` and `DEFAULT_SCORING_PROFILE` (a client-safe mirror of
  `ReferenceProfiles.POZNAN_HOUSE_SEEKER` - deliberately not in
  `lib/queries.ts`, which pulls in `node:sqlite` and would break the
  client bundle if imported from a `"use client"` component).

## Implemented (phase 11, Playwright-enabled sources + RynekPierwotny scope change + UX polish)

- **8 new sources unblocked by ADR-007's opt-in `PlaywrightPageFetcher`**:
  5 discovery (Buk, Szamotuły, Pobiedziska, Kórnik, Dopiewo) and 3
  developer (Archicom, PWD Deweloper, Nickel Development) - bringing the
  totals to 12 discovery sources and 29 developer sources. Nickel turned
  out not to actually need the browser fetcher once queried correctly
  (plain `JsoupPageFetcher` suffices); the other 7 do. Oborniki remains
  `BLOCKED` (its real WZ register is PDF-only) and Otodom remains
  unimplemented (reachable via Playwright but no parser built yet) - see
  `docs/SOURCES.md`.
- **RynekPierwotny scope change**: switched from `nowe-domy-poznan` (no
  room filter, Poznań city only) to `nowe-domy-wielkopolskie-liczba-pokoi-od-4`
  (4+ rooms, whole Wielkopolskie voivodeship), matching an explicit user
  preference for larger houses. `RynekPierwotnySource.fetch()` now
  paginates (`?page=N`), stopping on an empty page or a page containing
  only already-seen offers - this site's pagination was found to behave
  inconsistently for this route (an empty page 2, followed by page 3
  silently repeating page 1), so both stop conditions exist specifically
  to make that safe.
- **Location filter consolidation**: `/investments`' location dropdown
  previously listed every village/neighborhood name separately (e.g.
  Jasin, Gruszczyn, Bogucin all distinct, fragmenting a single gmina).
  `frontend/lib/location-groups.ts` maps every known location to its
  parent gmina; the filter now normalizes both its options and its
  predicate to gmina level via a case-insensitive substring match
  (mirroring `LocationCatalog.findIn`) - real `investment.location`
  values are rarely bare catalog names (e.g. "Swarzędz – Jasin",
  "Komorniki ul. Młyńska"), so an exact-match lookup alone would have
  left almost every real investment unnormalized.
- **Non-blocking scan progress**: `POST /api/scan` switched from
  `execFile` (blocks until the whole `bootRun` process exits, up to 120s)
  to `spawn`, returning `202` immediately and updating a shared in-memory
  `ScanState` as it parses `bootRun`'s stdout for a new explicit
  `"Scanning source [n/total]: 'id'"` progress line added to
  `MonitoringService.scan()`. `GET /api/scan/progress` exposes that
  state; `ScanButton` and the new `ScanProgress` sidebar component poll
  it independently via `useScanPoll`/`useScanFinishEffect` - neither
  depends on the other existing.
- **`npm start`** (root `package.json` + `scripts/start.mjs`):
  single-command launcher - starts the frontend dev server, waits for it
  to actually serve requests, then triggers a scan via the same
  non-blocking endpoint above. Dependency-free (no `concurrently`, no
  root `node_modules`), relies only on Node's built-ins already required
  for `node:sqlite`. `./gradlew bootRun` and `cd frontend && npm run dev`
  continue to work independently for manual control.
