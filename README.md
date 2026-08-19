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
        -> correlate -> analyze (new only) -> report -> persist
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
- **Analyze** — a numeric `DeterministicScorer` compares an investment
  against a `ReferenceInvestmentProfile` and `LocationProfile`. An
  optional local LLM (Ollama) adds qualitative interpretation
  (priority/reasoning) on top, with a fully deterministic fallback when
  it's unavailable or misconfigured.

## Business scope

The target is residential development in the Poznań metropolitan area,
prioritizing terraced/semi-detached/detached houses, small developments,
and **large plots as an explicit positive feature** — never an automatic
rejection. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the
full scope and the extensible location-profile model.

## Monitored sources

| Category | Source | Area | Notes |
|---|---|---|---|
| Developer | [Chronos Development](https://www.chronos.poznan.pl) | Poznań area | Each investment lives on its own external domain |
| Developer | [Greenbud Development](https://www.greenbud.com.pl) | Swarzędz / Poznań area | Publishes location and house/plot area directly on the list page |
| Discovery | Gmina Swarzędz BIP — zoning conditions ("warunki zabudowy") | Gmina Swarzędz | Real, verified municipal register; see `docs/DISCOVERY.md` |
| Aggregator | [RynekPierwotny.pl](https://www.rynekpierwotny.pl) — new houses, Poznań | Poznań metro | Completeness/cross-check only, never primary identity |

Several other discovery/aggregator candidates were investigated and found
**not currently implementable without either fake selectors or a headless
browser** (Kleszczewo/Komorniki BIP, Otodom) — see `docs/DISCOVERY.md` and
`docs/SOURCES.md` for exactly why, and what would be needed to add them.

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
correlations, source health (by category), run history, and trigger a
scan from the browser. Dark/light mode and an English/Polish language
toggle are built in.

Requires Node 22.5+.

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

See [`frontend/README.md`](frontend/README.md) for full setup and details.

## Project status

- **Done** — Chronos + Greenbud developer parsers; one verified discovery
  source (Gmina Swarzędz zoning-conditions register) and one verified
  aggregator source (RynekPierwotny); deterministic diff for both
  investments and signals; fail-closed validation; deterministic
  cross-source correlation; provenance/evidence tracking; raw HTML
  archival with retention; deterministic reference-profile scoring with
  explicit large-plot handling; local Ollama LLM integration with
  graceful fallback; a Next.js dashboard covering all of the above.
- **Explicitly not done** — Kleszczewo/Komorniki BIP discovery (blocked
  by client-side rendering / anti-bot measures in this environment, not
  by lack of effort — see `docs/DISCOVERY.md`), Otodom aggregator
  (requires JS execution, deliberately not added to avoid a headless
  browser dependency), per-field provenance (currently evidence is
  recorded per investment/signal, not per individual fact).

## License

[MIT](LICENSE)
