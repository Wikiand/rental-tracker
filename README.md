# Multi-User Rental Tracker

A secure, multi-user rental management application built with **Java 17**, **Maven**, and **SQLite**. The system allows users to register and authenticate, list items for rent, browse other users' listings, create rentals, and confirm returns.

The project focuses on **secure application design, clear separation of responsibilities, data integrity, validation, testing, logging, and persistent storage**.

---

## 👥 Team

| Name | Role |
|---|---|
| **Christine Nyambura** | Team Leader |
| **Zipporah Chibalonza** | Member |
| **Victor Kiama** | Member |

---

## 📌 Overview

The Multi-User Rental Tracker is a command-line rental platform where multiple users can interact with a shared rental database.

Each user can:

- Create an account
- Log in securely
- List items for rental
- View their own inventory
- Browse items listed by other users
- Rent available items
- Confirm returned rentals

The application enforces ownership and rental rules at the service layer while using SQLite to maintain persistent application data.

---

## ✨ Key Features

### 🔐 Authentication & Security

- User registration and login
- Unique usernames
- Password hashing using **Argon2id**
- Passwords are never stored in plaintext
- Password hashes are never written to application logs
- Session-aware operations use the currently authenticated user

### 🏠 Item Management

- List an item for rental
- Automatically associate the item with its owner
- View items owned by the current user
- Browse available listings from other users
- Set a rental price per day
- Validate item information before persistence

### 📦 Rental Management

- Rent available items
- Automatically associate rentals with the authenticated renter
- Calculate rental periods
- Prevent users from renting their own listings
- Prevent unavailable items from being rented
- Confirm returns
- Automatically update item and rental states

### 🛡️ Validation & Business Rules

Validation is performed at the service boundary to prevent invalid data from reaching the persistence layer.

Examples include:

- Username validation
- Password validation
- Item name and description validation
- Rental duration validation
- Ownership checks
- Item availability checks
- Rental state checks

### 📝 Structured Logging

The application includes structured application logging with:

- UTC timestamps
- Log levels
- Event names
- Context information

Examples of logged events include:

- `database.connection.opening`
- `database.connection.opened`
- `database.connection.closing`
- `database.connection.closed`
- `database.connection.failed`
- `item.listed`
- `rental.created`
- `return.confirmed`

Sensitive information such as passwords and password hashes is excluded from logs.

---

## 🏗️ Architecture

The application follows a layered architecture that separates user interaction, business logic, persistence, security, validation, and domain models.

```text
                    ┌──────────────────────┐
                    │      Main / CLI       │
                    │     User Interface    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │       Services       │
                    │                      │
                    │  UserService         │
                    │  ItemService         │
                    │  RentalService       │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
       ┌─────────────┐ ┌──────────────┐ ┌──────────────┐
       │ Validation  │ │   Security   │ │ Repositories │
       │             │ │              │ │              │
       │ User        │ │ Password     │ │ User         │
       │ Item        │ │ Hasher       │ │ Item         │
       │ Rental      │ │              │ │ Rental       │
       └─────────────┘ └──────────────┘ └──────┬───────┘
                                               │
                                               ▼
                                    ┌────────────────────┐
                                    │       SQLite       │
                                    │                    │
                                    │ users              │
                                    │ listed_items       │
                                    │ rentals            │
                                    └────────────────────┘
```

---

## 🗄️ Database Design

The application uses SQLite with foreign-key constraints enabled.

### Users

Stores registered users and their securely hashed passwords.

| Column | Description |
|---|---|
| `id` | Unique user identifier |
| `username` | Unique username |
| `password_hash` | Argon2id password hash |
| `created_at` | Account creation timestamp |

### Listed Items

Stores items made available for rental.

| Column | Description |
|---|---|
| `item_id` | Unique item identifier |
| `owner_id` | User who owns the item |
| `item_name` | Item name |
| `description` | Item description |
| `cost_per_day` | Rental price per day |
| `status` | Current item state |
| `created_at` | Listing creation timestamp |

### Rentals

Stores rental transactions.

| Column | Description |
|---|---|
| `rental_id` | Unique rental identifier |
| `item_id` | Rented item |
| `renter_id` | User renting the item |
| `start_time` | Rental start time |
| `end_time` | Expected rental end time |
| `returned_at` | Actual return time |
| `status` | Rental state |

### Relationships

```text
users
  │
  ├─────────────── owns ───────────────► listed_items
  │                                          │
  │                                          │
  └──────────── rents ───────────────► rentals
                                             │
                                             │
                                             ▼
                                        listed_items
```

Foreign keys maintain relationships between users, items, and rentals.

Database integrity was also verified using SQLite foreign-key checks.

---

## 🔄 Item State Transitions

