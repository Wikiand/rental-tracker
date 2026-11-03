# Multi-User Rental Tracker

A secure, multi-user rental management application built with **Java 17**, **Maven**, and **SQLite**.

The application supports user registration and authentication, item listings, rental and return workflows, input validation, structured logging, persistent database storage, and transactional database imports.

---

## ✨ Features

- Multi-user registration and authentication
- Argon2id password hashing
- Item listing and inventory management
- Rental and return workflows
- Ownership and availability checks
- Input validation
- SQLite persistence with foreign-key constraints
- Structured application logging
- CSV database import and merging
- Source-to-local ID mapping
- Transactional import with rollback on fatal errors
- Automated unit and integration tests
- Docker support

---

## 🏗️ Architecture

The application follows a layered architecture:

```text
             Main / CLI
                 |
                 v
              Services
                 |
        +--------+--------+
        |        |        |
        v        v        v
    Validation Security Repositories
        |        |        |
        +--------+--------+
                 |
                 v
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

The database import functionality is implemented in:

```text
src/main/java/com/kood/rentaltracker/importer/
```

Main importer components:

- `DatabaseImporter.java` — coordinates the import process.
- `ImportContext.java` — maintains source-to-local ID mappings.
- `ImportResult.java` — records inserted, matched, and rejected records.
- `CsvReader.java` — reads and parses CSV records.

---

## 🗄️ Database Design

The application uses three main tables:

| Table | Purpose |
|---|---|
| `users` | User accounts and password hashes |
| `listed_items` | Items listed by users for rental |
| `rentals` | Rental transactions and return information |

The relationships are:

```text
users
  |
  +---- listed_items
  |          |
  |          +---- rentals
  |
  +---------------- rentals
```

- Each listed item belongs to a user through `owner_id`.
- Each rental references a listed item through `item_id`.
- Each rental references a user through `renter_id`.
- Foreign-key constraints enforce referential integrity.
- A partial unique index prevents an item from having multiple active rentals.

See [Database ER Diagram](docs/ER_DIAGRAM.md) for the entity relationships.

---

## 🚀 Getting Started

### Requirements

- Java 17 or later
- Apache Maven
- SQLite

### Build the application

```bash
mvn clean package
```

### Run the application

```bash
mvn exec:java -Dexec.mainClass="com.kood.rentaltracker.transport.Main"
```

The application starts with an authentication menu where users can register, log in, or exit.

---

## 🔄 CSV Database Import

The application supports merging CSV exports from another populated Rental Tracker database into the current database.

The importer processes users, listed items, and rentals in dependency order. It matches existing records, inserts new records, translates source database IDs into local IDs, and reports rejected records.

### Running an import

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

- Inserted records
- Matched records
- Rejected records
- Rejected record filenames and line numbers
- The reason for each rejection

Example output from the demonstration import into a clean destination:

```text
Import complete: inserted=3, matched=4, rejected=3
Rejected: users.csv line 5: username is required
Rejected: listed_items.csv line 4: owner_id could not be resolved
Rejected: rentals.csv line 4: item_id and renter_id could not be resolved
```

### Creating CSV files

CSV exports can be generated from a populated SQLite database using the SQLite command-line interface.

Open the source database:

```bash
sqlite3 source.db
```

Export the required tables:

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

Place the three generated CSV files in the same directory before running the import.

### Import order

Records are processed in dependency order:

```text
users → listed_items → rentals
```

Users are processed first because listed items reference users.

Listed items are processed before rentals because rentals reference both listed items and users.

Records are processed in file order within each CSV file.

### Import workflow

Each record follows this logical sequence:

```text
Read record
     |
     v
Check for existing match
     |
     +------ Match found ------> Map existing local ID
     |
     v
No match found
     |
     v
Validate record
     |
     +------ Invalid ----------> Reject record
     |
     v
Insert new record
     |
     v
