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
not guessed. Re-verified in full (not just spot-checked) as part of the
"additional discovery sources" deferred-work session; findings updated
where the underlying site changed:

| Source | Category | Finding |
|---|---|---|
| Buk BIP, Oborniki BIP (now `bip.umoborniki.nv.pl`), Pobiedziska BIP, Szamotuły BIP | Discovery | Re-verified: HTTP 200 but an empty response body - still effectively a JS SPA shell with no server-rendered content. |
| Komorniki BIP (`bip2.komorniki.pl`) | Discovery | Re-verified: now returns HTTP 403 (was 429) - still a WAF/anti-bot block. The archival BIP has a real WZ register but is explicitly marked archival. |
| Luboń BIP | Discovery | Re-verified: now returns HTTP 403 (was 429) - still a WAF/anti-bot block. |
| Kostrzyn BIP, Rokietnica BIP | Discovery | Re-verified: consistent transport/DNS errors on both HTTP and HTTPS. |
| Kórnik BIP | Discovery | Re-verified: still a JS-hydrated Drupal 11 site - both the planning page and the obwieszczenia list return an effectively empty server-rendered shell. |
| Mosina BIP | Discovery | Re-verified: `bip.mosina.pl` is now just a directory of subsite tiles; the real BIP at `bip.um.mosina.pl` is server-rendered but its "Planowanie przestrzenne" section only has MPZP/studium static pages, no obwieszczenia/case register. |
| Puszczykowo BIP | Discovery | Re-verified: migrated to a WOKISS-hosted BIP with a real, server-rendered "Postępowania administracyjne" register - but every entry found so far is public-purpose infrastructure (water/sewer network), not residential warunki zabudowy, and it publishes no per-item date. |
| Kleszczewo BIP, Dopiewo BIP, Skoki BIP | Discovery | Re-verified: all three migrated off the old Nefeni (`nowoczesnagmina.pl`) JS SPA to a real, server-rendered Next.js BIP platform (`bip-api.{municipality}.pl`). Dopiewo has a dedicated "Decyzje o warunkach zabudowy" category but its article list isn't in the server-rendered HTML (client-side-fetched from its API); Skoki has a real combined celu-publiczne/warunki-zabudowy register but currently only one (non-residential) entry; Kleszczewo's "Obwieszczenia i ogłoszenia" category only surfaced non-residential notices this session. Worth re-checking Dopiewo/Skoki again later - they're the closest to being real. |
| Stęszew BIP | Discovery | Re-verified: reachable again (no longer a transport error) with a real "Zagospodarowanie Przestrzenne" section, but it only links to MPZP/studium/environmental pages - no obwieszczenia or case register found. |
| Otodom | Aggregator | Modern client-side-rendered listing; would require a headless browser (Selenium/Playwright) to read reliably, which this project deliberately avoids as a dependency for a local-first CLI tool. |
| PWD Deweloper (`pwd.com.pl`) | Developer | JavaScript fingerprinting (FingerprintJS) anti-bot protection serves only a JS-based redirect/challenge page; no content reachable without executing JS. |
| Archicom / Echo Residential (`archicom.pl`) | Developer | Client-side-rendered React/PWA storefront ("Oops! JavaScript is disabled" with no fallback content). |
| Sovo Development | Developer | No working domain found (`sovodevelopment.pl` does not resolve; `sovo.pl` is an unrelated app). |
| Nickel Development (`nickel.com.pl`) | Developer | Homepage is a heterogeneous hero carousel mixing investments, blog posts and resort/hotel properties, mostly linking off-domain with no location/area/price data. The dedicated investment-listing page (`/pl/nowe-mieszkania-...`) is AJAX-hydrated (Yii `multipage.js`) - raw HTML only contains empty `class="loading"` navigation stubs, no real card content. |
| Villa, Budimex, Novaform, Cavallia, BTM, Constructa Plus, Virke, SGI, FB Antczak | Developer | No verifiable Poznań-area developer found under this name (wrong company, defunct/rebranded domain, unrelated business, or unreachable domain) - see `registry.DeveloperRegistry` for per-developer notes. |

