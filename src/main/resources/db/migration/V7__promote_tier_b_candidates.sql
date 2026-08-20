-- Promotes the 7 Tier B CANDIDATE developers with a verified adapter
-- (Phase E, see docs/ROADMAP.md) to MONITORED, records their
-- adapter_source_id, and corrects investment_list_urls to the exact page
-- each adapter fetches (verified against the live site - see the KDoc on
-- each {Name}Parser for details).
--
-- Flyway migrations are append-only: V1-V6 are never edited once applied,
-- so this ships as plain UPDATEs against the rows V5 already inserted.

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'cordia'
WHERE id = 'cordia';

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'ronson',
    investment_list_urls = '["https://ronson.pl/poznan/inwestycje/"]'
WHERE id = 'ronson';

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'sivanet'
WHERE id = 'sivanet';

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'mj',
    investment_list_urls = '["https://mjdeweloper.pl"]'
WHERE id = 'mj';

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'area'
WHERE id = 'area';

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'inwestycje_wielkopolski',
    investment_list_urls = '["https://inwestycjewielkopolski.pl/realizacje/"]'
WHERE id = 'inwestycje_wielkopolski';

UPDATE developer_registry
SET status = 'MONITORED', adapter_source_id = 'vastbouw'
WHERE id = 'vastbouw';
