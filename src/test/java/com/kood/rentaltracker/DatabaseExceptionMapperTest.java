package com.kood.rentaltracker;

import com.kood.rentaltracker.repository.exception.ConstraintViolationException;
import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class DatabaseExceptionMapperTest {

    @Test
    void mapsUniqueViolationToConstraintException() {
        assertConstraintViolation(
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
        );
    }

    @Test
    void mapsNotNullViolationToConstraintException() {
        assertConstraintViolation(
                SQLiteErrorCode.SQLITE_CONSTRAINT_NOTNULL
        );
    }

    @Test
    void mapsCheckViolationToConstraintException() {
        assertConstraintViolation(
                SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK
        );
    }

    @Test
    void mapsForeignKeyViolationToConstraintException() {
        assertConstraintViolation(
                SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY
        );
    }

    @Test
    void mapsUnknownSqliteErrorToRuntimeException() {
        SQLiteException sqlException =
                new SQLiteException(
                        "Database is busy",
                        SQLiteErrorCode.SQLITE_BUSY
                );

        RuntimeException result =
                DatabaseExceptionMapper.map(sqlException);

        assertEquals(
                RuntimeException.class,
                result.getClass()
        );

        assertEquals("Database error", result.getMessage());
        assertSame(sqlException, result.getCause());
    }

    @Test
    void doesNotClassifyGenericSqlExceptionByMessage() {
        SQLException sqlException =
                new SQLException("UNIQUE constraint failed");

        RuntimeException result =
                DatabaseExceptionMapper.map(sqlException);

        assertEquals(
                RuntimeException.class,
                result.getClass()
        );

        assertSame(sqlException, result.getCause());
    }

    @Test
    void handlesNullSqliteExceptionMessage() {
        SQLiteException sqlException =
                new SQLiteException(
                        null,
                        SQLiteErrorCode.SQLITE_BUSY
                );

        RuntimeException result =
                DatabaseExceptionMapper.map(sqlException);

        assertEquals(
                RuntimeException.class,
                result.getClass()
        );

        assertEquals("Database error", result.getMessage());
    }

    private void assertConstraintViolation(
            SQLiteErrorCode errorCode
    ) {
        SQLiteException sqlException =
                new SQLiteException(
                        "SQLite constraint violation",
                        errorCode
                );

        RuntimeException result =
                DatabaseExceptionMapper.map(sqlException);

        assertInstanceOf(
                ConstraintViolationException.class,
                result
        );

        assertEquals(
                "Database constraint violation",
                result.getMessage()
        );

        assertSame(
                sqlException,
                result.getCause()
        );
    }
}
