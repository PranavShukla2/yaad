CREATE TABLE reminder (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL,
    category_id TEXT,
    start_local TEXT NOT NULL,
    rule TEXT,
    time_zone_id TEXT,
    all_day INTEGER NOT NULL DEFAULT 0,
    early_alert_seconds TEXT NOT NULL DEFAULT '',
    priority TEXT NOT NULL,
    nag_seconds INTEGER,
    amount_minor INTEGER,
    amount_currency TEXT,
    amount_minor_digits INTEGER,
    auto_pay INTEGER NOT NULL DEFAULT 0,
    linked_loan_id TEXT,
    archived INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);
CREATE TABLE checklist_item (
    reminder_id TEXT NOT NULL REFERENCES reminder(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    text TEXT NOT NULL,
    done INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (reminder_id, position)
);
CREATE TABLE occurrence (
    reminder_id TEXT NOT NULL REFERENCES reminder(id) ON DELETE CASCADE,
    due_at INTEGER NOT NULL,
    status TEXT NOT NULL,
    status_at INTEGER NOT NULL,
    paid_minor INTEGER,
    paid_currency TEXT,
    paid_minor_digits INTEGER,
    note TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (reminder_id, due_at)
);
CREATE TABLE category (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    color INTEGER
);
CREATE TABLE setting (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);
CREATE INDEX reminder_archived ON reminder(archived);
