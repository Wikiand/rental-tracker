# Multi-User Rental Tracker

A command-line rental management application that supports multiple user accounts, secure password hashing, item ownership, rentals, returns, validation, structured logging, persistent SQLite storage, and Docker deployment.

## Team

- **Christine Nyambura** — Team Leader
- **Zipporah Chibalonza** — Member
- **Victor Kiama** — Member

## Overview

The Multi-User Rental Tracker allows users to register and log in to their own accounts, list items for rent, manage their inventory, browse other users' available listings, rent items, and confirm returns.

The application enforces ownership and rental rules in the service layer rather than relying only on the CLI interface.

## Features

### Authentication
- User registration and login
- Unique usernames
- Argon2id password hashing
- Passwords and password hashes are never logged
- Invalid credentials produce a readable error message

### Rental Management
- List items for rent
- View personal inventory
- Browse available items
- Rent items for a selected number of days
- Confirm returned rentals
- Track active and completed rentals

### Ownership Enforcement
- Listed items automatically belong to the logged-in user
- Rentals automatically use the logged-in user as the renter
- Users cannot rent their own items
- Only the item owner can confirm a return
- Ownership checks are enforced in the service layer

### Validation
Validation is performed at the service boundary before repository/database operations.

The application validates:
- Required text fields
- Positive rental costs
- Valid user references
- Valid item references
- Positive rental duration
- Valid entity relationships

Invalid input is rejected before it reaches the database.

### Browsing and Pagination
- Available listings exclude items owned by the current user
- Listings are displayed five items per page
- Users can navigate between pages
- Users can view an item and choose to rent it

### Structured Logging
The application records structured events containing:
- Timestamp
- Log level
- Event name
- Context fields

Important events include:
- Database connection opening and closing
- Validation failures
- Database exceptions
- Recoverable failures
- `item.listed`
- `rental.created`
- `return.confirmed`

Sensitive information such as passwords and password hashes is never logged.

## Technology Stack

- **Java 17**
- **Maven**
- **SQLite**
- **Password4j**
- **Argon2id**
- **JUnit 5**
- **JaCoCo**
- **Docker**
- **Git / Gitea**

## Project Structure

```text
src/
├── main/java/com/kood/rentaltracker/
│   ├── domain/
│   ├── infrastructure/
│   ├── logging/
│   ├── repository/
│   ├── security/
│   ├── service/
│   ├── validation/
│   └── transport/
│
└── test/java/com/kood/rentaltracker/
    ├── support/
    └── test classes

docs/
└── ER_DIAGRAM.md

Dockerfile
compose.yml
docker-entrypoint.sh
pom.xml
README.md
Database

The application uses SQLite with three main tables.

Users

Stores registered user accounts.

Important columns:

id
username
password_hash
created_at

The username is unique and the password is stored as an Argon2id hash.

Listed Items

Stores items offered for rental.

Important columns:

item_id
owner_id
item_name
description
cost_per_day
status
created_at

The owner_id references the users table.

Rentals

Stores rental transactions.

Important columns:

rental_id
item_id
renter_id
start_time
end_time
returned_at
status

The item_id references listed_items and renter_id references users.

Relationships
users
  │
  ├── owns ────────> listed_items
  │                       │
  │                       │ rented through
  │                       ▼
  └── rents <──────── rentals

The complete ER diagram is available in:

docs/ER_DIAGRAM.md
Running Locally

Use Java 17:

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"

Build the project:

mvn clean package

Run the application:

mvn -q exec:java -Dexec.mainClass=com.kood.rentaltracker.transport.Main
Running Tests

Run the complete test suite:

mvn -q clean test

The test suite covers authentication, validation, services, repositories, database initialization, exception handling, logging, and integration behaviour.

Test Coverage

JaCoCo coverage is included in the project.

The latest coverage report achieved:

100% class coverage
89% method coverage
75% instruction coverage
72% line coverage
60% branch coverage

The core domain, validation, exception handling, logging, and security components have complete coverage, with high coverage across the service and repository layers.

The remaining uncovered code is primarily defensive CLI and error-handling branches in Main.

The generated JaCoCo report is available under:

target/site/jacoco/index.html
Docker

The application can be built and run using Docker Compose.

Build and start:

docker compose up --build

The SQLite database is stored in a Docker named volume so application data survives container restarts, rebuilds, and upgrades.

Stop the application:

docker compose down

Start it again:

docker compose up

The existing persistent database is reused when the container starts again.

Seeded Data

The repository includes a seeded SQLite database containing multiple user accounts and listed items owned by different users.

The seeded data supports demonstration of:

Multiple accounts
Different item owners
Available items
Rented items
Rental history
Ownership enforcement

Example demo accounts include:

christine
james
Alex
chrstine
Main Menu

After authentication, the application provides:

=== Rental tracker ===
1) List an item
2) View my inventory
3) Browse listings
4) Confirm a return
5) Exit
>

The logged-in user's account is automatically used when listing items, renting items, and performing ownership checks.

Example Workflow
Register or log in as a user.
List an item.
Log in using another account.
Browse available listings.
Select an item owned by another user.
Enter the rental duration.
The item becomes rented.
The owner can confirm the return.
The rental is closed and the item becomes available again.
Error Handling

The application separates user-facing messages from internal diagnostic information.

Users receive concise and readable messages such as:

Invalid username or password.

or:

Rental duration must be greater than zero.

Internal exception details are recorded in the application log instead of exposing database errors or implementation details to the user.

Recoverable errors return the user to the appropriate menu.

Database startup failures are treated as fatal and cause the application to exit after logging the internal failure.

Security Considerations
Passwords are hashed using Argon2id.
Plaintext passwords are not stored in the database.
Password hashes are not stored in the User domain object.
Passwords and password hashes are never written to logs.
Ownership is enforced in the service layer.
Invalid input is validated before repository/database operations.
SQLite foreign-key constraints are enabled.
Database errors are converted into application-level exceptions.
User-facing messages do not expose raw SQLite errors or internal implementation details.
Git and Collaboration

The project uses Git with the Kood Gitea repository.

Development was organized around focused commits covering features such as:

Authentication
Password security
Validation
Ownership enforcement
Rental workflows
Exception handling
Structured logging
Database improvements
Docker support
Testing and coverage
Documentation

The repository history provides a record of the project's development and review workflow.

Project Goal

The goal of the project is to provide a secure, maintainable multi-user rental application while demonstrating practical software engineering principles including:

Object-oriented programming
Layered architecture
Repository and service patterns
Input validation
Authentication and password security
Ownership and business-rule enforcement
Exception handling
Automated testing
Code coverage
Structured logging
Database persistence
Docker containerization
Collaborative Git workflows
