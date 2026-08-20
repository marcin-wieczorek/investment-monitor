-- Persists whether an aggregator investment currently has no matching
-- developer source covering its location (Phase F, see docs/ROADMAP.md).
-- Recomputed for every aggregator investment on every scan
-- (MonitoringService.updateAggregatorOnlyDiscoveryFlags) instead of being
-- re-derived ad hoc in the frontend, keeping the location-coverage logic
-- (LocationCatalog) single-sourced in the deterministic Kotlin core.
--
-- NULL/0 for every non-aggregator investment; meaningful only where
-- source_snapshot.source_category = 'AGGREGATOR'.

ALTER TABLE investment ADD COLUMN aggregator_only_discovery INTEGER;
