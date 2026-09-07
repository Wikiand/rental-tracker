package com.kood.rentaltracker.domain;

public enum ItemStatus {
    AVAILABLE,
    RENTED,
    UNLISTED;

    public boolean canTransitionTo(ItemStatus next) {
        return switch (this) {
            case AVAILABLE -> next == RENTED || next == UNLISTED;
            case RENTED -> next == AVAILABLE || next == UNLISTED;
            case UNLISTED -> next == AVAILABLE;
        };
    }
}