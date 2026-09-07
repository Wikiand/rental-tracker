package com.kood.rentaltracker.repository;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

    Item save(Item item);

    Optional<Item> findById(int id);

    List<Item> findByOwnerId(int ownerId);

    List<Item> findByOwnerIdAndStatus(int ownerId, ItemStatus status);

    void updateStatus(int itemId, ItemStatus status);
}