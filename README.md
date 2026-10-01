# Multi-User Rental Tracker

A secure multi-user rental management application built with **Java 17**, **Maven**, and **SQLite**.

The application supports user authentication, item listings, rentals, returns, validation, structured logging, and persistent database storage.

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

---

## 🗄️ Database

The database contains three main tables:

| Table          | Purpose                           |
| -------------- | --------------------------------- |
| `users`        | User accounts and password hashes |
| `listed_items` | Items available for rental        |
| `rentals`      | Rental transactions               |

Foreign keys connect users, items, and rentals.

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

The `import` command merges CSV exports from another populated Rental Tracker database.

```bash
mvn exec:java \
  -Dexec.mainClass="com.kood.rentaltracker.transport.Main" \
  -Dexec.args="import /path/to/import-directory"
```

The directory must contain:

```text
users.csv
listed_items.csv
rentals.csv
```

### Creating the CSV files

Export the source database using SQLite:

```bash
sqlite3 source.db
```

Then:

```sql
.headers on
.mode csv

.output users.csv
SELECT id, username, password_hash, created_at FROM users;

.output listed_items.csv
SELECT item_id, owner_id, item_name, description,
       cost_per_day, status, created_at
FROM listed_items;

.output rentals.csv
SELECT rental_id, item_id, renter_id, start_time,
       end_time, returned_at, status
FROM rentals;

.output stdout
.quit
```

Place the three files in one directory before importing.

### Import order

Records are processed in dependency order:

```text
users → listed_items → rentals
```

Each record follows:

```text
read → validate → resolve → match/new → insert/reject
```

Records are processed FIFO within each file.

### Matching and ID remapping

Source database IDs are not used for matching because IDs are local to each database.

| Table          | Matching fields                         | Reason                                               |
| -------------- | --------------------------------------- | ---------------------------------------------------- |
| `users`        | `username`                              | Usernames are unique                                 |
| `listed_items` | `owner + item_name`                     | Identifies an item belonging to a specific owner     |
| `rentals`      | `item + renter + start_time + end_time` | Identifies the rental independently of its source ID |

When a match is found, the source ID is mapped to the existing local ID. New records receive new local IDs.

Foreign keys are therefore translated before insertion:

```text
source user ID → local user ID
source item ID → local item ID
source renter ID → local user ID
```

### Blanks and rejected records

Required fields cannot be blank. For example, a blank `username` causes the record to be rejected.

For nullable fields, a blank represents `NULL`. For example:

```text
returned_at = blank → SQL NULL
```

An unresolved foreign key is rejected without preventing other valid records from importing.

### Merge guarantees

* Existing rows are not modified.
* Repeated imports are idempotent.
* The complete merge runs in one transaction.
* A failure rolls back the entire import.
* The command reports inserted, matched, and rejected records.

---

## 🧪 Testing

Run all tests:

```bash
mvn -Djacoco.skip=true test
```

Run importer tests only:

```bash
mvn -Djacoco.skip=true -Dtest=DatabaseImporterTest test
```

Importer tests cover:

* Merge into a populated database
* Existing records
* New records
* Idempotent repeated imports
* Unresolvable records
* Required and nullable blanks
* Existing-row preservation
* Transaction rollback

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
│   │
│   └── test/java/com/kood/rentaltracker/
│
├── import-demo/
│   ├── users.csv
│   ├── listed_items.csv
│   └── rentals.csv
│
├── docs/
├── rental-tracker.db
├── pom.xml
├── Dockerfile
├── compose.yml
└── README.md
```

---

## 🔐 Security

* Passwords are hashed using Argon2id.
* Passwords and hashes are excluded from logs.
* Ownership is enforced at the service layer.
* Input is validated before persistence.
* SQLite foreign-key constraints maintain data integrity.

---

**Built with Java 17, Maven, SQLite, Docker, and a security-first approach.**
