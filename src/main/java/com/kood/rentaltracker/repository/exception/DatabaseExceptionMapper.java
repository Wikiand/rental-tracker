package com.kood.rentaltracker.repository.exception;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.SQLException;

public final class DatabaseExceptionMapper {

    private DatabaseExceptionMapper() {
    }

    public static RuntimeException map(SQLException exception) {

        if (exception instanceof SQLiteException sqliteException) {
            SQLiteErrorCode resultCode =
                    sqliteException.getResultCode();

            if (resultCode != null
                    && (resultCode.code & 0xFF)
                    == SQLiteErrorCode.SQLITE_CONSTRAINT.code) {

                return new ConstraintViolationException(
                        "Database constraint violation",
                        exception
                );
            }
        }

        return new RuntimeException(
                "Database error",
                exception
        );
    }
}