Items follow controlled state transitions:

```text
                 ┌─────────────┐
                 │  AVAILABLE  │
                 └──────┬──────┘
                        │
                     Rent
                        │
                        ▼
                 ┌─────────────┐
                 │   RENTED    │
                 └──────┬──────┘
                        │
                      Return
                        │
                        ▼
                 ┌─────────────┐
                 │  AVAILABLE  │
                 └─────────────┘

AVAILABLE ───────────────► UNLISTED
RENTED ──────────────────► UNLISTED
UNLISTED ────────────────► AVAILABLE
```

Rental states are:

- `ACTIVE`
- `CLOSED`

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| **Java 17** | Application development |
| **Maven** | Build and dependency management |
| **SQLite** | Persistent relational database |
| **JDBC** | Database connectivity |
| **Password4j** | Argon2id password hashing |
| **JUnit 5** | Automated testing |
| **JaCoCo** | Code coverage |
| **Docker** | Containerized deployment |
| **Docker Compose** | Container orchestration |
| **Git** | Version control |

---

## 📁 Project Structure

```text
rental-tracker/
├── src/
│   ├── main/
│   │   └── java/com/kood/rentaltracker/
│   │       ├── transport/
│   │       │   └── Main.java
│   │       │
│   │       ├── service/
│   │       │   ├── UserService.java
│   │       │   ├── ItemService.java
│   │       │   └── RentalService.java
│   │       │
│   │       ├── repository/
│   │       │   ├── UserRepository.java
│   │       │   ├── ItemRepository.java
│   │       │   ├── RentalRepository.java
│   │       │   ├── UserRepositoryImpl.java
│   │       │   ├── ItemRepositoryImpl.java
│   │       │   ├── RentalRepositoryImpl.java
│   │       │   └── exception/
│   │       │       ├── DatabaseOpenException.java
│   │       │       ├── ConstraintViolationException.java
│   │       │       ├── NotFoundException.java
│   │       │       ├── MappingException.java
│   │       │       └── DatabaseExceptionMapper.java
│   │       │
│   │       ├── domain/
│   │       │   ├── User.java
│   │       │   ├── Item.java
│   │       │   ├── ItemStatus.java
│   │       │   ├── Rental.java
│   │       │   └── RentalStatus.java
│   │       │
│   │       ├── infrastructure/
│   │       │   ├── Database.java
│   │       │   └── DatabaseInitializer.java
│   │       │
│   │       ├── security/
│   │       │   └── PasswordHasher.java
│   │       │
│   │       ├── validation/
│   │       │   ├── UserValidator.java
│   │       │   ├── ItemValidator.java
│   │       │   └── RentalValidator.java
│   │       │
│   │       └── logging/
│   │           └── AppLogger.java
│   │
│   └── test/
│       └── java/com/kood/rentaltracker/
│           ├── UserServiceTest.java
│           ├── ItemStatusTest.java
│           ├── RentalServiceTest.java
│           ├── ItemServiceTest.java
│           ├── RepositoryIntegrationTest.java
│           ├── ExceptionTest.java
│           ├── DatabaseExceptionMapperTest.java
│           ├── ExceptionCoverageTest.java
│           ├── DatabaseInitializerTest.java
│           ├── MainIntegrationTest.java
│           ├── AppLoggerTest.java
│           ├── RentalValidatorTest.java
│           └── support/
│               └── DatabaseTestSupport.java
│
├── docs/
│   └── ER_DIAGRAM.md
│
├── Dockerfile
├── compose.yml
├── docker-entrypoint.sh
├── rental-tracker.db
├── pom.xml
├── .gitignore
└── README.md
```

---

## ⚙️ Requirements

Before running the application locally, make sure you have:

- Java 17 or later
- Maven
- SQLite
- Git

For containerized execution:

- Docker
- Docker Compose

---

## 🚀 Running Locally

Clone the repository and enter the project directory:

```bash
git clone https://gitea.kood.tech/christinenyambura/multi-user-rental-tracker.git
cd rental-tracker
```

Build the project:

```bash
mvn clean package
```

Run the application:

```bash
mvn exec:java -Dexec.mainClass="com.kood.rentaltracker.transport.Main"
```

---

## 🧪 Testing

Run the complete test suite with:

```bash
mvn test
```

The project includes unit, integration, validation, exception-handling, database, logging, and CLI integration tests.

The test suite covers areas including:

- User registration
- Authentication
- Password handling
- Item management
- Rental workflows
- Return workflows
- Validation
- Repository operations
- Database initialization
- Exception mapping
- Logging
- Main application workflows

---

## 📊 Test Coverage

JaCoCo is used to measure automated test coverage.

Current coverage:

