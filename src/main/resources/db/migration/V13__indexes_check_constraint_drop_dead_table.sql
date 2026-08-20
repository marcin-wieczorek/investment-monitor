-- Performance: explicit secondary index on investment.source, used by
-- InvestmentRepository.findAllBySource (see docs review - "missing indexes"
-- finding). The source_evidence indexes are created further down, after
-- that table is rebuilt with a CHECK constraint.
CREATE INDEX idx_investment_source ON investment(source);

-- Data integrity: enforce "exactly one of investment_id/signal_id" at the
-- database level (previously only enforced by SourceEvidence.init's
-- application-level require()). SQLite cannot ALTER a CHECK constraint
-- onto an existing table, so the table is recreated.
CREATE TABLE source_evidence_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investment_id INTEGER REFERENCES investment(id),
    signal_id INTEGER REFERENCES investment_signal(id),
    source_id TEXT NOT NULL,
    source_category TEXT NOT NULL,
    captured_at TEXT NOT NULL,
    url TEXT NOT NULL,
    extraction_method TEXT NOT NULL,
    field_name TEXT NOT NULL,
    field_value TEXT NOT NULL,
    CHECK ((investment_id IS NOT NULL) != (signal_id IS NOT NULL))
);

INSERT INTO source_evidence_new SELECT * FROM source_evidence;
DROP TABLE source_evidence;
ALTER TABLE source_evidence_new RENAME TO source_evidence;

CREATE INDEX idx_source_evidence_investment_id ON source_evidence(investment_id);
CREATE INDEX idx_source_evidence_signal_id ON source_evidence(signal_id);

-- Dead code cleanup: location_profile was declared in V4 but never
-- populated at runtime (see AGENTS.md) - LocationProfiles.kt is the single
-- source of truth for this data. Drop the unused table rather than carry
-- schema that nothing reads or writes.
DROP TABLE IF EXISTS location_profile;
