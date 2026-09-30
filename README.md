# 🏠 Multi-User Rental Tracker

A secure, multi-user command-line rental tracking application built with Java 17, SQLite, Maven, Docker, and GitHub Actions.

The application allows users to register, authenticate, manage rental items, rent and return items, and access functionality according to their permissions.

---

## 📋 Overview

Rental Tracker is a command-line application for managing rental items and users.

The project was extended into a multi-user application with:

- User accounts
- Authentication
- Password hashing
- User permissions
- Item management
- Rental and return workflows
- Input validation
- Exception handling
- Structured application logging
- Automated testing
- JaCoCo test coverage
- Docker containerization
- Persistent Docker volumes
- Continuous integration with GitHub Actions

---

## ✨ Key Features

### 👤 User Management

- User registration
- Username validation
- Password validation
- Secure password hashing
- User authentication
- Session management

### 🔐 Security

- Passwords are never stored in plaintext
- Passwords are hashed using Password4j
- Authentication is required for protected operations
- User permissions control available operations
- Sensitive information is excluded from application logs

### 📦 Item Management

- Add rental items
- List available items
- View item information
- Delist items
- Prevent invalid item operations

### 🔄 Rental Management

- Rent available items
- Return rented items
- Track rental ownership
- Prevent invalid rental operations
- Validate rental state transitions

### 🧪 Automated Testing

The project includes automated tests covering:

- User registration
- Authentication
- Password handling
- Item management
- Rental workflows
- Return workflows
- Validation
- Repository operations
- Database initialization
- Exception handling
- Logging
- Main application workflows

### 🐳 Docker

The complete application can be built and started using:

    docker compose up --build

SQLite remains embedded in the application. No separate database container is required.

### 🔄 Continuous Integration

GitHub Actions automatically builds and tests the project on every push and pull request.

---

## 🏗️ Architecture

The application follows a layered structure:

    com.kood.rentaltracker
    ├── database
    ├── exception
    ├── logging
    ├── model
    ├── repository
    ├── service
    ├── transport
    └── validation

### Main Layers

**Model**

Contains the application's domain objects.

**Repository**

Handles persistence and database operations.

**Service**

Contains application and business logic.

**Validation**

Handles user input and data validation.

**Transport**

Contains the command-line interface and application entry point.

**Exception**

Contains application-specific exceptions and exception handling.

**Logging**

Provides structured application logging.

**Database**

Handles SQLite database configuration and connections.

---

## 🗄️ Database

The application uses:

- SQLite
- JDBC
- Embedded database storage

The database stores information required for:

- Users
- Rental items
- Rentals
- User permissions
- Application state

SQLite is embedded directly into the application, so a separate database server is not required.

---

## 🔐 Password Security

User passwords are never stored as plaintext.

The application uses Password4j to hash and verify passwords.

Authentication compares the supplied password against the stored password hash without exposing the original password.

Sensitive credentials are also excluded from application logs.

---

## 🔄 Item State

Rental items move through controlled states depending on their current condition and rental status.

Typical workflow:

    Available
        ↓
      Rented
        ↓
     Returned
        ↓
      Available

Invalid state transitions are rejected by the application.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Application development |
| Maven | Build and dependency management |
| SQLite | Embedded database |
| JDBC | Database access |
| JUnit 5 | Automated testing |
| JaCoCo | Test coverage |
| Password4j | Password hashing |
| Docker | Containerization |
| Docker Compose | Application orchestration |
| GitHub Actions | Continuous integration |

---

## 📁 Project Structure

    rental-tracker/
    ├── .github/
    │   └── workflows/
    │       └── ci.yml
    ├── src/
    │   ├── main/
    │   └── test/
    ├── compose.yaml
    ├── Dockerfile
    ├── docker-entrypoint.sh
    ├── pom.xml
    ├── rental-tracker.db
    └── README.md

---

# 🚀 Running Locally

## Requirements

Install:

- Java 17
- Maven
- Git

Check Java:

    java -version

Check Maven:

    mvn -version

---

## ▶️ Build the Application

Build the project with:

    mvn clean package

---

## ▶️ Run the Application

Run the application using Maven:

    mvn exec:java -Dexec.mainClass="com.kood.rentaltracker.transport.Main"

The application starts with the interactive rental tracker menu.

---

# 🧪 Testing

Run the complete test suite with:

    mvn test

A failing test causes Maven to return a non-zero exit status.

The test suite covers:

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

# 📊 Test Coverage

JaCoCo is used to measure automated test coverage.

The coverage report can be generated with:

    mvn clean test

The report is generated under:

    target/site/jacoco/

The main HTML report is:

    target/site/jacoco/index.html

The project currently reports:

| Metric | Coverage |
|---|---:|
| Classes | **100%** |
| Methods | **89%** |
| Instructions | **75%** |
| Lines | **72%** |
| Branches | **60%** |

The remaining coverage gaps are primarily defensive CLI and error-handling paths in `Main`.

---

# 🔄 Continuous Integration

The project uses GitHub Actions to automatically verify every change.

The workflow configuration is located at:

    .github/workflows/ci.yml

The pipeline runs on:

- Every push
- Every pull request

## Pipeline Order

The CI pipeline:

1. Checks out the repository in a clean environment.
2. Sets up Java 17.
3. Restores Maven dependencies.
4. Builds the application.
5. Runs the complete multi-user test suite.
6. Generates the JaCoCo coverage report.
7. Uploads the coverage report as a downloadable workflow artifact.

The workflow uses:

    mvn -B clean verify

If a test fails, the CI pipeline fails.

This makes a broken branch visible before review or merging.

