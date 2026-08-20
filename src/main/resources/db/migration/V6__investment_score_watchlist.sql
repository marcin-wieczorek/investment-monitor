-- Deterministic scoring, persisted so both the default (no-LLM) and
-- LLM-enhanced analysis paths leave behind an explainable, queryable score
-- rather than only a priority/reason string (see analysis.DeterministicScorer).
-- Keyed by canonical_key (not investment_id), matching llm_analysis: scoring
-- happens before a newly-detected investment is committed/assigned an id.
CREATE TABLE investment_score (
    investment_canonical_key TEXT NOT NULL UNIQUE,
    overall_score REAL NOT NULL,
    property_type_match INTEGER NOT NULL,
    location_tier_match INTEGER,
    house_area_score REAL,
    plot_area_score REAL,
    price_score REAL,
    large_plot_bonus INTEGER NOT NULL,
    plot_to_house_ratio REAL,
    scored_at TEXT NOT NULL
);

-- Watchlist: a frontend-only flag alongside the existing `archived` column,
-- same table/pattern as investment_state (see V3__user_state.sql).
ALTER TABLE investment_state ADD COLUMN watched INTEGER NOT NULL DEFAULT 0;
