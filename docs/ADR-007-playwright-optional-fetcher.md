# ADR-007: Optional Playwright fetcher for JS-rendered sources

## Status

Accepted

## Context

`docs/SOURCES.md` and `DiscoverySourceRegistry`/`DeveloperRegistry` track a
growing list of BIP registers and developer sites marked `BLOCKED` or
`NOT_IMPLEMENTED`. Analysis of that list shows four distinct root causes:

1. **JS SPA / client-side rendering** (~10 sources) - the server returns
   HTTP 200 with an empty or shell HTML body; all real content is
   injected by JavaScript after page load. Affects: Buk, Oborniki,
   Pobiedziska, Szamotuly (same BIP platform), Kornik (Drupal 11
   JS-hydrated), Dopiewo (Next.js, content fetched client-side from
   `bip-api.dopiewo.pl`), Archicom (React PWA), Nickel Development
   (Yii AJAX), Otodom (aggregator SPA).
2. **WAF / anti-bot blocking** (Komorniki, Luboń - HTTP 403; PWD
   Deweloper - FingerprintJS challenge page).
3. **DNS/transport failures** (Kostrzyn, Rokietnica) - the site simply
   doesn't resolve or connect.
4. **Content gap** (Mosina, Puszczykowo, Kleszczewo, Stęszew, Skoki) -
   reachable and server-rendered, but no usable WZ/case-register data
   exists to scrape.

