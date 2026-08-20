-- Deterministic cross-source deduplication: a pair of investments from
-- *different* sources that likely describe the same real-world project
-- (e.g. "Tercja" published on the developer's own site and "Osiedle Tercja
-- | Chronos" listed on the RynekPierwotny aggregator). Without this,
-- canonical_key (source:url) alone leaves the two as unrelated rows
-- throughout the frontend even though they are the same investment. See
-- InvestmentDeduplicator and docs/ARCHITECTURE.md cross-source
-- deduplication section.
--
-- investment_id_a is always the smaller id of the pair (enforced by
-- JdbcInvestmentDuplicateRepository.save), so the same pair is never
-- stored twice in reversed order.
CREATE TABLE investment_duplicate (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investment_id_a INTEGER NOT NULL REFERENCES investment(id),
    investment_id_b INTEGER NOT NULL REFERENCES investment(id),
    confidence TEXT NOT NULL,
    matched_features TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (investment_id_a, investment_id_b)
);
