-- Developer and municipality registries: explicit, reviewable static data
-- populated from registry.DeveloperRegistry / registry.MunicipalityRegistry
-- at application context startup (see V4 location_profile precedent), plus
-- a durable table for developers discovered indirectly at runtime.
--
-- Every URL below was manually verified against the live site before this
-- migration was written (see AGENTS.md "no fake implementations"). Developers
-- and municipalities without a verified URL/source are recorded with a
-- null website/BLOCKED status rather than invented.

CREATE TABLE developer_registry (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    website TEXT,
    investment_list_urls TEXT NOT NULL DEFAULT '[]',
    tier TEXT NOT NULL,
    status TEXT NOT NULL,
    geographic_scope TEXT NOT NULL DEFAULT '[]',
    adapter_source_id TEXT
);

CREATE TABLE developer_candidate (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    developer_name TEXT NOT NULL,
    discovered_url TEXT NOT NULL,
    municipality TEXT,
    discovered_from_source TEXT NOT NULL,
    discovered_at TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'NEW',
    evidence TEXT
);

CREATE TABLE municipality_registry (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    powiat TEXT NOT NULL,
    developer_coverage TEXT NOT NULL,
    discovery_coverage TEXT NOT NULL,
    aggregator_coverage TEXT NOT NULL
);

-- Tier A developers (AGENTS.md section 3). MONITORED rows reference the
-- adapter implemented in this same change; CANDIDATE rows have a verified
-- URL but no adapter yet; BLOCKED rows were investigated and found
-- technically unreachable/unscrapable.
INSERT INTO developer_registry (id, name, website, investment_list_urls, tier, status, geographic_scope, adapter_source_id) VALUES
('chronos', 'Chronos Development', 'https://www.chronos.poznan.pl', '["https://www.chronos.poznan.pl/inwestycje"]', 'A', 'MONITORED', '["Komorniki","Swarzędz","Kruszewnia","Rokietnica"]', 'chronos'),
('greenbud', 'Greenbud Development', 'https://www.greenbud.com.pl', '["https://www.greenbud.com.pl/nasze-inwestycje/"]', 'A', 'MONITORED', '["Swarzędz","Pobiedziska"]', 'greenbud'),
('jakon', 'Jakon', 'https://www.jakon-inwest.pl', '["https://www.jakon-inwest.pl/pl/mieszkania"]', 'A', 'MONITORED', '["Poznań","Tarnowo Podgórne","Mosina"]', 'jakon-inwest'),
('nickel', 'Nickel Development', 'https://www.nickel.com.pl', '["https://www.nickel.com.pl"]', 'A', 'BLOCKED', '["Poznań"]', NULL),
('agrobex', 'Agrobex', 'https://www.agrobex.pl', '["https://www.agrobex.pl"]', 'A', 'MONITORED', '["Poznań","Kleszczewo","Pobiedziska","Szamotuły","Śrem"]', 'agrobex'),
('linea', 'Linea', 'https://linea-deweloper.pl', '["https://linea-deweloper.pl/inwestycje"]', 'A', 'MONITORED', '["Dopiewo","Murowana Goślina","Buk"]', 'linea'),
('duda', 'Duda Development', 'https://dudadevelopment.pl', '["https://dudadevelopment.pl/nowe-mieszkania/poznan"]', 'A', 'MONITORED', '["Poznań"]', 'duda'),
('ataner', 'Ataner', 'https://www.ataner.pl', '["https://www.ataner.pl/pl/mieszkania-start"]', 'A', 'MONITORED', '["Poznań"]', 'ataner'),
('uwi', 'UWI', 'https://uwi.com.pl', '["https://uwi.com.pl/oferta/"]', 'A', 'MONITORED', '["Poznań"]', 'uwi'),
('pwd', 'PWD Deweloper', 'https://pwd.com.pl', '[]', 'A', 'BLOCKED', '[]', NULL),
('villa', 'Villa', NULL, '[]', 'A', 'CANDIDATE', '[]', NULL),
('konimpex', 'Konimpex-Invest', 'https://www.konimpex-invest.pl', '["https://www.konimpex-invest.pl/pl/inwestycje-2"]', 'A', 'MONITORED', '["Poznań"]', 'konimpex'),
('sovo', 'Sovo Development', NULL, '[]', 'A', 'BLOCKED', '[]', NULL),
('pekabex', 'Pekabex Development', 'https://pekabexdevelopment.com', '["https://pekabexdevelopment.com"]', 'A', 'MONITORED', '["Poznań"]', 'pekabex'),
('monday', 'Monday Development', 'https://mondaydevelopment.pl', '["https://mondaydevelopment.pl/mieszkania"]', 'A', 'NO_CURRENT_INVESTMENTS', '["Poznań"]', NULL),
('murapol', 'Murapol', 'https://murapol.pl', '["https://murapol.pl/oferta/poznan/mieszkania"]', 'A', 'MONITORED', '["Poznań"]', 'murapol'),
('develia', 'Develia', 'https://develia.pl', '["https://develia.pl/pl/mieszkania/poznan/"]', 'A', 'MONITORED', '["Poznań"]', 'develia'),
('atal', 'ATAL', 'https://atal.pl', '["https://atal.pl/inwestycje/mieszkania/poznan/"]', 'A', 'MONITORED', '["Poznań","Swarzędz"]', 'atal'),
('archicom', 'Archicom / Echo Residential', 'https://archicom.pl', '[]', 'A', 'BLOCKED', '[]', NULL),
('robyg', 'ROBYG', 'https://robyg.pl', '["https://robyg.pl/poznan"]', 'A', 'MONITORED', '["Poznań"]', 'robyg');

