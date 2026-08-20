# Investment Monitor

A local-first Kotlin service that discovers new residential real-estate
investments in the Poznań metropolitan area - ideally *before* they show
up on aggregator portals like Otodom.

Instead of scraping developer websites and asking an LLM which ones look
good, the system continuously accumulates trustworthy evidence from three
independent source categories, deterministically detects what's new or
changed, correlates that evidence, and uses a local LLM only to interpret
and rank the result against an investment profile. Identity, diffing,
validation and persistence are all deterministic; the LLM never decides
what is new.

## Quick start

Requires JDK 21 and Node 22.5+ (see [`Getting started`](#getting-started) /
[`Frontend`](#frontend) below for details on each piece separately).

```bash
npm start
```

Starts the frontend dev server (installing its dependencies on first run)
and, once it's serving requests, automatically triggers a scan via the
same non-blocking endpoint the sidebar's "Run scan" button uses - so the
progress bar in the sidebar starts moving right away. Open
`http://localhost:3000`, `Ctrl+C` to stop. This is purely a convenience
wrapper (`scripts/start.mjs`, no extra dependencies) - `./gradlew bootRun`
and `cd frontend && npm run dev` still work exactly as before and can be
run separately if you'd rather control each piece yourself.

## Source categories

```
DEVELOPER sources     -> a developer's own site (ground truth for its investments)
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

- **Fetch & parse** - each source gets its own adapter + parser (Jsoup-
  based). No shared "universal" scraper; every site is different and
  selectors are verified against real, captured HTML.
- **Validate** - a result that looks broken (e.g. investment count
  suddenly drops, or an empty result) is rejected. The last trusted
  snapshot is never overwritten by a suspicious scrape (fail-closed).
- **Diff** - investments and discovery signals are identified by a
  canonical key (`source:normalized-url`), and classified deterministically
  (`NEW` / `CHANGED` / `UNCHANGED` / `REMOVED` for investments; new-or-not
  for signals) - never by a model's judgment call.
- **Enrich** - some investments publish their own dedicated page (often on
  a completely different domain than the developer's site). A generic
  `InvestmentDetailParser` mechanism matches a parser to an investment by
  URL, not by developer, and fills in fields like unit count or plot size.
- **Correlate** - a deterministic, feature-based matcher
  (`InvestmentCorrelator`) links discovery signals to investments that
  likely describe the same project (same location, developer name
  mentioned, ...) - never LLM-driven.
- **Deduplicate** - a second deterministic matcher (`InvestmentDeduplicator`)
  links investments from *different* sources that describe the same
  project (e.g. a developer's own listing and its RynekPierwotny
  aggregator listing) - never merges rows from the same source, never
  guesses across a weak name-overlap-only match (HIGH confidence only
  triggers cross-source enrichment; MEDIUM/LOW are shown but never
  auto-merged).
- **Cross-source enrich** - for HIGH-confidence duplicate pairs, missing
  facts (price, area, property type) are borrowed from the sibling
  investment on the other source, with full provenance recorded, and the
  score is recomputed immediately. A developer's own published fact is
  never overwritten by a borrowed one.
- **Analyze** - a numeric `DeterministicScorer` compares an investment
  against a `ReferenceInvestmentProfile` and `LocationProfile`, and always
  runs (and is persisted, and shown in the dashboard) even with no LLM
  configured. A local LLM (Ollama, enabled by default) adds qualitative
  interpretation (priority/reasoning) on top, with a fully deterministic
  fallback when it's unavailable, disabled, or misconfigured.

Discovery lead time - how many days before a developer publishes an
investment the system already had an official/public signal for it - is
computed for every correlation and shown both in the console report and
on `/correlations`/the dashboard (see `docs/ARCHITECTURE.md` phase 6).

Once per scan, active locations also get an LLM-assisted (or
deterministic-fallback) synthesis of their recent discovery-signal and
investment activity, plus a region-wide ranking of the most dynamically
developing areas - see `/locations` and `docs/ARCHITECTURE.md` phase 12.

## Business scope

The target is residential development in the Poznań metropolitan area,
prioritizing terraced/semi-detached/detached houses, small developments,
and **large plots as an explicit positive feature** - never an automatic
rejection. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the
full scope and the extensible location-profile model.

## Monitored sources

### Developers (29)

| Developer | URL | Tier |
|---|---|---|
| [Agrobex](https://www.agrobex.pl) | `agrobex.pl` | A |
| [Archicom](https://archicom.pl) | `archicom.pl` | A |
| [Area Development](https://areadevelopment.pl) | `areadevelopment.pl` | B |
| [ATAL](https://atal.pl) | `atal.pl` | A |
| [Ataner](https://www.ataner.pl) | `ataner.pl` | A |
| [Chronos Development](https://www.chronos.poznan.pl) | `chronos.poznan.pl` | A |
| [Cordia](https://cordiapolska.pl) | `cordiapolska.pl` | B |
| [Develia](https://develia.pl) | `develia.pl` | A |
| [Duda Development](https://dudadevelopment.pl) | `dudadevelopment.pl` | A |
| [EBF Development](https://ebfdevelopment.pl) | `ebfdevelopment.pl` | B |
| [GGW Development](https://ggwdevelopment.pl) | `ggwdevelopment.pl` | B |
| [Greenbud Development](https://www.greenbud.com.pl) | `greenbud.com.pl` | A |
| [Inwestycje Wielkopolski](https://inwestycjewielkopolski.pl) | `inwestycjewielkopolski.pl` | B |
| [Jakon](https://www.jakon-inwest.pl) | `jakon-inwest.pl` | A |
| [JaksBud](https://jaksbud.pl) | `jaksbud.pl` | B |
| [Konimpex-Invest](https://www.konimpex-invest.pl) | `konimpex-invest.pl` | A |
| [Linea](https://linea-deweloper.pl) | `linea-deweloper.pl` | A |
| [MJ Deweloper](https://mjdeweloper.pl) | `mjdeweloper.pl` | B |
| [Murapol](https://murapol.pl) | `murapol.pl` | A |
| [Nickel Development](https://www.nickel.com.pl) | `nickel.com.pl` | A |
| [Pekabex Development](https://pekabexdevelopment.com) | `pekabexdevelopment.com` | A |
| [PWD Deweloper](https://pwd-mieszkania.pl) | `pwd-mieszkania.pl` | A |
| [ROBYG](https://robyg.pl) | `robyg.pl` | A |
| [Ronson](https://ronson.pl) | `ronson.pl` | B |
| [Sagaris](https://sagaris.pl) | `sagaris.pl` | B |
| [SIVANET](https://sivanet.pl) | `sivanet.pl` | B |
| [Spravia](https://spravia.pl) | `spravia.pl` | B |
| [UWI](https://uwi.com.pl) | `uwi.com.pl` | A |
| [Vastbouw](https://vastbouw.pl) | `vastbouw.pl` | B |

See `registry/DeveloperRegistry.kt` for the full priority list (Tier A/B),
status of every developer investigated, and additional candidates not yet
implementable.

### Discovery - BIP (Biuletyn Informacji Publicznej) registers (12)

| Municipality | Register type | URL |
|---|---|---|
| [Buk](https://bip.buk.gmina.pl/m,1745,obwieszczenia-i-komunikaty.html) | Obwieszczenia | `bip.buk.gmina.pl` |
| [Czerwonak](https://bip.czerwonak.pl/6469) | Obwieszczenia | `bip.czerwonak.pl` |
| [Dopiewo](https://bip.dopiewo.pl/kategorie/125-decyzje-o-warunkach-zabudowy) | WZ | `bip.dopiewo.pl` |
| [Kórnik](https://bip.kornik.pl/obwieszczenia-i-ogloszenia) | Obwieszczenia | `bip.kornik.pl` |
| [Murowana Goślina](https://bip.murowana-goslina.pl/wiadomosci/9179/lista/1/obwieszczenia_inne) | Obwieszczenia | `bip.murowana-goslina.pl` |
| [Pobiedziska](https://bip.pobiedziska.pl/m,150,komunikaty.html) | Komunikaty | `bip.pobiedziska.pl` |
| [Poznań](https://bip.poznan.pl/bip/news/obwieszczenia-dotyczace-postepowan-o-ustalenie-lokalizacji-inwestycji-celu-publicznego-19,c,8440/) | ULICP | `bip.poznan.pl` |
| [Śrem](http://bip.srem.pl/public/?id=73563) | WZ | `bip.srem.pl` |
| [Suchy Las](https://bip.suchylas.pl/artykuly/planowanie-i-zagospodarowanie-przestrzenne-obwieszczenia-npp) | NPP | `bip.suchylas.pl` |
| [Swarzędz](https://bip.swarzedz.pl/index.php?id=344) | WZ | `bip.swarzedz.pl` |
| [Szamotuły](https://bip.szamotuly.pl/m,2101,obwieszczenia-wszczecie-postepowania.html) | ULICP | `bip.szamotuly.pl` |
| [Tarnowo Podgórne](http://bip2.tarnowo-podgorne.pl/6037) | WZ | `bip2.tarnowo-podgorne.pl` |

See `registry/DiscoverySourceRegistry.kt` for full municipal coverage
investigation, including municipalities investigated but not yet
implementable.

### Aggregator (1)

| Source | URL | Scope |
|---|---|---|
| [RynekPierwotny.pl](https://rynekpierwotny.pl) | `rynekpierwotny.pl` | New houses, 4+ rooms, Wielkopolskie voivodeship |

Completeness/cross-check only, never primary identity.

Developer and municipality registries (`registry/DeveloperRegistry.kt`,
`registry/MunicipalityRegistry.kt`) track **every** priority developer and
target municipality explicitly, whether or not a working source adapter
exists yet - see the `/developers` and `/coverage` dashboard pages.

Many other developer/discovery/aggregator candidates were investigated
and found **not currently implementable without either fake selectors or
a headless browser** (JS SPAs, anti-bot fingerprinting, AJAX-hydrated
listings, WAF-blocked BIPs, ...) - see `docs/SOURCES.md` "Investigated
but not implemented" for exactly why, and what would be needed to add
them.

Adding a new source means writing and fixture-testing a new parser - see
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
intentionally one-shot - schedule it with cron/systemd for recurring runs.

```bash
./gradlew test           # fixture-based tests, no network access
./gradlew verifySources   # live health-check of configured sources; never touches the trusted snapshot
./gradlew captureFixtures # fetches current HTML for review as a new/updated fixture
```

See [`docs/SOURCE-VERIFICATION.md`](docs/SOURCE-VERIFICATION.md) for the
full workflow of adding or fixing a parser after a site change.

### Local LLM (Ollama)

Analysis always works fully deterministically, LLM installed or not - if
Ollama isn't running, every call fails gracefully and falls back to a
deterministic score, so a fresh checkout runs `./gradlew bootRun`
successfully with zero LLM setup. LLM analysis is enabled by default; to
get qualitative interpretation (`priority`/`reason` text), install Ollama
and pull a model, see [`docs/LLM.md`](docs/LLM.md) for setup and model
recommendations. To skip the Ollama call attempt entirely, set
`investment-monitor.llm.enabled: false`.

### Optional: headless-browser fetching (Playwright)

Fetching works fully via plain HTTP (Jsoup) without any browser. Some
sources documented in `docs/SOURCES.md` "Investigated but not
implemented" return an empty/shell HTML body because their content is
rendered client-side (JS SPA, React, AJAX). For those, an opt-in
Playwright-based fetcher can be enabled (see ADR-007):

```bash
npx playwright install chromium   # one-time browser binary download
```

```yaml
investment-monitor:
  playwright:
    enabled: true
```

Disabled by default - a fresh checkout runs `./gradlew bootRun`
successfully with zero Playwright setup. Enabling it only changes how
HTML is *fetched* for a handful of registry-flagged hosts; a real,
fixture-verified parser is still required before any of those sources
counts as implemented.

## Frontend

A Next.js dashboard lives in [`frontend/`](frontend/) - it reads the same
SQLite database directly (no separate API layer, via Node's built-in
`node:sqlite`) to browse investments, discovery signals, cross-source
correlations, source health (by category), developer/geographic coverage,
run history, and trigger a scan from the browser. The investments list
shows a source-category badge per row, groups cross-source duplicates
under their most authoritative source, and has a collapsible filter panel
(source, property type, status, location, and range sliders for house
area/plot area/price) plus an always-visible sort control. A `/map` page
(Leaflet + OpenStreetMap, no API key) shows where every currently known
investment is located across the Poznań metro area, with an optional
development-activity overlay. A `/locations` page shows the LLM-assisted
(or deterministic-fallback) per-location synthesis and region-wide
development-hotspot ranking (see `docs/ARCHITECTURE.md` phase 12). A
`/settings` page lets you configure the scoring reference profile
(property types, location tiers, area/price ranges, large-plot
preference) - saving immediately recomputes every investment's score, no
new scan needed. Dark/light mode and an English/Polish language toggle
are built in.

Requires Node 22.5+.

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

See [`frontend/README.md`](frontend/README.md) for full setup and details.

## License

[AGPL-3.0](LICENSE)