| Metric | Coverage |
|---|---:|
| Classes | **100%** |
| Methods | **89%** |
| Instructions | **75%** |
| Lines | **72%** |
| Branches | **60%** |

The remaining coverage gaps are primarily defensive CLI and error-handling paths in `Main`.

Generate the JaCoCo report with:

```bash
mvn test jacoco:report
```

The report is generated under:

```text
target/site/jacoco/
```

---

## 🐳 Docker

The application can be built and run using Docker Compose.

Build and start the application:

```bash
docker compose up --build
```

The application runs interactively inside the container.

Stop the application:

```bash
docker compose down
```

---

## 💾 Database Persistence

SQLite data is stored in a Docker named volume:

```text
rental_tracker_data
```

The container uses:

```text
/app/data/rental-tracker.db
```

On the first startup, the seeded database is copied into the persistent volume.

On subsequent startups, the existing database is reused.

This allows application data to survive container recreation.

---

## 🌱 Seeded Data

The project includes a seeded SQLite database containing multiple users and rental listings to make it possible to test multi-user workflows immediately.

The seed database contains:

- Multiple registered users
- Multiple item owners
- Multiple rental listings
- Different item states

Demo credentials are intentionally not documented in this README.

---

## 🖥️ Application Menu

After authentication, users can access the main rental menu:

```text
=== Rental tracker ===
1) List an item
2) View my inventory
3) Browse listings
4) Confirm a return
5) Exit
>
```

### List an Item

The currently authenticated user becomes the owner automatically.

### View My Inventory

Displays items owned by the currently authenticated user.

### Browse Listings

Displays available items belonging to other users.

Listings are paginated to keep the CLI manageable.

### Confirm a Return

Allows the renter to confirm the return of an active rental.

### Exit

Terminates the current application session.

---

## 🔁 Example Rental Workflow

A typical rental flow is:

```text
1. User registers
        │
        ▼
2. User logs in
        │
        ▼
3. User lists an item
        │
        ▼
4. Another user logs in
        │
        ▼
5. User browses available listings
        │
        ▼
6. User selects an item
        │
        ▼
7. User rents the item
        │
        ▼
8. Item becomes RENTED
        │
        ▼
9. User confirms the return
        │
        ▼
10. Rental becomes CLOSED
        │
        ▼
11. Item becomes AVAILABLE
```

---

## 🛡️ Security Considerations

Security was considered throughout the application design.

### Password Protection

Passwords are hashed using **Argon2id** before being stored in the database.

The application never stores plaintext passwords.

### Ownership Enforcement

Users cannot arbitrarily modify another user's inventory through the service layer.

### Rental Restrictions

The application prevents invalid rental operations such as:

- Renting an unavailable item
- Renting an item owned by the current user
- Confirming invalid or already-closed rentals

### Input Validation

User-controlled values are validated before reaching persistence operations.

### Database Integrity

Foreign keys and database constraints are used to maintain relationships and prevent invalid records.

### Sensitive Logging

Passwords and password hashes are excluded from application logs.

---

## ⚠️ Error Handling

The application uses dedicated exception types to separate different failure conditions.

Examples include:

- `DatabaseOpenException`
- `ConstraintViolationException`
- `NotFoundException`
- `MappingException`

Database exceptions are translated through:

```text
DatabaseExceptionMapper
```

This keeps low-level database failures from leaking directly into higher application layers.

---

## 📝 Logging

Application logging is handled by:

```text
AppLogger
```

Log entries contain structured information such as:

```text
timestamp
level
event
context
```

The logging system records important application events while avoiding sensitive authentication information.

---

## 🤝 Git & Collaboration

The project uses Git for version control and collaborative development.

The repository contains separate concerns for:

- Domain modelling
- Business logic
- Persistence
- Validation
- Security
- Logging
- Testing
- Deployment

Changes are committed incrementally so that project development and debugging can be tracked throughout the implementation process.

---

## 📚 Documentation

Additional technical documentation is available under:

```text
docs/
```

Including the database/entity relationship documentation:

```text
docs/ER_DIAGRAM.md
```

---

## 🎯 Project Goal

The goal of the Multi-User Rental Tracker is to demonstrate how a real-world multi-user application can be designed with:

- Secure authentication
- Clear application architecture
- Relational data modelling
- Ownership and authorization rules
- Input validation
- Exception handling
- Automated testing
- Code coverage
- Structured logging
- Containerized deployment
- Persistent database storage

The project combines software development practices with security-conscious engineering to create a maintainable and reliable rental management system.

---

## 👩‍💻 Team

**Christine Nyambura** — Team Leader  
**Zipporah Chibalonza** — Member  
**Victor Kiama** — Member

---

**Built with Java 17, Maven, SQLite, Docker, and a security-first approach.**
