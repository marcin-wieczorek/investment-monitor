# Source parser standard

For every real developer source:

1. Capture representative current HTML as a fixture.
2. Implement a source-specific adapter/parser.
3. Test every mapped field, including plot area and price where published.
4. Add negative/edge-case fixtures.
5. Add validation expectations for count and field coverage.
6. Keep the last trusted snapshot when validation fails.
7. Add a regression fixture for every production parsing bug.

Do not ship guessed CSS selectors as production parsing rules.

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