Map generated local ID
```

A matching record is mapped to its existing local row and is not modified.

A new record is validated before insertion.

A rejected record does not stop subsequent records from being processed.

### Matching and ID remapping

Source database IDs are local to their respective databases. They cannot be assumed to identify the same records in the destination database.

The importer uses identifying fields to match records:

| Table | Matching fields | Purpose |
|---|---|---|
| `users` | `username` | Identifies an existing user |
| `listed_items` | Owner and `item_name` | Identifies an item belonging to a particular owner |
| `rentals` | Item, renter, `start_time`, and `end_time` | Identifies an existing rental |

When a match is found, the source ID is mapped to the corresponding local ID.

When a record is new, it is inserted and its generated local ID is stored in the mapping context.

### Foreign-key translation

Source foreign keys are translated before new records are inserted.

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

If an item references source user `15`, the importer resolves that source ID to the corresponding local user ID before inserting the item.

Similarly, a rental must resolve both its source item and source renter before insertion.

This prevents source database IDs from being incorrectly written directly into destination foreign-key fields.

### Validation and nullable fields

The importer validates and converts CSV values before insertion.

Examples:

| Input | Result |
|---|---|
| Blank required `username` | Rejected |
| Blank `returned_at` | Converted to SQL `NULL` |
| Invalid numeric value | Rejected |
| Unresolvable foreign key | Rejected |

Example rejection:

```text
username is required
```

Unresolvable references produce errors such as:

```text
owner_id could not be resolved
```

or:

```text
item_id and renter_id could not be resolved
```

Rejected records are reported with their filename and line number. Processing continues with subsequent records.

### Merge guarantees

- Existing rows are not modified.
- Matching records are mapped rather than overwritten.
- New records receive locally generated database IDs.
- Source foreign keys are translated to local IDs.
- Repeated imports do not create duplicate records for successfully matched records.
- The import runs inside a database transaction.
- Fatal failures roll back the transaction.
- Rejected records are reported individually.
- A rejected record does not prevent later records from being processed.

### Password hashes

Password hashes are imported as-is.

Users exported from the same application can retain their existing password credentials after import.

The importer does not convert password hashes between different hashing formats.

---

## 🧪 Testing

### Run importer tests

```bash
mvn -q -Djacoco.skip=true clean test -Dtest=DatabaseImporterTest
```

### Run the complete test suite

```bash
mvn -q -Djacoco.skip=true clean test
```

The project includes unit and integration tests covering:

- User registration and authentication
- Password hashing
- Item creation and inventory operations
- Rental and return workflows
- Ownership and availability validation
- Atomic rental operations
- Database initialization
- Repository integration
- CSV database imports
- Existing-record matching
- New-record insertion
- Source-to-local ID remapping
- Required-field validation
- Nullable-field handling
- Unresolvable foreign keys
- Rejection handling and continued processing
- Repeated imports
- Existing-row preservation
- Transaction rollback

The importer rollback test forces a failure after user processing and verifies that changes made before the failure are rolled back.

---

## 🐳 Docker

Build and start the application:

```bash
docker compose up --build
```

Stop the application:

```bash
docker compose down
```

The Docker configuration uses persistent volumes for database storage and application logs.

The database is retained between container restarts. Removing the named volumes with `docker compose down -v` also removes the persisted data.

---

## 📁 Project Structure

```text
rental-tracker/
├── src/
│   ├── main/
│   │   └── java/com/kood/rentaltracker/
│   │       ├── domain/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── validation/
│   │       ├── security/
│   │       ├── logging/
│   │       ├── infrastructure/
│   │       ├── transport/
│   │       └── importer/
│   │           ├── DatabaseImporter.java
│   │           ├── ImportContext.java
│   │           ├── ImportResult.java
│   │           └── CsvReader.java
│   │
│   └── test/
│       └── java/com/kood/rentaltracker/
│           ├── AppLoggerTest.java
│           ├── AtomicRentalOperationsTest.java
│           ├── DatabaseExceptionMapperTest.java
│           ├── DatabaseImporterTest.java
│           ├── DatabaseInitializerTest.java
│           ├── ExceptionCoverageTest.java
│           ├── ExceptionTest.java
│           ├── ItemServiceTest.java
│           ├── ItemStatusTest.java
│           ├── MainIntegrationTest.java
│           ├── RentalServiceTest.java
│           ├── RentalValidatorTest.java
│           ├── RepositoryIntegrationTest.java
│           ├── UserServiceTest.java
│           └── support/
│               └── DatabaseTestSupport.java
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
│   └── ER_DIAGRAM.md
│
├── pom.xml
├── Dockerfile
├── compose.yaml
└── README.md
```

---

## 🔐 Security

- Passwords are hashed using Argon2id.
- Ownership checks are enforced at the service layer.
- Input is validated before persistence.
- SQLite foreign-key constraints maintain referential integrity.
- Source database IDs are translated before being used as foreign keys.
- Existing database records are not overwritten during imports.
- Database imports use transactions to prevent partial merges after fatal failures.

---

## 📝 Logging

The application uses structured logging to record important application events.

Log entries include timestamps, severity levels, event information, and contextual details.

Supported logging levels:

- `INFO`
- `WARN`
- `ERROR`

The default log file is:

```text
rental-tracker.log
```

The log location can be configured using:

```text
RENTAL_TRACKER_LOG_PATH
```

Authentication credentials should not be included in application log messages or contextual data.

---

## 📊 Import Demonstration

The repository includes a populated source database and CSV fixtures for demonstrating the import functionality.

Source database:

```text
merge-source.db
```

Clean CSV export:

```text
import-demo-clean/
```

Demonstration fixture:

```text
import-demo/
```

The demonstration fixture includes records designed to exercise:

- Existing-record matching
- Primary-key collisions
- New record insertion
- Foreign-key ID remapping
- Unresolvable user references
- Unresolvable item references
- Blank required fields
- Blank nullable fields
- Repeated imports

Run the demonstration import:

```bash
mvn -q exec:java \
  -Dexec.mainClass="com.kood.rentaltracker.transport.Main" \
  -Dexec.args="import import-demo" \
  -Djacoco.skip=true
```

The repeat import against the populated demonstration destination produced:

```text
Import complete: inserted=0, matched=7, rejected=3
Rejected: users.csv line 5: username is required
Rejected: listed_items.csv line 4: owner_id could not be resolved
Rejected: rentals.csv line 4: item_id and renter_id could not be resolved
```

The demonstrated database counts were:

```text
users: 8
listed_items: 15
rentals: 12
```

These results demonstrate that successfully matched records are not inserted again during a repeated import.

---

## ☕ Java and Maven

The project targets **Java 17** and uses **Maven** for dependency management, building, and testing.

Build:

```bash
mvn clean package
```

Run tests:

```bash
mvn -q -Djacoco.skip=true clean test
```

JaCoCo is used for test coverage reporting. The `jacoco.skip` property can be used to skip coverage instrumentation when required by the local Java runtime or build environment.

---

## 👥 Team

This project was developed collaboratively.

| Contributor | Role |
|---|---|
| Christine Nyambura | Team Leader |
| Zipporah Chibalonza | Team Member |
| Victor Kiama | Team Member |

---

**Built with Java 17, Maven, SQLite, and Docker.**
