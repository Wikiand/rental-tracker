package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.repository.ItemRepository;

import java.util.List;

public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item listItem(
            int ownerId,
            String name,
            String description,
            double costPerDay
    ) {
        validateItem(name, description, costPerDay);

        return itemRepository.save(
                new Item(
                        0,
                        ownerId,
                        name.trim(),
                        description.trim(),
                        costPerDay,
                        ItemStatus.AVAILABLE
                )
        );
    }

    public List<Item> getInventory(int ownerId) {
        return itemRepository.findByOwnerId(ownerId);
    }

    public List<Item> getAvailableItems(int ownerId) {
        return itemRepository.findByOwnerIdAndStatus(
                ownerId,
                ItemStatus.AVAILABLE
        );
    }

    public void changeStatus(
            Item item,
            ItemStatus nextStatus
    ) {
        if (!item.getStatus().canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: "
                            + item.getStatus()
                            + " -> "
                            + nextStatus
            );
        }

        itemRepository.updateStatus(
                item.getId(),
                nextStatus
        );

        item.setStatus(nextStatus);
    }

    private void validateItem(
            String name,
            String description,
            double costPerDay
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Item name cannot be empty."
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Item description cannot be empty."
            );
        }

        if (costPerDay < 0) {
            throw new IllegalArgumentException(
                    "Cost cannot be negative."
            );
        }
    }
}