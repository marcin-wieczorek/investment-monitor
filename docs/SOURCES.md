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
  execution where possible - `RynekPierwotnySource` works because its
  listing page is server-rendered. Sites that genuinely require JS can use
  the opt-in `PlaywrightPageFetcher` instead (see ADR-007,
  `investment-monitor.playwright.enabled`), which `BukObwieszczeniaSource`
  does - but this is heavier (real Chromium, disabled by default) and
  should stay the exception, not the default.
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
| Oborniki BIP (now `bip.umoborniki.nv.pl`) | Discovery | Fetchable via the opt-in `PlaywrightPageFetcher` (ADR-007) - unlike Buk/Pobiedziska/Szamotuły (same "Madkom SIDAS BIP" platform), its content renders fine once JS executes. But its real WZ register ("Rejestr wydanych decyzji o warunkach zabudowy", `m,262`) publishes one PDF attachment per calendar year, not one HTML entry per case like Buk's - would need PDF text extraction, a capability this project doesn't have. Its "Ogłoszenia / Obwieszczenia" category (`m,189`) exists but defaults to an empty view behind a "Pokaż archiwalne" (show archived) button requiring a real click interaction, not just navigation - not attempted. See `registry.DiscoverySourceRegistry` for the full note. |
| Komorniki BIP (`bip2.komorniki.pl`) | Discovery | Re-verified: now returns HTTP 403 (was 429) - still a WAF/anti-bot block. The archival BIP has a real WZ register but is explicitly marked archival. |
| Luboń BIP | Discovery | Re-verified: now returns HTTP 403 (was 429) - still a WAF/anti-bot block. |
| Kostrzyn BIP, Rokietnica BIP | Discovery | Re-verified: consistent transport/DNS errors on both HTTP and HTTPS. |
| Mosina BIP | Discovery | Re-verified: `bip.mosina.pl` is now just a directory of subsite tiles; the real BIP at `bip.um.mosina.pl` is server-rendered but its "Planowanie przestrzenne" section only has MPZP/studium static pages, no obwieszczenia/case register. |
| Puszczykowo BIP | Discovery | Re-verified: migrated to a WOKISS-hosted BIP with a real, server-rendered "Postępowania administracyjne" register - but every entry found so far is public-purpose infrastructure (water/sewer network), not residential warunki zabudowy, and it publishes no per-item date. |
| Kleszczewo BIP, Skoki BIP | Discovery | Re-verified: both migrated off the old Nefeni (`nowoczesnagmina.pl`) JS SPA to a real, server-rendered Next.js BIP platform (`bip-api.{municipality}.pl`). Skoki has a real combined celu-publiczne/warunki-zabudowy register but currently only one (non-residential) entry; Kleszczewo's "Obwieszczenia i ogłoszenia" category only surfaced non-residential notices this session. Worth re-checking again later. |
| Stęszew BIP | Discovery | Re-verified: reachable again (no longer a transport error) with a real "Zagospodarowanie Przestrzenne" section, but it only links to MPZP/studium/environmental pages - no obwieszczenia or case register found. |
| Otodom | Aggregator | Modern client-side-rendered listing; requires a headless browser to read reliably. Fetchable via the opt-in `PlaywrightPageFetcher` (ADR-007, disabled by default), but no parser has been built/verified yet. |
| Archicom / Echo Residential (`archicom.pl`) | Developer | **Now implemented** as `archicom` via `PlaywrightPageFetcher` (ADR-007) - see "Implemented developer sources" below. |
| Sovo Development | Developer | No working domain found (`sovodevelopment.pl` does not resolve; `sovo.pl` is an unrelated app). |
| Villa, Budimex, Novaform, Cavallia, BTM, Constructa Plus, Virke, SGI, FB Antczak | Developer | No verifiable Poznań-area developer found under this name (wrong company, defunct/rebranded domain, unrelated business, or unreachable domain) - see `registry.DeveloperRegistry` for per-developer notes. |

