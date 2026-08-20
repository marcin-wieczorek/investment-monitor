# Architecture

```
sources (developer / discovery / aggregator)
    -> fetch -> parser -> validation -> deterministic diff
    -> enrich (new only) -> correlate -> analyze (new only, optional local LLM)
    -> report -> trusted snapshot
```

## Source categories

Three independent source categories, each with its own contract (see
`source/InvestmentSource.kt`, `source/DiscoverySource.kt`,
`source/AggregatorSource.kt`), collected by `SourceRegistry`:

- **Developer** (`InvestmentSource`) — a developer's own site. Ground
  truth for its own investments.
- **Discovery** (`DiscoverySource`) — official/public sources that reveal
  planned residential development before a marketable investment
  necessarily exists (municipal zoning-conditions registers, planning
  decisions, ...). Returns `InvestmentSignal`, not `Investment` — a
  signal is evidence, not a claim that a specific project exists (see
  `docs/DISCOVERY.md`).
- **Aggregator** (`AggregatorSource`) — third-party portals (RynekPierwotny,
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
  Swarzędz's real "warunki zabudowy" (zoning conditions) register —
  verified against live HTML, ~280 real decision documents, including
  genuine large-scale residential cases (e.g. a 224-house development in
  Kruszewnia). See `docs/DISCOVERY.md` for what else was investigated and
  why it isn't implemented yet.
- **Aggregator**: `RynekPierwotnySource` + `RynekPierwotnyParser` parse
  RynekPierwotny's server-rendered "new houses near Poznań" listing,
  anchored on the site's own `data-testid` attributes (stable across
  deploys) rather than its content-hashed CSS classes.
- **`InvestmentSignal`** (`domain/InvestmentSignal.kt`) models discovery
  evidence: municipality, location, `SignalType`, title, reference,
  detection date, source URL, raw facts. Identity follows the same
  `source:normalized-url` canonical-key scheme as `Investment`
  (see ADR-002), so the same deterministic-diff philosophy applies.
- **`SourceEvidence`** (`domain/SourceEvidence.kt`) is an append-only
  provenance record: which source produced a fact, when, from which URL,
  and how (`PARSER`/`LLM`/`MANUAL`). Recorded per investment/signal at
  commit time (not yet per individual field — see "Known scope
  limitations" below).
- **`InvestmentCorrelator`** (`correlation/InvestmentCorrelator.kt`)
  deterministically links discovery signals to investments: same
  recognized location (never the signal's own municipality — too coarse
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
  page fetch (via the existing `PageFetcher` interface — no source code
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
  bonus** — a plot larger than the reference profile's preferred range
  is rewarded, never penalized (see "Large plots" below).
- **Local LLM (Ollama)**: `OllamaClient` (plain JDK `HttpClient`, no
  Spring MVC dependency) + `OllamaInvestmentAnalyzer`
  (`ConditionalOnProperty`-gated on `investment-monitor.llm.enabled`,
  default `false`). The LLM only supplies `priority`/`reason` — the
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
This is domain data, not hard-coded parsing logic — new locations are
added to the catalog, not scattered across parsers.

## Known scope limitations (deliberately not built yet)

Signal-to-signal correlation (grouping filing stages of the same zoning
case by shared `reference`) is implemented client-side in `/signals`
(see "Implemented (phase 8, ...)" below) rather than as a first-class
`Correlation` type - a plain reference-equality group-by needs no
scan-time matching, unlike deterministic investment<->signal correlation.

## Not yet implemented

- Additional discovery sources beyond the eight municipalities implemented
  so far - see `registry/DiscoverySourceRegistry.kt` for the full,
  per-municipality investigation record (which BIPs are `BLOCKED`/
  `NOT_IMPLEMENTED` and why; Dopiewo and Skoki are the closest to real -
  both migrated to a real server-rendered platform but content wasn't
  parseable yet as of the last check).
- Otodom aggregator (requires JS execution to read; deliberately not
  implemented with a headless browser to keep this a lightweight,
  local-first CLI tool - see `docs/DISCOVERY.md`).
- Additional detail parsers for other Chronos/Greenbud investment sites.
- Automated test coverage for `InvestmentDetailEnricher` and
  `PolishAreaFormat` edge cases (still covered only indirectly).

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
  BIP investigation outcome (URL looked at, status, and - when blocked - a
  specific documented reason), which is more detail than
  `MunicipalityRegistry`'s coverage status alone and is essential context
  for continuing discovery-source work later.
- **16 new developer adapters** and **4 new discovery adapters** - see
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
- **25 developer adapters total**: the 7 remaining Tier B `CANDIDATE`
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
