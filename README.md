# Investment Monitor

A local-first Kotlin service that discovers new residential real-estate
investments in the Poznań metropolitan area — ideally *before* they show
up on aggregator portals like Otodom.

Instead of scraping developer websites and asking an LLM which ones look
good, the system continuously accumulates trustworthy evidence from three
independent source categories, deterministically detects what's new or
changed, correlates that evidence, and uses a local LLM only to interpret
and rank the result against an investment profile. Identity, diffing,
validation and persistence are all deterministic; the LLM never decides
what is new.

## Source categories

```
DEVELOPER sources    -> a developer's own site (ground truth for its investments)
DISCOVERY sources     -> official/public evidence of planned development,
                         often before a marketable investment exists
                         (municipal zoning-conditions registers, ...)
AGGREGATOR sources    -> third-party portals, used only as a completeness/
                         cross-check layer, never as primary identity
```

Factual authority follows **developer > discovery > aggregator**. See
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full reasoning and
[`docs/SOURCES.md`](docs/SOURCES.md) / [`docs/DISCOVERY.md`](docs/DISCOVERY.md)
for what's actually implemented (vs. planned).

## How it works

```
sources -> fetch -> parse -> validate -> diff -> enrich (new only)
        -> correlate -> deduplicate -> cross-source enrich (HIGH-confidence only)
        -> analyze (new only) -> report -> persist
```

- **Fetch & parse** — each source gets its own adapter + parser (Jsoup-
  based). No shared "universal" scraper; every site is different and
  selectors are verified against real, captured HTML.
- **Validate** — a result that looks broken (e.g. investment count
  suddenly drops, or an empty result) is rejected. The last trusted
  snapshot is never overwritten by a suspicious scrape (fail-closed).
- **Diff** — investments and discovery signals are identified by a
  canonical key (`source:normalized-url`), and classified deterministically
  (`NEW` / `CHANGED` / `UNCHANGED` / `REMOVED` for investments; new-or-not
  for signals) — never by a model's judgment call.
