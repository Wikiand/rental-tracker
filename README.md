# Multi-User Rental Tracker

A secure multi-user rental management application built with **Java 17**, **Maven**, and **SQLite**.

The application supports user authentication, item listings, rentals, returns, validation, structured logging, persistent database storage, and merging data from another populated database.

---

## ✨ Features

* User registration and authentication
* Argon2id password hashing
* Item listing and inventory management
* Rental and return workflows
* Ownership and availability checks
* Input validation
* SQLite persistence with foreign keys
* Structured logging without sensitive credentials
* Automated unit and integration tests
* Docker support
* Database export and merge/import

---

## 🏗️ Architecture

The application uses a layered structure:

```text
Main / CLI
    ↓
Services
    ↓
Validation / Security / Repositories
    ↓
SQLite
```

Main packages:

```text
domain/
service/
repository/
validation/
security/
logging/
infrastructure/
transport/
importer/
```

The database merge functionality is implemented in:

```text
src/main/java/com/kood/rentaltracker/importer/
```

The main importer components are:

```text
DatabaseImporter.java
ImportContext.java
ImportResult.java
CsvReader.java
```

---

## 🗄️ Database

The database contains three main tables:

| **Table**      | **Purpose**                       |
| -------------- | --------------------------------- |
| `users`        | User accounts and password hashes |
| `listed_items` | Items available for rental        |
| `rentals`      | Rental transactions               |

Foreign keys connect users, items, and rentals.

The important relationships are:

```text
users
  ↓
listed_items
  ↓
rentals
```

A listed item belongs to a user, while a rental references both an item and a renter.

---

## 🚀 Running

Requirements:

* Java 17+
* Maven
* SQLite

Build:

```bash
mvn clean package
```

Run:

```bash
mvn exec:java -Dexec.mainClass="com.kood.rentaltracker.transport.Main"
```

---

## 🔄 Database Import

The `import` command merges CSV exports from another populated Rental Tracker database into the current database.

Run:

```bash
mvn -q exec:java \
  -Dexec.mainClass="com.kood.rentaltracker.transport.Main" \
  -Dexec.args="import /path/to/import-directory" \
  -Djacoco.skip=true
```

The import directory must contain:

```text
users.csv
listed_items.csv
rentals.csv
```

The importer reports:

* inserted records
* matched records
* rejected records
* the filename and line number of rejected records
* the reason for each rejection

Example from the first demonstration import into a clean destination:

```text
Import complete: inserted=3, matched=4, rejected=3
Rejected: users.csv line 5: username is required
Rejected: listed_items.csv line 4: owner_id could not be resolved
Rejected: rentals.csv line 4: item_id and renter_id could not be resolved
```

### Creating the CSV files

The CSV files can be produced from a populated SQLite database using the SQLite CLI.

For example:

```bash
sqlite3 source.db
```

Then:

```sql
.headers on
.mode csv

.output users.csv
SELECT id, username, password_hash, created_at
FROM users
ORDER BY id;

.output listed_items.csv
SELECT item_id, owner_id, item_name, description,
       cost_per_day, status, created_at
FROM listed_items
ORDER BY item_id;

.output rentals.csv
SELECT rental_id, item_id, renter_id, start_time,
       end_time, returned_at, status
FROM rentals
ORDER BY rental_id;

.output stdout
.quit
```

Place the three CSV files in the same directory before running the import.

For the repository demonstration, the populated source database is:

```text
merge-source.db
```

Its clean raw CSV export is stored in:

```text
import-demo-clean/
```

The demonstration import fixture is stored in:

```text
import-demo/
```

The demonstration fixture contains intentionally invalid records so that rejection handling can be demonstrated.

### Import order

Records are processed in dependency order:

```text
users → listed_items → rentals
```

Users must be processed first because listed items reference users.

Listed items must be processed before rentals because rentals reference listed items and users.

Records are processed FIFO within each CSV file.

### Import state machine

Each record follows the same logical state-machine sequence:

```text
read
  ↓
matched or new
  ↓
validated
  ↓
inserted or rejected
```

A record that matches an existing local row is mapped to that row and is not modified.

A new record is validated before it is inserted.

A rejected record does not stop later records from being processed.

### Matching and ID remapping

Source database IDs are not used as globally unique identifiers because IDs are local to each database.

For example:

```text
owner_id = 3
```

means the third user in the source database. It does not necessarily mean local user ID `3`.

The importer therefore matches records using identifying fields.

| **Table**      | **Matching fields**                     | **Reason**                                                |
| -------------- | --------------------------------------- | --------------------------------------------------------- |
| `users`        | `username`                              | Usernames are unique                                      |
| `listed_items` | `owner + item_name`                     | Identifies an item belonging to a specific owner          |
| `rentals`      | `item + renter + start_time + end_time` | Identifies a rental independently of its source rental ID |

When a match is found, the source ID is mapped to the existing local ID.

When a record is new, it is inserted and its newly generated local ID is stored in the mapping.

### Foreign-key translation

Foreign keys from the source database are translated before insertion.

The importer maintains mappings such as:

```text
source user ID → local user ID
source item ID → local item ID
```

For example:

```text
source user 15 → local user 8
source item 15 → local item 15
source rental 15 → local rental 12
```

If an item references source user `15`, the importer first resolves source user `15` to its local user ID.

The local ID is then stored as the item's `owner_id`.