-- Tier B developers (AGENTS.md section 4).
INSERT INTO developer_registry (id, name, website, investment_list_urls, tier, status, geographic_scope, adapter_source_id) VALUES
('ebf', 'EBF Development', 'https://ebfdevelopment.pl', '["https://ebfdevelopment.pl/poznan"]', 'B', 'MONITORED', '["Poznań"]', 'ebf'),
('cordia', 'Cordia', 'https://cordiapolska.pl', '["https://cordiapolska.pl/miasta/poznan/"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('ronson', 'Ronson', 'https://ronson.pl', '["https://ronson.pl/inwestycja/grunwald-miedzy-drzewami/"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('budimex', 'Budimex', NULL, '[]', 'B', 'INACTIVE', '[]', NULL),
('novaform', 'Novaform', NULL, '[]', 'B', 'BLOCKED', '[]', NULL),
('ggw', 'GGW Development', 'https://ggwdevelopment.pl', '["https://ggwdevelopment.pl"]', 'B', 'MONITORED', '["Poznań"]', 'ggw'),
('sivanet', 'SIVANET', 'https://sivanet.pl', '["https://sivanet.pl/nieruchomosci/lechicka-65/"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('mj', 'MJ Deweloper', 'https://mjdeweloper.pl', '["https://mjdeweloper.pl/oferta/"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('spravia', 'Spravia', 'https://spravia.pl', '["https://spravia.pl/inwestycje/poznan/"]', 'B', 'MONITORED', '["Poznań"]', 'spravia'),
('cavallia', 'Cavallia', NULL, '[]', 'B', 'BLOCKED', '[]', NULL),
('area', 'Area Development', 'https://areadevelopment.pl', '["https://areadevelopment.pl/pl/nasza-oferta"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('jaksbud', 'JakśBud', 'https://jaksbud.pl', '["https://jaksbud.pl/znajdz-mieszkanie/"]', 'B', 'MONITORED', '["Poznań"]', 'jaksbud'),
('btm', 'BTM', NULL, '[]', 'B', 'BLOCKED', '[]', NULL),
('constructa_plus', 'Constructa Plus', NULL, '[]', 'B', 'BLOCKED', '[]', NULL),
('inwestycje_wielkopolski', 'Inwestycje Wielkopolski', 'https://inwestycjewielkopolski.pl', '["https://inwestycjewielkopolski.pl/w-sprzedazy/"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('virke', 'Virke', NULL, '[]', 'B', 'BLOCKED', '[]', NULL),
('sgi', 'SGI', 'https://sgi.pl', '[]', 'B', 'BLOCKED', '[]', NULL),
('sagaris', 'Sagaris', 'https://sagaris.pl', '["https://sagaris.pl/inwestycje/poznan/"]', 'B', 'MONITORED', '["Poznań"]', 'sagaris'),
('vastbouw', 'Vastbouw', 'https://vastbouw.pl', '["https://vastbouw.pl/inwestycje/mieszkania-domy-poznan/"]', 'B', 'CANDIDATE', '["Poznań"]', NULL),
('fb_antczak', 'FB Antczak', NULL, '[]', 'B', 'BLOCKED', '[]', NULL);

-- All 23 target Metropolia Poznań municipalities (AGENTS.md section 1).
-- Coverage columns reflect the source adapters that exist after this
-- migration (see MonitoringService/SourceRegistry) - never invented.
INSERT INTO municipality_registry (id, name, powiat, developer_coverage, discovery_coverage, aggregator_coverage) VALUES
('poznan', 'Poznań', 'miasto na prawach powiatu', 'IMPLEMENTED', 'IMPLEMENTED', 'IMPLEMENTED'),
('buk', 'Buk', 'poznański', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('czerwonak', 'Czerwonak', 'poznański', 'NOT_IMPLEMENTED', 'IMPLEMENTED', 'NOT_IMPLEMENTED'),
('dopiewo', 'Dopiewo', 'poznański', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('kleszczewo', 'Kleszczewo', 'poznański', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('komorniki', 'Komorniki', 'poznański', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('kostrzyn', 'Kostrzyn', 'poznański', 'NOT_IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('kornik', 'Kórnik', 'poznański', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED'),
('lubon', 'Luboń', 'poznański', 'NOT_IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('mosina', 'Mosina', 'poznański', 'IMPLEMENTED', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED'),
('murowana_goslina', 'Murowana Goślina', 'poznański', 'IMPLEMENTED', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED'),
('oborniki', 'Oborniki', 'obornicki', 'NOT_IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('pobiedziska', 'Pobiedziska', 'poznański', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('puszczykowo', 'Puszczykowo', 'poznański', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED'),
('rokietnica', 'Rokietnica', 'poznański', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('skoki', 'Skoki', 'wągrowiecki', 'NOT_IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('steszew', 'Stęszew', 'poznański', 'NOT_IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('suchy_las', 'Suchy Las', 'poznański', 'NOT_IMPLEMENTED', 'IMPLEMENTED', 'NOT_IMPLEMENTED'),
('swarzedz', 'Swarzędz', 'poznański', 'IMPLEMENTED', 'IMPLEMENTED', 'IMPLEMENTED'),
('szamotuly', 'Szamotuły', 'szamotulski', 'IMPLEMENTED', 'BLOCKED', 'NOT_IMPLEMENTED'),
('srem', 'Śrem', 'śremski', 'IMPLEMENTED', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED'),
('tarnowo_podgorne', 'Tarnowo Podgórne', 'poznański', 'IMPLEMENTED', 'IMPLEMENTED', 'NOT_IMPLEMENTED');
