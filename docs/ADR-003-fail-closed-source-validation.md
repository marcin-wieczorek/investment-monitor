# ADR-003: Fail closed

## Status

Accepted

## Context

Developer websites change their HTML without notice. A parser that
silently breaks (e.g. after a CSS class rename) can return a suspiciously
short list - or none at all - while still returning HTTP 200 and no
exception. If that broken result were treated the same as a real scrape,
the tool would either:

- report every previously-known investment as newly "removed", or
- (worse) quietly commit an empty/partial snapshot as the new trusted
  baseline, after which the *next* correct scrape would look like a wave
  of brand-new investments.

Either failure mode defeats the tool's purpose: the person relying on it
would either get spammed with false signals or, worse, stop trusting it
and miss a real new investment later.

## Decision

A source result that looks suspicious is rejected outright and never
replaces the last trusted snapshot:

- `SourceValidator` rejects a result if any investment has a blank
  name/URL, if the investment count dropped more than the configured
  threshold (default 50%) versus the last trusted count, or if the result
  is empty (including on a source's very first scrape - an empty first
  result is not treated as "the trusted baseline is zero investments").
- `MonitoringService` only calls `commit(...)` when both the fetch
  succeeded *and* validation passed. On any failure, the previous
  `investment` rows and `source_snapshot` are left untouched, and the scan
  report explicitly shows the source as failed with a reason.

This is deliberately **fail closed**: when unsure whether a result is
trustworthy, keep the old state rather than risk corrupting it.

## Consequences

**Gained:**
- A broken parser produces a visible, actionable failure in the scan
  report instead of silent data corruption.
- The trusted snapshot can only move forward when there's reasonable
  confidence the new data is real.
- Detail-page enrichment failures (see `InvestmentDetailEnricher`) follow
  the same philosophy at a smaller scale: a failed enrichment leaves the
  investment's list-page fields intact rather than blocking the scan.

**Traded away:**
- A source legitimately dropping in size by more than the threshold (e.g.
  a developer selling out and delisting many investments at once) will be
  flagged as a validation failure requiring manual review, even though
  nothing is actually broken. The threshold is configurable
  (`investment-monitor.validation.max-investment-drop-percentage`) for
  exactly this reason.
- A scan that fails validation for every source produces no committed
  data at all for that run - the tool does not attempt a partial commit
  of "the sources that looked fine."
