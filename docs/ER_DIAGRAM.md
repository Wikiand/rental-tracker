# Rental Tracker ER Diagram

## Entity Relationship Diagram

```text
┌──────────────────────────────┐
│            users             │
├──────────────────────────────┤
│ PK  id           INTEGER     │
│     username     TEXT NOT NULL│
│                   UNIQUE      │
│     password     TEXT        │
│     created_at   TEXT NOT NULL│
└──────────────┬───────────────┘
               │
               │ 1
               │
               │
               │ *
┌──────────────▼───────────────┐
│        listed_items          │
├──────────────────────────────┤
│ PK  item_id       INTEGER    │
│     owner_id      INTEGER NOT NULL
│     item_name     TEXT NOT NULL
│     description   TEXT NOT NULL
│     cost_per_day  REAL NOT NULL
│     status        TEXT NOT NULL
│                  CHECK(status │
│                  IN           │
│                  ('available',│
│                   'rented',   │
│                   'unlisted'))│
│     created_at    TEXT NOT NULL
└──────────────┬───────────────┘
               │
               │ 1
               │
               │
               │ *
┌──────────────▼───────────────┐
│           rentals            │
├──────────────────────────────┤
│ PK  rental_id    INTEGER     │
│     item_id      INTEGER NOT NULL
│     renter_id    INTEGER NOT NULL
│     start_time   TEXT NOT NULL
│     end_time     TEXT NOT NULL
│     returned_at  TEXT        │
│     status       TEXT NOT NULL
│                  CHECK(status │
│                  IN           │
│                  ('active',   │
│                   'closed')) │
└──────────────────────────────┘