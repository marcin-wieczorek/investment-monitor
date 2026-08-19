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

- **Per-field provenance**: `SourceEvidence` is currently recorded once
  per investment/signal at commit time (field_name="investment"/"signal"),
  not once per individual fact (e.g. a separate evidence row for
  `plotArea` vs. `price`). Full per-field provenance would require
  carrying provenance metadata through every parser and was out of scope
  for this iteration without a much larger parser rewrite.
- **Signal-to-signal correlation**: only signal-to-investment correlation
  is implemented. Grouping multiple discovery signals that share the same
  case `reference` (e.g. different filing stages of the same zoning case)
  as "the same case" is possible client-side (the `reference` field is
  already present) but not yet a first-class `Correlation` type.
- **Discovery lead-time metric**: the data to compute it exists
  (`first_seen_at` per source category, joined via `Correlation`), but no
  dedicated report/query surfaces "detected N days before the developer
  published" yet.

## Not yet implemented

- Additional discovery sources beyond Gmina Swarzędz (Kleszczewo,
  Komorniki: real URLs identified, HTML inspected, but blocked by
  client-side rendering / anti-bot measures in this environment - see
  `docs/DISCOVERY.md` for the investigation and what would unblock them).
- Otodom aggregator (requires JS execution to read; deliberately not
  implemented with a headless browser to keep this a lightweight,
  local-first CLI tool - see `docs/DISCOVERY.md`).
- Additional detail parsers for other Chronos/Greenbud investment sites.
- Automated test coverage for `InvestmentDetailEnricher` and
  `PolishAreaFormat` edge cases (still covered only indirectly).