If any of these become accessible in the future (Komorniki's WAF rules
change, Kleszczewo's category gets a WZ subsection, etc.), implement them
following the same standard as `SwarzedzWzSource`/`RynekPierwotnySource`:
real fixture, real parser, real tests, `verifySources` passing.

Sources whose finding is a JS SPA / client-side-rendered shell (Otodom)
can be fetched via the opt-in
`PlaywrightPageFetcher` (see ADR-007, `investment-monitor.playwright.enabled`)
instead of `JsoupPageFetcher` - this unblocks *fetching* their HTML, but a
real parser (fixture-verified per the standard above) is still required
before it moves to `IMPLEMENTED`/`MONITORED`. Buk, Szamotuły, Pobiedziska,
Kórnik, Dopiewo (same or comparable BIP platforms) and Archicom have
already gone through this - see `BukObwieszczeniaSource`,
`SzamotulyUlicpSource`, `PobiedziskaKomunikatySource`,
`KornikObwieszczeniaSource`, `DopiewoWzSource`, `ArchicomSource` and their
parsers - as templates for Otodom. Sources blocked for other reasons (WAF/403,
DNS/transport failures, no content, PDF-only registers, interaction-gated
views, expired/parked domains) are not affected by this - see
ADR-007's "Alternatives considered"/scope discussion for why a headless
browser doesn't fix those.

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

`archicom` (Archicom / Echo Residential) is a further exception: a
client-side-rendered React/PWA, fetched via the opt-in
`PlaywrightPageFetcher` (see ADR-007) rather than plain `JsoupPageFetcher`.
Its Poznań listing page publishes only investment name, location and
thumbnail image - no price/area/units/property type - left `null` per the
same "no fake implementations" rule as every other parser.

`pwd` (PWD Deweloper) is implemented at its real, current domain
`pwd-mieszkania.pl` - the domain previously recorded in this registry,
`pwd.com.pl`, turned out to have expired and now resolves to an unrelated
domain-marketplace parking page. Also fetched via `PlaywrightPageFetcher`
(a Leaflet SVG site-plan whose per-unit popup HTML is present in the
initial page load, but the plan graphic itself needs JS). Same
per-unit-aggregation shape as `jaksbud`: each of its two currently-built
stages ("Etap I"/"Etap II") is one [Investment], with unit count and
house/plot area aggregated across every unit's site-plan popup; unit-level
sale status (sold/reserved/available) and price are not aggregated into a
single page-level value and are left `null`.

`nickel` (Nickel Development) turned out, on closer investigation, to
need neither a headless browser nor an interaction to reach real data -
the homepage and per-district "listing" pages genuinely only show
generic promotional tiles as originally found, but the site's own
apartment *search* page (`/pl/wyszukiwarka-mieszkan`) is a traditional
server-rendered Yii1/jQuery results grid, reachable with plain
`JsoupPageFetcher`. It publishes one row per unit (157 across 6 paginated
pages as of implementation) mixing Poznań-area residential investments
with seaside/mountain resort properties ("Nickel Resort & ..." -
excluded by name). `NickelParser` aggregates unit rows into one
`Investment` per investment name (same shape as `jaksbud`/`pwd`), using
the search page's own `id_loc[]` location-filter checkboxes to build a
real, unique, navigable per-investment URL (`?id_loc%5B%5D={id}`) -
essential since the aggregated investments would otherwise all share the
same unfiltered search URL and collide on canonical key (see
ADR-002). Location, property type and per-investment status are not
reliably published and are left `null`.

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

Status extraction (`InvestmentStatus`, widened with `LAST_UNITS`/
`READY_FOR_HANDOVER`/`UNDER_CONSTRUCTION` - see `docs/ARCHITECTURE.md`
phase 9): `agrobex` (`div.investment-block__status` - note the real
spelling is "obioru", not "odbioru" as on the other three, verified
against the live markup), `develia` (`div.investment-box__new-label`,
keyword-filtered so generic marketing badges like "Top inwestycja" stay
unmapped), `linea` (`div.investment-tag`), `jakon-inwest`
(`div.ribbon > p`) all publish a genuine per-card readiness label that
was previously parsed but discarded. `area` and `konimpex` were
investigated for the same thing and dropped: `area`'s apparent status
text is a constant category tag identical across every card (no
discriminating signal), and `konimpex`'s "Dostępne mieszkania" text lives
in an unrelated map-widget section of the page with no reliable link back
to a specific investment card - verified with BeautifulSoup against the
real fixture before writing (or not writing) any selector.

## Implemented discovery sources


Beyond `swarzedz-wz`, eleven more municipal discovery sources are
implemented: `czerwonak-obwieszczenia` and `tarnowo-podgorne-wz` (identical
"Rekord BIP" CMS, share `RekordBipParser`), `suchy-las-npp` (Logonet CMS),
`poznan-ulicp` (City of Poznań's public-purpose siting register, a
custom CMS that also exposes an XML/JSON API worth migrating to in a
future revision), `srem-wz` (Gmina Śrem BIP - uniquely among these,
split one page per calendar year rather than a single evergreen feed, so
`SremWzSource` does a two-step fetch: find the current year's URL from
an index page, then fetch that page), `murowana-goslina-obwieszczenia`
(Gmina Murowana Goślina BIP - a single evergreen feed mixing zoning-conditions
and public-purpose siting decisions, classified by keyword), and three
sources on the same "Madkom SIDAS BIP" React SPA platform, all fetched
via the opt-in `PlaywrightPageFetcher` (see ADR-007) and each a distinct
sub-pattern of that platform:
- `buk-obwieszczenia` (Gmina Buk) - split one page per calendar year like
  Śrem's, with every announcement's full description already inline as a
  file-attachment entry on that year's page.
- `szamotuly-ulicp` (Gmina Szamotuły) - the list page only publishes a
  generic per-row title; the real description only exists on each row's
  own article page, so `SzamotulyUlicpSource` does a per-announcement
  detail fetch (list + N articles), unlike every other discovery source.
- `pobiedziska-komunikaty` (Gmina Pobiedziska) - simplest of the three:
  the list page's title column already contains each announcement's full
  description, so a single fetch suffices, same shape as `RekordBipParser`.

Both Szamotuły's dedicated "Decyzje o warunkach zabudowy" category and
Pobiedziska's dedicated "Warunki zabudowy" category are currently empty
("Brak artykułów") - not implemented since there was nothing to verify a
parser against; both municipalities' registers are only reachable through
their broader "celu publicznego"/"Komunikaty" categories instead, which
is why every currently-observed signal from them is
`SignalType.LAND_DEVELOPMENT_SIGNAL` rather than `WZ_DECISION` (the
keyword classifier would still correctly tag a WZ-worded entry if one
appears).

Two more discovery sources round this out, both fetched via
`PlaywrightPageFetcher` (ADR-007) despite being on entirely different BIP
platforms from the Madkom one above:
- `kornik-obwieszczenia` (Gmina Kórnik, a Drupal 11 BIP) - split one page
  per calendar year like Śrem's/Buk's. Announcements are grouped into
  accordion sections by department; only the "Wydział Planowania
  Przestrzennego" (Spatial Planning) accordion is selected, skipping
  every unrelated department's content entirely rather than trying to
  classify it. Each entry publishes its real document date as free Polish
  text ("z dnia 4 grudnia 2025 r.") - parsed via an explicit Polish
  month-name map rather than falling back to `Instant.EPOCH`, since it's
  reliably present.
- `dopiewo-wz` (Gmina Dopiewo, a Next.js/Nefeni BIP) - also split one page
  per calendar year. The real content lives in a stable `#category` id
  (not a hashed/utility class), as `<a title="...">` links whose `title`
  attribute holds the full case text - more reliable than relying on the
  link's own visible text. No per-item date is published here at all
  (unlike Kórnik), so `detectedAt` falls back to `Instant.EPOCH`.

See `registry.DiscoverySourceRegistry` for the full
per-municipality investigation record, including documented reasons for
every municipality that is currently `BLOCKED` or `NOT_IMPLEMENTED`
(e.g. Oborniki - same platform, but PDF-only register and an
interaction-gated announcements view, see that registry entry for
detail).

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
