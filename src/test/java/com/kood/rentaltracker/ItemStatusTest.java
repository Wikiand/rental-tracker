
package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.ItemStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStatusTest {

    @Test
    void availableCanBeRented() {
        assertTrue(
                ItemStatus.AVAILABLE.canTransitionTo(
                        ItemStatus.RENTED
                )
        );
    }

    @Test
    void availableCanBeDelisted() {
        assertTrue(
                ItemStatus.AVAILABLE.canTransitionTo(
                        ItemStatus.UNLISTED
                )
        );
    }

    @Test
    void unlistedCanBeRelisted() {
        assertTrue(
                ItemStatus.UNLISTED.canTransitionTo(
                        ItemStatus.AVAILABLE
                )
        );
    }

    @Test
    void rentedCanBeReturned() {
        assertTrue(
                ItemStatus.RENTED.canTransitionTo(
                        ItemStatus.AVAILABLE
                )
        );
    }

    @Test
    void rentedCanBeDelisted() {
        assertTrue(
                ItemStatus.RENTED.canTransitionTo(
                        ItemStatus.UNLISTED
                )
        );
    }

    @Test
    void rentedCannotBeRentedAgain() {
        assertFalse(
                ItemStatus.RENTED.canTransitionTo(
                        ItemStatus.RENTED
                )
        );
    }

    @Test
    void unlistedCannotBeRentedDirectly() {
        assertFalse(
                ItemStatus.UNLISTED.canTransitionTo(
                        ItemStatus.RENTED
                )
        );
    }

    @Test
    void availableCannotBeReturned() {
        assertFalse(
                ItemStatus.AVAILABLE.canTransitionTo(
                        ItemStatus.AVAILABLE
                )
        );
    }
}