package com.kood.rentaltracker.domain;

public class Item {
    private final int id;
    private final int ownerId;
    private final String name;
    private final String description;
    private final double costPerDay;
    private ItemStatus status;

    public Item(
            int id,
            int ownerId,
            String name,
            String description,
            double costPerDay,
            ItemStatus status
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.costPerDay = costPerDay;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getCostPerDay() {
        return costPerDay;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }
}