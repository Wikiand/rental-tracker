package com.kood.rentaltracker;

import com.kood.rentaltracker.validation.RentalValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RentalValidatorTest {

    private final RentalValidator validator =
            new RentalValidator();

    @Test
    void acceptsValidRentalValues() {
        assertDoesNotThrow(() ->
                validator.validate(
                        1,
                        2,
                        7
                )
        );
    }

    @Test
    void rejectsInvalidItemId() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                0,
                                2,
                                7
                        )
                );

        assertEquals(
                "Item reference is invalid.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsNegativeItemId() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                -1,
                                2,
                                7
                        )
                );

        assertEquals(
                "Item reference is invalid.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsInvalidRenterId() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                1,
                                0,
                                7
                        )
                );

        assertEquals(
                "Renter account is invalid.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsNegativeRenterId() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                1,
                                -1,
                                7
                        )
                );

        assertEquals(
                "Renter account is invalid.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsZeroDuration() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                1,
                                2,
                                0
                        )
                );

        assertEquals(
                "Rental duration must be greater than zero.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsNegativeDuration() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                1,
                                2,
                                -1
                        )
                );

        assertEquals(
                "Rental duration must be greater than zero.",
                exception.getMessage()
        );
    }

    @Test
    void validatesItemIdDirectly() {
        assertDoesNotThrow(() ->
                validator.validateItemId(1)
        );
    }

    @Test
    void validatesRenterIdDirectly() {
        assertDoesNotThrow(() ->
                validator.validateRenterId(1)
        );
    }

    @Test
    void validatesDurationDirectly() {
        assertDoesNotThrow(() ->
                validator.validateDurationDays(1)
        );
    }
}
