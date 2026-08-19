CREATE TABLE investment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source TEXT NOT NULL,
    canonical_key TEXT NOT NULL UNIQUE,
    developer TEXT NOT NULL,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    location TEXT,
    property_type TEXT,
    units INTEGER,
    house_area_min REAL,
    house_area_max REAL,
    plot_area_min REAL,
    plot_area_max REAL,
    price_min INTEGER,
    price_max INTEGER,
    status TEXT,
    first_seen_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL
);

CREATE TABLE source_snapshot (
    source TEXT PRIMARY KEY,
    captured_at TEXT NOT NULL,
    investment_count INTEGER NOT NULL,
    content_hash TEXT NOT NULL
);

CREATE TABLE monitoring_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    started_at TEXT NOT NULL,
    finished_at TEXT,
    status TEXT NOT NULL,
    sources_checked INTEGER NOT NULL DEFAULT 0,
    sources_failed INTEGER NOT NULL DEFAULT 0,
    new_investments INTEGER NOT NULL DEFAULT 0
);