If any of these become accessible in the future (Komorniki's WAF rules
change, Dopiewo's API becomes documented, etc.), implement them
following the same standard as `SwarzedzWzSource`/`RynekPierwotnySource`:
real fixture, real parser, real tests, `verifySources` passing.

## Implemented developer sources

Beyond `chronos`/`greenbud`, the following are implemented and verified
against live HTML (see `registry.DeveloperRegistry` for tier/status and
`FixtureCaptureCli`/`SourceVerificationCli` for the full list): `atal`,
`agrobex`, `spravia`, `duda`, `develia`, `jakon-inwest`, `robyg`, `linea`,
`murapol`, `ataner`, `konimpex`, `pekabex`, `ebf`, `ggw`, `jaksbud`, `uwi`,
`sagaris`, `cordia`, `ronson`, `sivanet`, `mj`, `area`,
`inwestycje_wielkopolski`, `vastbouw`. Several of these publish only a
subset of fields (no area/price on the list page, or - for
`jaksbud`/`uwi` - a single investment represented as an aggregated unit
table rather than a card list); the per-parser KDoc documents exactly
what each page publishes and why a field was deliberately left null.

Notes on the last seven (Phase E, all Tier B):

- `cordia`/`ronson` each publish a dedicated Poznań-only listing page
  (`/miasta/poznan/`, `/poznan/inwestycje/`); as of implementation both
  currently list exactly one active Poznań investment.
- `sivanet` is a single-investment one-page site (`Lechicka 65`), parsed
  as a fixed set of labelled `.atile` blocks rather than a card list.
- `mj` is a homepage hub linking out to each investment's own external
  domain (same pattern as `ggw`), currently listing three investments
  across three cities (one in Poznań).
- `area` parses the "Nasza oferta" (currently marketed) slider; as of
  implementation all four listed investments are on the coast - Area
  Development's one Poznań investment is a completed project shown
  separately under "Realizacje", so it won't appear via this adapter
  until a new Poznań investment enters active sales.
- `inwestycje_wielkopolski` parses the "Realizacje" (completed projects)
  page (11 investments, all in Poznań, all marked `SOLD_OUT`) rather than
  "W sprzedaży", which as of implementation only teases an unannounced
  upcoming project with no dedicated URL of its own.
- `vastbouw` publishes no plain-text investment name on its Poznań
  archive page - the name is derived from the detail-page URL slug, the
  same fallback `ChronosParser` uses.

## Implemented discovery sources


Beyond `swarzedz-wz`, six more municipal discovery sources are
implemented: `czerwonak-obwieszczenia` and `tarnowo-podgorne-wz` (identical
"Rekord BIP" CMS, share `RekordBipParser`), `suchy-las-npp` (Logonet CMS),
`poznan-ulicp` (City of Poznań's public-purpose siting register, a
custom CMS that also exposes an XML/JSON API worth migrating to in a
future revision), `srem-wz` (Gmina Śrem BIP - uniquely among these,
split one page per calendar year rather than a single evergreen feed, so
`SremWzSource` does a two-step fetch: find the current year's URL from
an index page, then fetch that page), and `murowana-goslina-obwieszczenia`
(Gmina Murowana Goślina BIP - a single evergreen feed mixing zoning-conditions
and public-purpose siting decisions, classified by keyword). See
`registry.DiscoverySourceRegistry` for the full per-municipality
investigation record, including documented reasons for every municipality
that is currently `BLOCKED` or `NOT_IMPLEMENTED`.

## `LocationCatalog.findIn` word-boundary bugfix

While implementing the two sources above, `LocationCatalog.findIn` was
found to silently fail to match most of its own catalog (`Poznań`,
`Śrem`, `Łowęcin`, ...) whenever a location name was adjacent to
punctuation/whitespace in real text - Java's `\b` word boundary is
ASCII-only (`[a-zA-Z0-9_]`) unless `UNICODE_CHARACTER_CLASS` is set, and
Polish diacritic letters aren't in that ASCII set. Fixed by replacing
`\b` with explicit `(?<![\p{L}\p{N}])`/`(?![\p{L}\p{N}])` Unicode
letter/digit lookarounds. This affects every call site - aggregator-only-discovery
location matching, developer-candidate municipality assignment, and every
discovery parser's `location` field - not just the two new sources.
