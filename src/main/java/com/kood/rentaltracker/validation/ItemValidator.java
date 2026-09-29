package com.kood.rentaltracker.validation;

public class ItemValidator {

    public void validateOwnerId(int ownerId) {
        if (ownerId <= 0) {
            throw new IllegalArgumentException(
                    "Owner account is invalid."
            );
        }
    }

    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Item name cannot be empty."
            );
        }
    }

    public void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Item description cannot be empty."
            );
        }
    }

    public void validateCostPerDay(double costPerDay) {
        if (Double.isNaN(costPerDay)
                || Double.isInfinite(costPerDay)
                || costPerDay <= 0) {

            throw new IllegalArgumentException(
                    "Cost per day must be greater than zero."
            );
        }
    }

    public void validate(
            int ownerId,
            String name,
            String description,
            double costPerDay
    ) {
        validateOwnerId(ownerId);
        validateName(name);
        validateDescription(description);
        validateCostPerDay(costPerDay);
    }
}