## Coverage Artifact

After a successful GitHub Actions run, the `jacoco-report` artifact can be downloaded from the workflow run.

The artifact contains the generated JaCoCo coverage report.

---

# 🔒 Main Branch Protection

The `main` branch is protected.

Changes must be submitted through a pull request rather than being pushed directly to `main`.

The required CI status check is:

    test

A pull request cannot be merged while the required CI check is failing.

Force pushes to `main` are also blocked.

---

# 🐳 Docker Compose

The complete application can be started from a fresh clone using:

    docker compose up --build

Docker Compose:

1. Builds the application image.
2. Creates the required named volumes.
3. Configures the application environment.
4. Starts the container.
5. Launches the interactive rental tracker menu.

The application starts with:

    === Rental tracker ===
    1) Register
    2) Login
    3) Exit

SQLite is embedded in the application, so no separate database container is required.

---

## 🐋 Docker Image

The Dockerfile uses a multi-stage build.

### Build Stage

The build stage:

- Uses Maven with Java 17
- Compiles the application
- Packages the application
- Copies runtime dependencies

### Runtime Stage

The runtime stage:

- Uses Eclipse Temurin Java 17
- Contains the compiled application
- Contains runtime dependencies
- Contains the seeded database
- Starts the application through `docker-entrypoint.sh`

---

# 💾 Persistent Volumes

Docker Compose defines two named volumes:

| Volume | Container Path | Purpose |
|---|---|---|
| `rental_tracker_data` | `/app/data` | Stores the persistent SQLite database |
| `rental_tracker_logs` | `/app/logs` | Stores application logs |

The database path and log path are supplied through environment variables.

Database:

    RENTAL_TRACKER_DB_URL=jdbc:sqlite:/app/data/rental-tracker.db

Log:

    RENTAL_TRACKER_LOG_PATH=/app/logs/rental-tracker.log

---

## 🗃️ Database Persistence

The SQLite database is stored at:

    /app/data/rental-tracker.db

The directory is backed by the named Docker volume:

    rental_tracker_data

On the first startup, the seeded database is copied into the persistent volume if no database already exists.

On subsequent startups, the existing database is reused.

Therefore, application data survives:

- `docker compose down`
- Container recreation
- Image rebuilds

Existing application data is not overwritten during normal container restarts.

---

## 📝 Log Persistence

Application logs are written to:

    /app/logs/rental-tracker.log

The directory is backed by:

    rental_tracker_logs

This allows application logs to survive container recreation and image rebuilds.

The application uses structured log events such as:

    INFO event=database.connection.opened databaseType=sqlite

Sensitive information such as plaintext passwords and password hashes must not be written to logs.

---

# 🔧 Docker Commands

Build and start the application:

    docker compose up --build

Start the application:

    docker compose up

Stop the application:

    docker compose down

Rebuild the image:

    docker compose build

View running containers:

    docker compose ps

View application logs:

    docker compose logs

---

# 🔁 Persistence Demonstration

To verify that application data survives container recreation:

1. Start the application:

       docker compose up --build

2. Create or modify application data.

3. Stop the containers:

       docker compose down

4. Start the application again:

       docker compose up

5. Verify that the previously stored data is still available.

The named Docker volumes preserve the SQLite database and application logs.

---

# 🌱 Seeded Database

A seeded SQLite database is included for initial application setup.

The seed database is:

    rental-tracker.db

The Docker entrypoint initializes the persistent database only when the persistent volume does not already contain one.

This prevents existing application data from being overwritten during container restarts.

---

# 🖥️ Application Menu

When the application starts, the main menu provides:

    === Rental tracker ===
    1) Register
    2) Login
    3) Exit

After authentication, users can access the operations permitted by their account.

---

# 🔄 Rental Workflow

The general rental workflow is:

    Register
       ↓
    Login
       ↓
    Browse available items
       ↓
    Rent item
       ↓
    Item becomes unavailable
       ↓
    Return item
       ↓
    Item becomes available again

Validation and business rules prevent invalid operations.

---

# ⚠️ Error Handling

The application uses structured exception handling to deal with invalid operations and unexpected errors.

Examples include:

- Invalid user input
- Invalid credentials
- Invalid item operations
- Invalid rental operations
- Database errors
- Missing records
- Invalid state transitions

Exceptions are mapped to appropriate user-facing messages without exposing unnecessary internal details.

---

# 📝 Logging

The application uses a centralized `AppLogger`.

Logging supports:

- Informational events
- Warnings
- Errors
- Database connection events
- Authentication-related events
- Application events

Log entries use structured event information, for example:

    INFO event=database.connection.opened databaseType=sqlite

Errors record the exception type where appropriate while avoiding sensitive information.

Passwords and password hashes are not written to logs.

---

# 🔐 Secure Development

Security considerations implemented in the project include:

- Password hashing
- Authentication
- Permission checks
- Input validation
- Controlled exception handling
- Sensitive-data protection in logs
- Database access through JDBC
- Automated security-related tests

---

# 🌿 Git & Collaboration

Development follows a branch-and-pull-request workflow.

Changes are developed on feature or task branches and submitted through pull requests.

The protected `main` branch requires the CI status check to pass before merging.

---

# 🎯 Project Goal

The goal of the Multi-User Rental Tracker is to provide a secure and maintainable rental management application while demonstrating practical software engineering practices.

The project demonstrates:

- Object-oriented programming
- Java application development
- Database integration
- Authentication
- Authorization
- Secure password handling
- Automated testing
- Test coverage
- Continuous integration
- Docker containerization
- Persistent storage
- Git-based development