- **Enrich** — some investments publish their own dedicated page (often on
  a completely different domain than the developer's site). A generic
  `InvestmentDetailParser` mechanism matches a parser to an investment by
  URL, not by developer, and fills in fields like unit count or plot size.
- **Correlate** — a deterministic, feature-based matcher
  (`InvestmentCorrelator`) links discovery signals to investments that
  likely describe the same project (same location, developer name
  mentioned, ...) — never LLM-driven.
- **Deduplicate** — a second deterministic matcher (`InvestmentDeduplicator`)
  links investments from *different* sources that describe the same
  project (e.g. a developer's own listing and its RynekPierwotny
  aggregator listing) — never merges rows from the same source, never
  guesses across a weak name-overlap-only match (HIGH confidence only
  triggers cross-source enrichment; MEDIUM/LOW are shown but never
  auto-merged).
- **Cross-source enrich** — for HIGH-confidence duplicate pairs, missing
  facts (price, area, property type) are borrowed from the sibling
  investment on the other source, with full provenance recorded, and the
  score is recomputed immediately. A developer's own published fact is
  never overwritten by a borrowed one.
- **Analyze** — a numeric `DeterministicScorer` compares an investment
  against a `ReferenceInvestmentProfile` and `LocationProfile`, and always
  runs (and is persisted, and shown in the dashboard) even with no LLM
  configured. An optional local LLM (Ollama) adds qualitative
  interpretation (priority/reasoning) on top, with a fully deterministic
  fallback when it's unavailable or misconfigured.

Discovery lead time — how many days before a developer publishes an
investment the system already had an official/public signal for it — is
computed for every correlation and shown both in the console report and
on `/correlations`/the dashboard (see `docs/ARCHITECTURE.md` phase 6).

## Business scope

The target is residential development in the Poznań metropolitan area,
prioritizing terraced/semi-detached/detached houses, small developments,
and **large plots as an explicit positive feature** — never an automatic
rejection. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the
full scope and the extensible location-profile model.

## Monitored sources

| Category | Source | Area | Notes |
|---|---|---|---|
| Developer | 26 developers (Chronos, Greenbud, ATAL, Agrobex, Spravia, Duda, Develia, Jakon, ROBYG, Linea, Murapol, Ataner, Konimpex, Pekabex, EBF, GGW, JaksBud, UWI, Sagaris, Sivanet, Cordia, Ronson, MJ, Area, Inwestycje Wielkopolski, Vastbouw) | Poznań metro area | See `registry/DeveloperRegistry.kt` for the full Tier A/B priority list, verified URLs, and status of every developer investigated |
| Discovery | Swarzędz, Czerwonak, Tarnowo Podgórne, Suchy Las, Poznań, Śrem, Murowana Goślina BIP registers | 7 municipalities | Zoning-conditions/planning-announcement registers; see `registry/DiscoverySourceRegistry.kt` for full municipal coverage investigation |
| Aggregator | [RynekPierwotny.pl](https://www.rynekpierwotny.pl) — new houses, Poznań | Poznań metro | Completeness/cross-check only, never primary identity |

Developer and municipality registries (`registry/DeveloperRegistry.kt`,
`registry/MunicipalityRegistry.kt`) track **every** priority developer and
target municipality explicitly, whether or not a working source adapter
exists yet — see the `/developers` and `/coverage` dashboard pages.

Many other developer/discovery/aggregator candidates were investigated
and found **not currently implementable without either fake selectors or
a headless browser** (JS SPAs, anti-bot fingerprinting, AJAX-hydrated
listings, WAF-blocked BIPs, ...) — see `docs/SOURCES.md` "Investigated
but not implemented" for exactly why, and what would be needed to add
them.

Adding a new source means writing and fixture-testing a new parser — see
[`docs/SOURCES.md`](docs/SOURCES.md) for the checklist.

## Tech stack

Kotlin · Spring Boot · SQLite (via JDBC + Flyway) · Jsoup · Kotest/JUnit5 ·
Ollama (optional local LLM, behind an interface)

## Getting started

Requires JDK 21.

```bash
./gradlew bootRun
```

Runs a single scan end-to-end: fetch, validate, diff, enrich new
investments, correlate, analyze, print a report, persist, exit. It's
intentionally one-shot — schedule it with cron/systemd for recurring runs.

```bash
./gradlew test           # fixture-based tests, no network access
./gradlew verifySources   # live health-check of configured sources; never touches the trusted snapshot
./gradlew captureFixtures # fetches current HTML for review as a new/updated fixture
```

See [`docs/SOURCE-VERIFICATION.md`](docs/SOURCE-VERIFICATION.md) for the
full workflow of adding or fixing a parser after a site change.

### Optional: local LLM (Ollama)

Analysis works fully deterministically without any LLM. To enable
qualitative interpretation, see [`docs/LLM.md`](docs/LLM.md) for local
Ollama setup and configuration.

## Frontend

A Next.js dashboard lives in [`frontend/`](frontend/) — it reads the same
SQLite database directly (no separate API layer, via Node's built-in
`node:sqlite`) to browse investments, discovery signals, cross-source
correlations, source health (by category), developer/geographic coverage,
run history, and trigger a scan from the browser. The investments list
shows a source-category badge per row, groups cross-source duplicates
under their most authoritative source, and has a collapsible filter panel
(source, property type, status, location, and range sliders for house
area/plot area/price) plus an always-visible sort control. A `/map` page
(Leaflet + OpenStreetMap, no API key) shows where every currently known
investment is located across the Poznań metro area. A `/settings` page
lets you configure the scoring reference profile (property types,
location tiers, area/price ranges, large-plot preference) - saving
immediately recomputes every investment's score, no new scan needed.
Dark/light mode and an English/Polish language toggle are built in.

Requires Node 22.5+.

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

See [`frontend/README.md`](frontend/README.md) for full setup and details.

## Project status

- **Done** — 26 verified developer parsers, 7 verified discovery sources
  (municipal zoning/planning registers) and one verified aggregator
  source (RynekPierwotny); explicit `DeveloperRegistry`/
  `MunicipalityRegistry`/`DiscoverySourceRegistry` tracking every priority
  developer and target municipality regardless of implementation status;
  developer-candidate discovery feedback loop from aggregator-only finds;
  deterministic diff for both investments and signals; fail-closed
  validation; deterministic cross-source correlation (signal<->investment)
  and deduplication (investment<->investment across sources, with
  HIGH-confidence cross-source enrichment); per-field provenance/evidence
  tracking; raw HTML archival with retention; deterministic
  reference-profile scoring that always runs and is persisted, with
  explicit large-plot handling, a data-completeness indicator, and a
  discovery-lead-time metric per correlation; a user-configurable scoring
  reference profile (`/settings`, persisted in SQLite, immediate rescore-all
  on save without a live-source scan); local Ollama LLM
  integration with graceful fallback; a Next.js dashboard covering all of
  the above plus developer/geographic coverage pages, a score/price-aware
  investment filter panel, a location map, and a watchlist.
- **Explicitly not done** — several BIPs and developer sites investigated
  and found technically unimplementable (JS SPAs, anti-bot fingerprinting,
  AJAX-hydrated listings, WAF-blocked BIPs — see `docs/SOURCES.md`),
  Otodom aggregator (requires JS execution, deliberately not added to
  avoid a headless browser dependency), per-investment detail-page
  enrichment beyond the single Tercja/Chronos case (most developer list
  pages don't publish price/plot area at all — see `docs/ARCHITECTURE.md`
  scoring completeness section).

## License

[MIT](LICENSE)
