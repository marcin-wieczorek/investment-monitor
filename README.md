# Investment Monitor

A local-first Kotlin service that watches Polish real estate developer
websites for new housing investments — often days or weeks before they
show up on aggregator portals like Otodom.

Instead of scraping everything and asking an LLM to figure out what
changed, the pipeline is deterministic by design: identity and change
detection are plain Kotlin, backed by a SQLite snapshot. An LLM (not yet
wired in) is meant to sit *after* detection, purely as an interpretation
layer — never as the source of truth for facts a parser already knows.

## How it works

```
sources -> fetch -> parse -> validate -> diff -> enrich (new only) -> analyze (new only) -> report -> persist
```

- **Fetch & parse** — each developer gets its own `InvestmentSource` +
  parser (Jsoup-based). No shared "universal" scraper; every site is
  different and selectors are verified against real, captured HTML.
- **Validate** — a result that looks broken (e.g. investment count
  suddenly drops) is rejected. The last trusted snapshot is never
  overwritten by a suspicious scrape (fail-closed).
- **Diff** — investments are identified by a canonical key
  (`source:normalized-url`), and classified as `NEW` / `CHANGED` /
  `UNCHANGED` deterministically, not by a model's judgment call.
- **Enrich** — some investments publish their own dedicated page (often on
  a completely different domain than the developer's site). A generic
  `InvestmentDetailParser` mechanism matches a parser to an investment by
  URL, not by developer, and fills in fields like unit count or plot size.
  This only runs once, for newly detected investments.
- **Analyze** — an `InvestmentAnalyzer` interface exists for scoring new
  investments against a reference profile. It currently ships with a
  no-op implementation that explicitly reports "not analyzed" — a real
  local LLM (Ollama + Qwen) is a planned next step, not a hidden
  dependency.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and the ADRs in
[`docs/`](docs/) for the reasoning behind these choices.

## Monitored sources

| Developer | Area | Notes |
|---|---|---|
| [Chronos Development](https://www.chronos.poznan.pl) | Poznań area | Each investment lives on its own external domain |
| [Greenbud Development](https://www.greenbud.com.pl) | Swarzędz / Poznań area | Publishes location and house/plot area directly on the list page |

Adding a new source means writing and fixture-testing a new parser — see
[`docs/SOURCES.md`](docs/SOURCES.md) for the checklist.

## Tech stack

Kotlin · Spring Boot · SQLite (via JDBC + Flyway) · Jsoup · Kotest/JUnit5

## Getting started

Requires JDK 21.

```bash
./gradlew bootRun
```

Runs a single scan end-to-end: fetch, validate, diff, enrich/analyze new
investments, print a report, persist, exit. It's intentionally one-shot —
schedule it with cron/systemd for recurring runs.

```bash
./gradlew test           # fixture-based parser tests, no network access
./gradlew verifySources   # live health-check of configured sources; never touches the trusted snapshot
./gradlew captureFixtures # fetches current HTML for review as a new/updated fixture
```

See [`docs/SOURCE-VERIFICATION.md`](docs/SOURCE-VERIFICATION.md) for the
full workflow of adding or fixing a parser after a site change.

## Project status

- **Done** — Chronos + Greenbud parsers, deterministic diff, fail-closed
  validation, SQLite persistence, one detail-page parser (Tercja), LLM
  interface with a no-op placeholder.
- **Not yet done** — a real local LLM wired into `InvestmentAnalyzer`,
  reference-profile scoring, more detail parsers, raw HTML archival.

## License

[MIT](LICENSE)
