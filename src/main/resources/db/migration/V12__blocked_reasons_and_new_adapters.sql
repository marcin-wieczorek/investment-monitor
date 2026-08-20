-- Syncs developer_registry / municipality_registry with the outcome of
-- the ADR-007 (Playwright) work session: new adapters implemented
-- (buk-obwieszczenia, szamotuly-ulicp, pobiedziska-komunikaty,
-- kornik-obwieszczenia, dopiewo-wz, archicom, pwd, nickel), one developer
-- domain correction (PWD moved from the now-expired pwd.com.pl to its
-- real site pwd-mieszkania.pl), and - the main point of this migration -
-- a `blocked_reason` column on both tables so the frontend can show
-- *why* a source is BLOCKED/NOT_IMPLEMENTED, not just the bare status
-- (see docs/SOURCES.md and registry.DiscoverySourceRegistry/DeveloperRegistry
-- for the full-length prose these are condensed from).
--
-- V1-V11 are never edited once applied - this ships as ALTER/UPDATE
-- against rows already inserted by V5/V9 (see V9 precedent).

ALTER TABLE municipality_registry ADD COLUMN discovery_blocked_reason TEXT;
ALTER TABLE developer_registry ADD COLUMN blocked_reason TEXT;

-- Newly implemented discovery sources.
UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'buk';
UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'szamotuly';
UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'pobiedziska';
UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'kornik';
UPDATE municipality_registry SET discovery_coverage = 'IMPLEMENTED' WHERE id = 'dopiewo';

-- Oborniki: re-verified reachable via PlaywrightPageFetcher (no longer a
-- JS-SPA-empty-shell BLOCKED case), but still not implementable - a
-- content-shape problem, not a technical block, so NOT_IMPLEMENTED is
-- the more accurate status now (see reason below).
UPDATE municipality_registry SET discovery_coverage = 'NOT_IMPLEMENTED' WHERE id = 'oborniki';

-- Blocked-reason text for every municipality whose discovery_coverage is
-- currently BLOCKED or NOT_IMPLEMENTED.
UPDATE municipality_registry SET discovery_blocked_reason = 'Rejestr WZ istnieje, ale publikuje jeden plik PDF na rok (nie HTML per sprawa) - wymagałoby ekstrakcji tekstu z PDF. Obwieszczenia są dostępne dopiero po kliknięciu "Pokaż archiwalne", ale to archiwum jest martwe: tylko 6 wpisów, najnowszy z 2022 roku.' WHERE id = 'oborniki';
UPDATE municipality_registry SET discovery_blocked_reason = 'BIP jest tylko katalogiem podstron; właściwa sekcja planowania przestrzennego zawiera wyłącznie statyczne dokumenty MPZP/studium - brak rejestru obwieszczeń.' WHERE id = 'mosina';
UPDATE municipality_registry SET discovery_blocked_reason = 'Rejestr "Postępowania administracyjne" istnieje i jest server-rendered, ale każdy wpis dotyczy infrastruktury publicznej (woda/kanalizacja), nie warunków zabudowy; brak też daty per wpis.' WHERE id = 'puszczykowo';
UPDATE municipality_registry SET discovery_blocked_reason = 'Kategoria "Obwieszczenia i ogłoszenia" istnieje, ale zawiera wyłącznie ogłoszenia nierezydentalne (wywłaszczenia drogowe, zabytki, wojsko) - brak dedykowanej kategorii warunków zabudowy.' WHERE id = 'kleszczewo';
UPDATE municipality_registry SET discovery_blocked_reason = 'Sekcja "Zagospodarowanie Przestrzenne" prowadzi wyłącznie do dokumentów MPZP/studium/środowiskowych - brak rejestru obwieszczeń lub spraw.' WHERE id = 'steszew';
UPDATE municipality_registry SET discovery_blocked_reason = 'Rejestr celu-publicznego/warunków-zabudowy istnieje i ma poprawną nazwę, ale zawiera obecnie tylko jeden wpis (infrastruktura drogowo-wodna) - za mało do zweryfikowania parsera.' WHERE id = 'skoki';
UPDATE municipality_registry SET discovery_blocked_reason = 'WAF/ochrona antybotowa blokuje dostęp (HTTP 403). Archiwalny BIP zawiera realny rejestr WZ, ale jest jawnie oznaczony jako archiwalny.' WHERE id = 'komorniki';
UPDATE municipality_registry SET discovery_blocked_reason = 'WAF/ochrona antybotowa blokuje dostęp (HTTP 403).' WHERE id = 'lubon';
UPDATE municipality_registry SET discovery_blocked_reason = 'Strona BIP nie odpowiada - błędy DNS/transportu zarówno dla HTTP jak i HTTPS.' WHERE id = 'kostrzyn';
UPDATE municipality_registry SET discovery_blocked_reason = 'Strona BIP nie odpowiada - błędy DNS/transportu zarówno dla HTTP jak i HTTPS.' WHERE id = 'rokietnica';

-- New developer adapters, implemented in this session.
UPDATE developer_registry SET status = 'MONITORED', adapter_source_id = 'archicom' WHERE id = 'archicom';
UPDATE developer_registry SET status = 'MONITORED', adapter_source_id = 'nickel' WHERE id = 'nickel';
UPDATE developer_registry SET status = 'MONITORED', adapter_source_id = 'pwd', website = 'https://pwd-mieszkania.pl' WHERE id = 'pwd';

-- Blocked/inactive-reason text for the remaining developer entries.
UPDATE developer_registry SET blocked_reason = 'Nie znaleziono działającej domeny (sovodevelopment.pl nie istnieje; sovo.pl to niepowiązana aplikacja).' WHERE id = 'sovo';
UPDATE developer_registry SET blocked_reason = 'Domena wygasła/porzucona - firma niedostępna w sieci.' WHERE id = 'budimex';
UPDATE developer_registry SET blocked_reason = 'Nie znaleziono zweryfikowanego dewelopera pod tą nazwą w rejonie Poznania.' WHERE id IN ('novaform', 'cavallia', 'btm', 'constructa_plus', 'virke', 'sgi', 'fb_antczak');
