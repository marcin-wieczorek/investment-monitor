# ADR-005: Deterministic reference-profile scoring, LLM only for ranking

## Status

Accepted

## Context

The project previously had (informally) a "how similar is this to
Tercja" concept in mind for future LLM scoring. Two problems with letting
an LLM own that comparison directly:

1. **Non-determinism**: the same investment, scanned twice, could get a
   different numeric score from an LLM depending on model version,
   temperature, or phrasing - undermining the "deterministic core" the
   rest of the pipeline is built on (see ADR-002).
2. **Large plots**: a naive prompt ("is this attractive?") gives an LLM
   free rein to penalize large plots as "too expensive to maintain" or
   similar plausible-sounding but business-wrong reasoning, when the
   actual requirement is the opposite - a large plot should be a positive
   signal.

## Decision

- `ReferenceInvestmentProfile` (`domain/ReferenceInvestmentProfile.kt`)
  generalizes "similarity to Tercja" into an explicit, named, editable
  profile (preferred property types, location tiers, house/plot area
  ranges, price range, `largePlotPreferred`, max distance from Poznań).
  `analysis/ReferenceProfiles.kt` holds `POZNAN_HOUSE_SEEKER`, derived
  from Tercja's actual characteristics but not tied to that specific
  investment.
- `DeterministicScorer` (`analysis/DeterministicScorer.kt`) is the *only*
  place that produces `InvestmentAnalysis.investmentScore` /
  `.referenceProfileScore`. It's pure Kotlin: range-overlap scoring for
  house/plot area and price, exact-match scoring for property type and
  location tier, and an explicit bonus (not penalty) for a plot larger
  than the reference profile's preferred range.
- The LLM (`OllamaInvestmentAnalyzer`) receives the *result* of this
  scoring as context (via `InvestmentPromptBuilder`) and is only ever
  asked to produce `priority` and `reason` - ranking/interpretation, never
  the numeric score itself. Even the LLM's own `priority` field falls
  back to a deterministic threshold derived from `DeterministicScorer`'s
  score whenever the LLM is unavailable or its response is unusable.

## Consequences

**Gained:**
- Re-running a scan with unchanged inputs and no LLM configured produces
  byte-identical numeric scores - fully testable without network access
  (`DeterministicScorerTest`).
- Large plots are structurally rewarded (`ScoringResult.largePlotBonus`)
  rather than depending on an LLM being prompted carefully enough not to
  penalize them.
- Swapping the reference profile (e.g. targeting apartments instead of
  houses) is a data change (`ReferenceProfiles.kt`), not a prompt-engineering
  exercise.

**Traded away:**
- The deterministic scorer's *weighting* (property type 25%, location tier
  15%, plot area 25%, house area 20%, price 15%, see
  `DeterministicScorer.weightedAverage`) is a fixed heuristic, not learned
  or user-configurable via the UI. Changing it requires a code change.
  The reference *profile* those weights are applied against (property
  types, location tiers, area/price ranges, large-plot preference) is
  user-configurable via the `/settings` page and `UserPreferencesRepository`
  - saving immediately triggers a rescore of every known investment - but
  the weighting percentages themselves are not.
- The LLM's qualitative output (strongest positives, risks, missing
  information) is currently only stored in the `llm_analysis` cache table
  and not yet surfaced anywhere beyond `priority`/`reason` in
  `InvestmentAnalysis` - a reasonable follow-up once a UI need for the
  richer fields emerges.
