# ADR-002: Deterministic diff

## Status

Accepted

## Context

The core value of this tool is catching *new* investments quickly and
correctly. An earlier design considered letting an LLM look at "yesterday's
list" and "today's list" and describe what changed. That is tempting
because it requires no explicit identity/comparison logic to write - but
it means the single most important signal the tool produces (is this
investment new?) would be a model's best guess rather than a fact.

LLMs are non-deterministic, can hallucinate, and give no guarantee that
"the same investment" is recognized as such across two runs (e.g. after a
minor URL or wording change on the developer's site).

## Decision

Investment identity and change detection are implemented in plain Kotlin,
never delegated to an LLM:

- **Identity**: `Investment.canonicalKey` (`source:normalized-url`),
  computed deterministically from the parsed URL.
- **Change classification**: `ChangeDetector` compares the current scrape
  against the last trusted snapshot by canonical key and classifies each
  investment as `NEW`, `CHANGED`, `UNCHANGED`, or `REMOVED` - a pure,
  testable function with no external calls.

An LLM (see `InvestmentAnalyzer`) is only ever invoked *after* this
classification, to interpret and score investments already known to be
`NEW`. It never decides what is new, and it is not treated as a second
source of truth for facts (price, area, location, ...) that the parser
already extracted.

## Consequences

**Gained:**
- Change detection is unit-testable and fully reproducible - the same
  input always produces the same classification.
- No risk of the tool missing a genuinely new investment because a model
  "decided" it looked similar to something already seen.
- The LLM's role stays clearly scoped (interpretation, not detection),
  which keeps prompt design simple and keeps a broken/absent LLM from
  affecting the tool's core guarantee.

**Traded away:**
- Adding a new source requires writing a canonical-key-compatible URL
  scheme up front; the tool can't "figure out" identity heuristically for
  a badly-structured source.
- Field-level nuance the deterministic comparison doesn't capture (e.g. "is
  this really a meaningfully different investment or just a typo fix?") is
  not distinguished - `CHANGED` is reported whenever any field differs by
  equality, with no semantic weighting.
