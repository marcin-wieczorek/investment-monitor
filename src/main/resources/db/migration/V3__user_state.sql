CREATE TABLE user_note (
    investment_id INTEGER PRIMARY KEY REFERENCES investment(id),
    note TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE investment_state (
    investment_id INTEGER PRIMARY KEY REFERENCES investment(id),
    archived INTEGER NOT NULL DEFAULT 0,
    reviewed_at TEXT
);
