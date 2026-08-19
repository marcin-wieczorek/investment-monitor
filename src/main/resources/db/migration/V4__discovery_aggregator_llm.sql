-- Discovery signals: evidence from official/public sources (BIP, zoning
-- registers, ...) that residential development is planned, before a
-- marketable investment necessarily exists. See docs/DISCOVERY.md.
CREATE TABLE investment_signal (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source TEXT NOT NULL,
    canonical_key TEXT NOT NULL UNIQUE,
    municipality TEXT NOT NULL,
    location TEXT,
    signal_type TEXT NOT NULL,
    title TEXT NOT NULL,
    reference TEXT,
    detected_at TEXT NOT NULL,
    url TEXT NOT NULL,
    raw_facts TEXT,
    first_seen_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL
);

-- Provenance: which source produced a fact, when, and how. Exactly one of
-- investment_id/signal_id is set per row.
CREATE TABLE source_evidence (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investment_id INTEGER REFERENCES investment(id),
    signal_id INTEGER REFERENCES investment_signal(id),
    source_id TEXT NOT NULL,
    source_category TEXT NOT NULL,
    captured_at TEXT NOT NULL,
    url TEXT NOT NULL,
    extraction_method TEXT NOT NULL,
    field_name TEXT NOT NULL,
    field_value TEXT NOT NULL
);

-- Deterministic cross-source correlation between a discovery signal and an
-- investment that likely describe the same underlying project.
CREATE TABLE correlation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investment_id INTEGER NOT NULL REFERENCES investment(id),
    signal_id INTEGER NOT NULL REFERENCES investment_signal(id),
    confidence TEXT NOT NULL,
    matched_features TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (investment_id, signal_id)
);

-- Which category a source belongs to (developer/discovery/aggregator).
-- Existing rows (chronos, greenbud) predate the three-category model and
-- default to DEVELOPER, which is factually correct for both.
ALTER TABLE source_snapshot ADD COLUMN source_category TEXT NOT NULL DEFAULT 'DEVELOPER';

-- Cached local-LLM interpretation of an investment, keyed by the exact
-- prompt that produced it so a re-analysis with unchanged inputs is a
-- cache hit rather than a repeated model call. Keyed by canonical_key
-- (not investment_id): analysis runs before a newly-detected investment
-- is committed/assigned a database id (see MonitoringService).
CREATE TABLE llm_analysis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investment_canonical_key TEXT NOT NULL,
    model TEXT NOT NULL,
    prompt_hash TEXT NOT NULL,
    response TEXT NOT NULL,
    analyzed_at TEXT NOT NULL,
    UNIQUE (investment_canonical_key, prompt_hash)
);

-- Explicit, reviewable location-development-potential data (see
-- docs/ARCHITECTURE.md location potential section). Populated from
-- analysis.LocationProfiles at application context startup, not
-- hand-edited in the database.
CREATE TABLE location_profile (
    name TEXT PRIMARY KEY,
    tier TEXT NOT NULL,
    growth_score INTEGER NOT NULL,
    infrastructure_score INTEGER NOT NULL,
    transport_score INTEGER NOT NULL,
    family_score INTEGER NOT NULL
);