`JsoupPageFetcher` (the only current `PageFetcher` implementation) is a
plain HTTP client + HTML parser - it cannot execute JavaScript, so
category 1 (the largest and most valuable group) is permanently
unreachable with the current infrastructure, regardless of parser
quality. Categories 2-4 would not be fixed by a headless browser alone
(2 is a coin flip depending on how the WAF fingerprints clients; 3 and 4
aren't fetching problems at all).

ADR-001 (local-first) established a strong bias against heavyweight
dependencies for a one-shot CLI tool. A full headless-browser dependency
(binary Chromium download, ~200-400 MB, higher RAM, slower per-page
fetch: seconds instead of milliseconds) is a real cost that must not be
imposed on the ~20 sources that already work fine with Jsoup.

## Decision

Add Playwright as an **optional, disabled-by-default** `PageFetcher`
implementation, selected transparently per-host by the existing
`ArchivingPageFetcher` decorator - no changes to any `*Source.kt` or
`*Parser.kt` class, and no change to the `PageFetcher` interface itself.

### 1. Dependency

`build.gradle.kts`:

```kotlin
implementation("com.microsoft.playwright:playwright:1.52.0")
```

Playwright's browser binaries are downloaded on demand (`playwright
install chromium`), not bundled in the JVM dependency itself - documented
as a one-time opt-in setup step in `README.md`, not a default part of
`./gradlew bootRun`.

### 2. `PlaywrightPageFetcher`

New class in `scraping/`, registered as a Spring bean only when enabled:

```kotlin
@Component
@ConditionalOnProperty("investment-monitor.playwright.enabled", havingValue = "true")
class PlaywrightPageFetcher(
    @Value("\${investment-monitor.playwright.timeout-ms:30000}") private val timeoutMs: Long
) : PageFetcher, AutoCloseable {

    private val playwright = Playwright.create()
    private val browser = playwright.chromium().launch(
        BrowserType.LaunchOptions().setHeadless(true)
    )

    override fun fetch(uri: URI): String {
        val page = browser.newPage()
        return try {
            page.navigate(uri.toString(), Page.NavigateOptions().setTimeout(timeoutMs.toDouble()))
            page.waitForLoadState(LoadState.NETWORKIDLE)
            page.content()
        } finally {
            page.close()
        }
    }

    override fun close() {
        browser.close()
        playwright.close()
    }
}
```

Same `@ConditionalOnProperty` pattern already used for
`OllamaInvestmentAnalyzer` vs `NoOpInvestmentAnalyzer` (ADR-006): when
disabled, the bean simply doesn't exist, no Chromium download is
triggered, and `./gradlew bootRun` keeps working with zero setup on a
fresh checkout.

### 3. Transparent per-host routing in `ArchivingPageFetcher`

`ArchivingPageFetcher` already wraps `JsoupPageFetcher` as the `@Primary`
`PageFetcher` bean. It gains an optional `PlaywrightPageFetcher` and a set
of hosts that require a real browser:

```kotlin
@Primary
@Component
class ArchivingPageFetcher(
    private val jsoup: JsoupPageFetcher,
    private val playwright: PlaywrightPageFetcher?,   // null when disabled
    private val archiver: RawHtmlArchiver,
    private val browserRequiredHosts: Set<String>      // sourced from the registries, see below
) : PageFetcher {

    override fun fetch(uri: URI): String {
        val delegate = if (uri.host in browserRequiredHosts && playwright != null) {
            playwright
        } else {
            jsoup
        }
        val html = delegate.fetch(uri)
        archiver.archive(uri.host ?: "unknown-host", html)
        return html
    }
}
```

This is the architecturally cleaner of two options considered (see
"Alternatives considered"): sources keep calling
`pageFetcher.fetch(uri)` exactly as before, with zero knowledge of *how*
the page is retrieved. Fetcher selection is purely an infrastructure
concern, consistent with `ArchivingPageFetcher` already being a
transparent decorator.

### 4. Marking sources as browser-required

No new YAML config (the project has no per-source YAML today - see
`AGENTS.md`). Instead, extend the existing registry data classes, which
already carry source metadata like `status` and `blockedReason`:

- `DiscoverySourceEntry` (`DiscoverySourceRegistry.kt`): add
  `requiresBrowser: Boolean = false`.
- `Developer` (`domain/Developer.kt`): add
  `requiresBrowser: Boolean = false`.

`browserRequiredHosts` is then derived at startup by collecting the
hosts of every registry entry with `requiresBrowser = true`, and injected
into `ArchivingPageFetcher` as a `@Bean` (e.g. from a small
`@Configuration` class), rather than hardcoded.

Sources whose `status` flips from `BLOCKED`/`NOT_IMPLEMENTED` to
`IMPLEMENTED` once a parser exists: Buk, Oborniki, Pobiedziska,
Szamotuly, Kornik, Dopiewo (discovery); Archicom, Nickel Development
(developer). PWD Deweloper is attempted but may remain `BLOCKED` if
FingerprintJS still rejects a headless browser. Otodom (aggregator) is
in scope but out of this ADR's immediate parser work.

Komorniki, Luboń (WAF 403), Kostrzyn, Rokietnica (DNS/transport), and the
five content-gap sources are explicitly **not** addressed by this
decision - a headless browser does not fix any of those root causes.

### 5. Configuration

`application.yml`:

```yaml
investment-monitor:
  playwright:
    enabled: false
    timeout-ms: 30000
```

### 6. New parsers

Each newly reachable source still needs a parser built and verified
against a real captured fixture, per the existing "Adding a new
developer/discovery source" workflow in `AGENTS.md` - Playwright only
solves *fetching*, not *parsing*. Suggested order (highest value /
lowest risk first):

1. Buk / Oborniki / Pobiedziska / Szamotuly - same BIP platform, one
   parser likely covers all four.
2. Dopiewo - dedicated WZ register, most valuable discovery source.
3. Archicom, Nickel Development - real Poznań-area developer inventory.
4. Kornik - Drupal 11, one-off parser.
5. PWD Deweloper - attempt last; may stay `BLOCKED` if FingerprintJS
   still blocks headless Chromium.

`FixtureCaptureCli` needs a way to capture via the browser fetcher for
these sources (e.g. instantiate `PlaywrightPageFetcher` directly, same
way it already instantiates sources manually rather than through Spring).

## Alternatives considered

**Explicit per-source fetcher selection** (`Source` constructors take a
`PageFetcherSelector` and call
`fetcherSelector.fetcherFor(requiresBrowser = true).fetch(uri)`) was
rejected in favor of the transparent host-based routing above: it would
require touching every new browser-dependent `*Source.kt` class and leaks
an infrastructure concern (how to fetch) into source implementations,
which today only know how to parse. Transparent routing in
`ArchivingPageFetcher` keeps that boundary intact and requires zero
changes to existing or new `*Source.kt` classes beyond registry metadata.

**Always-on Playwright** (replacing Jsoup entirely) was rejected: it
would impose the dependency, download, and per-fetch latency cost on all
~20 sources that work fine today, contradicting ADR-001.

## Consequences

**Gained:**
- Recovers the largest category of blocked sources (~10 of ~19) without
  touching any working source or parser.
- Zero cost for the majority of sources that don't need it - opt-in via
  registry metadata + a disabled-by-default flag.
- Consistent with the existing conditional-bean pattern from ADR-006.

**Traded away:**
- New optional heavyweight dependency (~200-400 MB Chromium download,
  higher RAM, seconds-not-milliseconds per fetch) for anyone who enables
  it - must be clearly documented as opt-in in `README.md`.
- `FixtureCaptureCli`/`SourceVerificationCli`, both plain `main()`
  entry points constructed manually (not Spring-managed), need explicit
  handling for browser-required sources rather than uniform Jsoup usage.
- Does not fix WAF (category 2), DNS/transport (category 3), or
  content-gap (category 4) sources - roughly half the currently blocked
  list stays blocked regardless of this change.
- CI/test environments must not depend on a real Chromium install;
  parser tests remain fixture-based (no network, no browser) per
  existing convention - `PlaywrightPageFetcher` itself would need at
  most a narrowly-scoped, opt-in integration test, never part of the
  default `./gradlew test` run.
