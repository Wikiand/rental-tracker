package com.kood.rentaltracker;

import com.kood.rentaltracker.repository.exception.MappingException;
import com.kood.rentaltracker.repository.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionCoverageTest {

    @Test
    void mappingExceptionStoresMessageAndCause() {
        RuntimeException cause =
                new RuntimeException("database problem");

        MappingException exception =
                new MappingException(
                        "Mapping failed",
                        cause
                );

        assertEquals(
                "Mapping failed",
                exception.getMessage()
        );

        assertSame(
                cause,
                exception.getCause()
        );
    }

    @Test
    void notFoundExceptionStoresMessage() {
        NotFoundException exception =
                new NotFoundException("Item not found");

        assertEquals(
                "Item not found",
                exception.getMessage()
        );
    }
}
