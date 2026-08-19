# ADR-004: Three source categories, not one universal `InvestmentSource`

## Status

Accepted

## Context

The original brief asked for "scrape developer websites" only. The
expanded brief requires early detection *before* a developer publishes,
plus a completeness cross-check against aggregator portals. These are
fundamentally different kinds of evidence:

- A developer's own site is ground truth for its own investments, but by
  definition only exists once the developer has decided to market
  something.
- A municipal zoning-conditions register can reveal a 150-house
  development months before any developer publishes a sales page - but
  it never claims to know a project name, price, or unit count. It's
  evidence of planned construction, not a marketable investment.
- An aggregator portal (RynekPierwotny, Otodom) lists investments from
  many developers, but is inherently secondary - a developer or the
  municipality always knows about a project before a listings portal
  scrapes/republishes it.

Forcing all three into a single `InvestmentSource` (`fetch(): List<Investment>`)
would mean either fabricating investment names/prices for a zoning
decision (dishonest) or diluting the `Investment` domain model with
"maybe unknown" semantics everywhere.

## Decision

Three separate, minimal interfaces:

```kotlin
interface InvestmentSource   { fun fetch(): List<Investment> }        // developer
interface DiscoverySource    { fun fetch(): List<InvestmentSignal> }  // discovery
interface AggregatorSource   { fun fetch(): List<Investment> }        // aggregator
```

`SourceRegistry` collects all three categories from Spring's dependency
injection (auto-discovering `@Component` beans, same as before) and
exposes them separately. `MonitoringService` scans all three, but only
developer-sourced investments go through detail enrichment and (LLM)
analysis - aggregator investments are validated/diffed/persisted with the
same rigor but never enriched or ranked, since they're a cross-check
layer, not the primary target.

`Investment` and `InvestmentSignal` share the same canonical-key identity
scheme (`source:normalized-url`, see ADR-002) so both get the same
deterministic diffing, but they are deliberately different types - an
`InvestmentSignal` never silently becomes an `Investment`.
`InvestmentCorrelator` links the two explicitly and only when a
deterministic feature match (location, developer name) exists.

## Consequences

**Gained:**
- Existing developer sources (`ChronosSource`, `GreenbudSource`) required
  zero changes - `InvestmentSource` is untouched.
- A discovery signal can never be mistaken for a confirmed investment
  anywhere in the codebase - the type system enforces it.
- Each category can evolve its own validation/reporting rules
  independently (e.g. discovery sources don't have a meaningful
  "investment count drop" concept the way developer sources do).

**Traded away:**
- Some duplication between `scanInvestmentSource` (shared by developer
  and aggregator) and `scanDiscoverySource` in `MonitoringService` - judged
  acceptable since the two pipelines genuinely diverge (aggregator skips
  enrichment/analysis; discovery has no "changed" semantics, only
  new-or-not).
- `SourceRegistry` is a thin pass-through today; if source count grows
  much further, per-category sub-registries with their own validation
  policy objects may be worth extracting.
