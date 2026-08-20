-- Generic key-value store for user-configurable preferences, starting
-- with the scoring reference profile (see docs/PLAN-configurable-scoring.md
-- and ReferenceProfiles.kt: previously a single hard-coded profile with no
-- way to change it short of editing Kotlin source).
--
-- One row per key; `value` is a JSON-encoded string. Unlike `location_profile`
-- (see V4, dead code - never populated/read), this table is read at scan
-- time by UserPreferencesRepository.effectiveScoringProfile() - don't
-- repeat that mistake.
CREATE TABLE user_preferences (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
