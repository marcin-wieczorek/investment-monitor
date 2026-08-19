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
| Kleszczewo BIP | Discovery | Client-side rendered (Next.js SPA) with no discoverable public JSON API; content only exists after JS execution. Would require reverse-engineering an undocumented private API. |
| Komorniki BIP (`bip2.komorniki.pl`) | Discovery | Server-rendered (verified: HTML + working RSS feed), but the specific "Obwieszczenia" (planning announcements) section actively blocks repeated automated requests (WAF/rate-limiting observed in testing). The general "Ogłoszenia" RSS feed that *is* accessible doesn't carry planning/zoning content. |
| Otodom | Aggregator | Modern client-side-rendered listing; would require a headless browser (Selenium/Playwright) to read reliably, which this project deliberately avoids as a dependency for a local-first CLI tool. |

If any of these become accessible in the future (Komorniki's WAF rules
change, Kleszczewo publishes a documented API, etc.), implement them
following the same standard as `SwarzedzWzSource`/`RynekPierwotnySource`:
real fixture, real parser, real tests, `verifySources` passing.
