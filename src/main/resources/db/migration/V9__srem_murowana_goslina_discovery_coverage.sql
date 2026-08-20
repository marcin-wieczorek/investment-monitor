-- Updates municipality_registry and developer_registry discovery-coverage
-- rows for the "Lower-priority / explicitly deferred: additional discovery
-- sources" work (see docs/ROADMAP.md). V1-V8 are never edited once
-- applied, so this ships as plain UPDATEs against rows V5 already
-- inserted.
--
-- Śrem and Murowana Goślina now have a real, verified, working discovery
-- adapter (srem-wz, murowana-goslina-obwieszczenia). Kleszczewo, Dopiewo,
-- Skoki and Stęszew were re-investigated and found reachable/server-rendered
-- (no longer pure JS SPAs or transport-error dead ends), but no adapter
-- could be built yet - NOT_IMPLEMENTED is a more accurate status for them
-- now than BLOCKED (see registry.DiscoverySourceRegistry for the detailed
-- per-municipality reason).

UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'srem';
UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'murowana_goslina';
UPDATE municipality_registry SET discovery_coverage = 'NOT_IMPLEMENTED' WHERE id = 'kleszczewo';
UPDATE municipality_registry SET discovery_coverage = 'NOT_IMPLEMENTED' WHERE id = 'dopiewo';
UPDATE municipality_registry SET discovery_coverage = 'NOT_IMPLEMENTED' WHERE id = 'skoki';
UPDATE municipality_registry SET discovery_coverage = 'NOT_IMPLEMENTED' WHERE id = 'steszew';