Similarly, a rental cannot be inserted until both its source item and source renter have been resolved to local IDs.

This prevents source database IDs from being incorrectly written directly into the destination database.

### Blanks and validation

CSV files do not preserve SQL `NULL` values and database types in exactly the same way as SQLite.

The importer therefore validates and converts values before insertion.

Required fields cannot be blank.

For example:

```text
username = blank
```

is rejected with:

```text
username is required
```

Nullable fields may be blank.

For example:

```text
returned_at = blank
```

is converted to SQL `NULL`.

Invalid numeric values are rejected before insertion.

Unresolvable foreign keys are also rejected.

For example:

```text
owner_id could not be resolved
```

or:

```text
item_id and renter_id could not be resolved
```

A rejected record does not prevent subsequent valid records from being processed.

### Merge guarantees

The importer provides the following guarantees:

* Existing rows are not modified.
* Existing records are matched rather than overwritten.
* New records receive new local database IDs.
* Source foreign keys are translated to local IDs.
* Repeated imports do not create duplicate records.
* The complete merge is performed inside one database transaction.
* A failure rolls back the entire merge.
* Rejected records include the filename, line number, and reason.
* Records after a rejected record continue to be processed.

### Password hashes

Password hashes are imported as-is.

This means a user exported from the same application can retain their existing password credentials after import.

The importer does not convert password hashes between different hashing formats.

---

## 🧪 Testing

Run the importer tests:

```bash
mvn -q -Djacoco.skip=true clean test -Dtest=DatabaseImporterTest
```

Run the full test suite:

```bash
mvn -q -Djacoco.skip=true clean test
```

The importer tests cover:

* Merge into a populated database
* Matching existing users
* Matching existing items
* Matching existing rentals
* Importing new users
* Importing new items
* Importing new rentals
* Source-to-local ID remapping
* Required-field validation
* Nullable-field handling
* Unresolvable foreign keys
* Continuing after rejected records
* Idempotent repeated imports
* Existing-row preservation
* Transaction rollback after a forced failure

The rollback test intentionally throws an exception after user processing and verifies that changes made before the failure are rolled back.

---

## 🐳 Docker

Build and run:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

---

## 📁 Project Structure

```text
rental-tracker/
├── src/
│   ├── main/java/com/kood/rentaltracker/
│   │   ├── domain/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── validation/
│   │   ├── security/
│   │   ├── logging/
│   │   ├── infrastructure/
│   │   ├── transport/
│   │   └── importer/
│   │       ├── DatabaseImporter.java
│   │       ├── ImportContext.java
│   │       ├── ImportResult.java
│   │       └── CsvReader.java
│   │
│   └── test/java/com/kood/rentaltracker/
│       └── DatabaseImporterTest.java
│
├── import-demo/
│   ├── users.csv
│   ├── listed_items.csv
│   └── rentals.csv
│
├── import-demo-clean/
│   ├── users.csv
│   ├── listed_items.csv
│   └── rentals.csv
│
├── docs/
├── rental-tracker.db
├── merge-source.db
├── pom.xml
├── Dockerfile
├── compose.yml
└── README.md
```

---

## 🔐 Security

* Passwords are hashed using Argon2id.
* Passwords and password hashes are excluded from application logs.
* Ownership is enforced at the service layer.
* Input is validated before persistence.
* SQLite foreign-key constraints maintain data integrity.
* Source database IDs are translated before being used as foreign keys.
* Existing database records are not overwritten during imports.
* Database imports use transactions to prevent partial merges.

---

## 📝 Logging

The application uses structured logging for important application events.

Logging records useful application and security events without exposing sensitive authentication information.

Plaintext passwords and password hashes must not be written to application logs.

---

## 📊 Import Demonstration

The repository includes a populated source database and CSV files designed to exercise the merge functionality.

The source database is:

```text
merge-source.db
```

The clean export is:

```text
import-demo-clean/
```

The demonstration fixture is:

```text
import-demo/
```

The demonstration exercises:

* Records that match existing destination records
* Primary-key collisions
* Genuinely new records
* Foreign-key ID remapping
* An unresolvable user reference
* An unresolvable item reference
* A blank required field
* A blank nullable field
* Repeated import with no additional inserts

Run the demonstration with:

```bash
mvn -q exec:java \
  -Dexec.mainClass="com.kood.rentaltracker.transport.Main" \
  -Dexec.args="import import-demo" \
  -Djacoco.skip=true
```

On the populated database used during testing, the repeat import reports:

```text
Import complete: inserted=0, matched=7, rejected=3
Rejected: users.csv line 5: username is required
Rejected: listed_items.csv line 4: owner_id could not be resolved
Rejected: rentals.csv line 4: item_id and renter_id could not be resolved
```

The database remains:

```text
users: 8
listed_items: 15
rentals: 12
```

This confirms that the repeat import does not add duplicate rows.

---

## ☕ Java and Maven

The project targets Java 17.

Build:

```bash
mvn clean package
```

Run tests:

```bash
mvn -q -Djacoco.skip=true clean test
```

JaCoCo is skipped when necessary because the current Java runtime may be newer than the JaCoCo version supported by the project.

---

## 👥 Team

This project was developed collaboratively.

| Contributor | Role |
|---|---|
| Christine Nyambura | Team Leader |
| Zipporah Chibalonza | Team Member |
| Victor Kiama | Team Member |

---

**Built with Java 17, Maven, SQLite, Docker, and a security-first approach.**
