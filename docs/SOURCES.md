# Source standard

There are three source categories (see `docs/ARCHITECTURE.md`):
`InvestmentSource` (developer), `DiscoverySource` (discovery),
`AggregatorSource` (aggregator). Every real source, regardless of
category, follows the same discipline:

1. Capture representative current HTML as a fixture.
2. Implement a source-specific adapter/parser.
3. Test every mapped field, including plot area and price where published.
4. Add negative/edge-case fixtures.
5. Add validation expectations for count and field coverage.
6. Keep the last trusted snapshot when validation fails.
7. Add a regression fixture for every production parsing bug.

Do not ship guessed CSS selectors as production parsing rules. If a
source cannot be verified against real HTML (JS-rendered content behind
an undocumented API, active anti-bot blocking, ...), it must be
documented as **PLANNED**, not shipped as a working adapter (see
"Investigated but not implemented" below).

## Detail-page parsers

An investment may publish its own dedicated page, possibly on a domain
unrelated to the developer's list page. Support this generically:

1. Implement `InvestmentDetailParser` (matches an investment by URL/host via `supports()`).
2. Register it as a Spring bean; `InvestmentDetailEnricher` matches it automatically.
3. Verify the real page before writing selectors/regexes; add a fixture + test.
4. Leave fields unset rather than guessing when the page doesn't publish them.
5. A missing or failing detail parser must never fail a scan - enrichment is best-effort.

Detail parsers are matched by domain, not by owning developer, since one
developer's investments can each live on a different, independently
designed site (see `ChronosSource`/`TercjaDetailParser`).

## Discovery sources

See `docs/DISCOVERY.md` for the discovery-specific standard (signal
types, location extraction, municipal source investigation notes).

## Aggregator sources

Aggregators are a completeness/cross-check layer only (see
`docs/ARCHITECTURE.md` source precedence). When adding one:

- Prefer server-rendered listing pages over anything requiring JS
  execution - this project deliberately has no headless-browser
  dependency (`RynekPierwotnySource` works because its listing page is
  server-rendered; Otodom does not qualify for exactly this reason).
- Anchor selectors on stable attributes (`data-testid`, semantic HTML)
  rather than content-hashed CSS classes, which regenerate on every
  deploy of modern CSS-in-JS sites.
- Never write a field the page doesn't clearly and reliably publish -
  see `RynekPierwotnyParser`, which deliberately leaves `propertyType`,
  `units` and `status` unset.

## Investigated but not implemented

Documented explicitly per the project's "no fake implementations" rule -
these were investigated with real HTTP requests against the live sites,
not guessed:

| Source | Category | Finding |
|---|---|---|
| Kleszczewo BIP, Dopiewo BIP, Buk BIP, Oborniki BIP, Pobiedziska BIP, Szamotuły BIP, Skoki BIP | Discovery | All run the Nefeni (`nowoczesnagmina.pl`) JavaScript SPA platform with no discoverable public JSON API; content only exists after JS execution. |
| Komorniki BIP (`bip2.komorniki.pl`) | Discovery | Server-rendered (verified: HTML + working RSS feed), but the specific "Obwieszczenia" (planning announcements) section actively blocks repeated automated requests (WAF/rate-limiting observed in testing, HTTP 429). The archival BIP has a real WZ register but is explicitly marked archival. |
| Luboń BIP | Discovery | Returns HTTP 429 (rate limiting/WAF). |
| Kostrzyn BIP, Rokietnica BIP | Discovery | Consistent transport errors on both HTTP and HTTPS. |
| Stęszew BIP | Discovery | Only an archival BIP is reachable; the current BIP URL returns transport errors. |
| Kórnik BIP | Discovery | Planning page exists (Drupal 11) but obwieszczenia/WZ listing URLs return 404; needs further URL discovery before a parser can be built. |
| Śrem BIP | Discovery | Planning section only contains application forms/instructions, no register of issued decisions. |
| Mosina BIP | Discovery | BIP root page is a near-empty redirect stub; no discoverable WZ register. |
| Murowana Goślina BIP, Puszczykowo BIP | Discovery | SSR sites with planning sections, but no confirmed WZ/obwieszczenia register structure verified yet. |
| Otodom | Aggregator | Modern client-side-rendered listing; would require a headless browser (Selenium/Playwright) to read reliably, which this project deliberately avoids as a dependency for a local-first CLI tool. |
| PWD Deweloper (`pwd.com.pl`) | Developer | JavaScript fingerprinting (FingerprintJS) anti-bot protection serves only a JS-based redirect/challenge page; no content reachable without executing JS. |
| Archicom / Echo Residential (`archicom.pl`) | Developer | Client-side-rendered React/PWA storefront ("Oops! JavaScript is disabled" with no fallback content). |
| Sovo Development | Developer | No working domain found (`sovodevelopment.pl` does not resolve; `sovo.pl` is an unrelated app). |
| Nickel Development (`nickel.com.pl`) | Developer | Homepage is a heterogeneous hero carousel mixing investments, blog posts and resort/hotel properties, mostly linking off-domain with no location/area/price data. The dedicated investment-listing page (`/pl/nowe-mieszkania-...`) is AJAX-hydrated (Yii `multipage.js`) - raw HTML only contains empty `class="loading"` navigation stubs, no real card content. |
| Villa, Budimex, Novaform, Cavallia, BTM, Constructa Plus, Virke, SGI, FB Antczak | Developer | No verifiable Poznań-area developer found under this name (wrong company, defunct/rebranded domain, unrelated business, or unreachable domain) - see `registry.DeveloperRegistry` for per-developer notes. |

If any of these become accessible in the future (Komorniki's WAF rules
change, Kleszczewo publishes a documented API, etc.), implement them
following the same standard as `SwarzedzWzSource`/`RynekPierwotnySource`:
real fixture, real parser, real tests, `verifySources` passing.

## Implemented developer sources

Beyond `chronos`/`greenbud`, the following are implemented and verified
against live HTML (see `registry.DeveloperRegistry` for tier/status and
`FixtureCaptureCli`/`SourceVerificationCli` for the full list): `atal`,
`agrobex`, `spravia`, `duda`, `develia`, `jakon-inwest`, `robyg`, `linea`,
`murapol`, `ataner`, `konimpex`, `pekabex`, `ebf`, `ggw`, `jaksbud`, `uwi`,
`sagaris`. Several of these publish only a subset of fields (no area/price
on the list page, or - for `jaksbud`/`uwi` - a single investment
represented as an aggregated unit table rather than a card list); the
per-parser KDoc documents exactly what each page publishes and why a
field was deliberately left null.

## Implemented discovery sources

Beyond `swarzedz-wz`, four more municipal discovery sources are
implemented: `czerwonak-obwieszczenia` and `tarnowo-podgorne-wz` (identical
"Rekord BIP" CMS, share `RekordBipParser`), `suchy-las-npp` (Logonet CMS),
and `poznan-ulicp` (City of Poznań's public-purpose siting register, a
custom CMS that also exposes an XML/JSON API worth migrating to in a
future revision). See `registry.DiscoverySourceRegistry` for the full
per-municipality investigation record, including documented reasons for
every municipality that is currently `BLOCKED` or `NOT_IMPLEMENTED`.
