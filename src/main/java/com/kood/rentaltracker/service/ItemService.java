package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.repository.ItemRepository;

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
        return itemRepository.save(
                new Item(
                        0,
                        ownerId,
                        name,
                        description,
                        costPerDay,
                        ItemStatus.AVAILABLE
                )
        );
    }

    public void changeStatus(Item item, ItemStatus nextStatus) {
        if (!item.getStatus().canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: "
                            + item.getStatus()
                            + " -> "
                            + nextStatus
            );
        }

        itemRepository.updateStatus(item.getId(), nextStatus);
        item.setStatus(nextStatus);
    }
}