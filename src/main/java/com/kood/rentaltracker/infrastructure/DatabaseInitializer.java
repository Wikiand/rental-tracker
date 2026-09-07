package com.kood.rentaltracker.infrastructure;

import com.kood.rentaltracker.repository.exception.ConstraintViolationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                PRAGMA foreign_keys = ON;

                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS listed_items (
                    item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_id INTEGER NOT NULL,
                    item_name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    cost_per_day REAL NOT NULL,
                    status TEXT NOT NULL
                        CHECK (status IN ('available', 'rented', 'unlisted')),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (owner_id) REFERENCES users(id)
                );

                CREATE TABLE IF NOT EXISTS rentals (
                    rental_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id INTEGER NOT NULL,
                    renter_id INTEGER NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    returned_at TEXT,
                    status TEXT NOT NULL
                        CHECK (status IN ('active', 'closed')),
                    FOREIGN KEY (item_id) REFERENCES listed_items(item_id),
                    FOREIGN KEY (renter_id) REFERENCES users(id)
                );
            """);

        } catch (SQLException e) {
            throw new ConstraintViolationException(
                    "Failed to initialize database",
                    e
            );
        }
    }
}