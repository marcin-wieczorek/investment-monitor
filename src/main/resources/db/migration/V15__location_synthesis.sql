-- Location-intelligence synthesis (see docs/ARCHITECTURE.md phase 12 and
-- analysis/LocationActivityCollector.kt): per-location and region-wide
-- LLM-assisted (with deterministic fallback) summaries of discovery-signal
-- and investment activity, generated once per scan.
--
-- No FK to `investment`/`investment_signal`: a synthesis describes an
-- aggregate over many rows across both tables, not a single one, and must
-- remain queryable even after the underlying investments/signals that
-- contributed to it have been superseded or removed on a later scan.
CREATE TABLE location_synthesis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    location TEXT NOT NULL,
    municipality TEXT,
    development_trend TEXT NOT NULL,
    summary TEXT NOT NULL,
    estimated_timeline TEXT,
    key_developers TEXT NOT NULL DEFAULT '[]',
    opportunities TEXT NOT NULL DEFAULT '[]',
    risks TEXT NOT NULL DEFAULT '[]',
    recommended_action TEXT NOT NULL,
    reason TEXT NOT NULL,
    signal_count INTEGER NOT NULL,
    investment_count INTEGER NOT NULL,
    average_lead_time_days REAL,
    synthesized_at TEXT NOT NULL,
    UNIQUE (location)
);

-- One row per scan: the latest region-wide comparison always replaces the
-- previous one (upsert on a fixed synthetic key), since only "the current
-- hotspot ranking" is ever meaningful, not a history of past rankings.
CREATE TABLE hotspot_synthesis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hotspots TEXT NOT NULL,
    emerging_areas TEXT NOT NULL DEFAULT '[]',
    summary TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    synthesized_at TEXT NOT NULL
);
