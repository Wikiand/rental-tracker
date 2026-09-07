package com.kood.rentaltracker.repository.exception;

import java.sql.SQLException;

public final class DatabaseExceptionMapper {

    private DatabaseExceptionMapper() {
    }

    public static RuntimeException map(SQLException e) {

        String message = e.getMessage() == null
                ? ""
                : e.getMessage().toLowerCase();

        if (message.contains("unique")
                || message.contains("not null")
                || message.contains("check")
                || message.contains("foreign key")) {
            return new ConstraintViolationException(
                    "Database constraint violation",
                    e
            );
        }

        return new RuntimeException("Database error", e);
    }
}