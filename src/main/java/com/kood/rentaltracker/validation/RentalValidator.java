package com.kood.rentaltracker.validation;

public class RentalValidator {

    public void validateItemId(int itemId) {
        if (itemId <= 0) {
            throw new IllegalArgumentException(
                    "Item reference is invalid."
            );
        }
    }

    public void validateRenterId(int renterId) {
        if (renterId <= 0) {
            throw new IllegalArgumentException(
                    "Renter account is invalid."
            );
        }
    }

    public void validateDurationDays(int durationDays) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException(
                    "Rental duration must be greater than zero."
            );
        }
    }

    public void validate(
            int itemId,
            int renterId,
            int durationDays
    ) {
        validateItemId(itemId);
        validateRenterId(renterId);
        validateDurationDays(durationDays);
    }
}
