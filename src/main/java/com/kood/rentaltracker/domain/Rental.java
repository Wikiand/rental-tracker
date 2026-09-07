package com.kood.rentaltracker.domain;

import java.time.LocalDateTime;

public class Rental {
    private final int id;
    private final int itemId;
    private final int renterId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private LocalDateTime returnedAt;
    private RentalStatus status;

    public Rental(
            int id,
            int itemId,
            int renterId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime returnedAt,
            RentalStatus status
    ) {
        this.id = id;
        this.itemId = itemId;
        this.renterId = renterId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.returnedAt = returnedAt;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getItemId() {
        return itemId;
    }

    public int getRenterId() {
        return renterId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }
}