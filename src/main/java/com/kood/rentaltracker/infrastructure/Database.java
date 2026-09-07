package com.kood.rentaltracker.infrastructure;

import com.kood.rentaltracker.repository.exception.DatabaseOpenException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String DEFAULT_URL =
            "jdbc:sqlite:rental-tracker.db";

    private Database() {
    }

    public static Connection connect() {
        String url = System.getProperty(
                "rental.tracker.db.url",
                DEFAULT_URL
        );

        try {
            Connection connection =
                    DriverManager.getConnection(url);

            try (Statement statement =
                         connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }

            return connection;

        } catch (SQLException e) {
            throw new DatabaseOpenException(
                    "Cannot open database",
                    e
            );
        }
    }
}