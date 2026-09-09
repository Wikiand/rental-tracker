package com.kood.rentaltracker;

import com.kood.rentaltracker.repository.exception.ConstraintViolationException;
import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DatabaseExceptionMapperTest {

    @Test
    void mapsUniqueViolationToConstraintException() {
        RuntimeException result =
                DatabaseExceptionMapper.map(
                        new SQLException("UNIQUE constraint failed")
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                result
        );

        assertEquals(
                "Database constraint violation",
                result.getMessage()
        );
    }

    @Test
    void mapsNotNullViolationToConstraintException() {
        RuntimeException result =
                DatabaseExceptionMapper.map(
                        new SQLException("NOT NULL constraint failed")
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                result
        );
    }

    @Test
    void mapsCheckViolationToConstraintException() {
        RuntimeException result =
                DatabaseExceptionMapper.map(
                        new SQLException("CHECK constraint failed")
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                result
        );
    }

    @Test
    void mapsForeignKeyViolationToConstraintException() {
        RuntimeException result =
                DatabaseExceptionMapper.map(
                        new SQLException("FOREIGN KEY constraint failed")
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                result
        );
    }

    @Test
    void mapsUnknownDatabaseErrorToRuntimeException() {
        RuntimeException result =
                DatabaseExceptionMapper.map(
                        new SQLException("Connection unexpectedly closed")
                );

        assertEquals(
                RuntimeException.class,
                result.getClass()
        );

        assertEquals(
                "Database error",
                result.getMessage()
        );
    }

    @Test
    void handlesNullSqlExceptionMessage() {
        RuntimeException result =
                DatabaseExceptionMapper.map(
                        new SQLException((String) null)
                );

        assertEquals(
                RuntimeException.class,
                result.getClass()
        );

        assertEquals(
                "Database error",
                result.getMessage()
        );
    }
}
