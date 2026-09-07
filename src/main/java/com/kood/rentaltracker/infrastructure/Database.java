package com.kood.rentaltracker.infrastructure;

import com.kood.rentaltracker.repository.exception.DatabaseOpenException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:sqlite:rental-tracker.db";

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            throw new DatabaseOpenException(
                    "Cannot open database",
                    e
            );
        }
    }
}