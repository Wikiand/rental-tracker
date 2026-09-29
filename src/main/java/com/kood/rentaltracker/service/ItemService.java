package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.logging.AppLogger;
import com.kood.rentaltracker.repository.ItemRepository;
import com.kood.rentaltracker.validation.ItemValidator;

import java.util.List;

public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemValidator itemValidator;
    private final AppLogger logger;

    public ItemService(
            ItemRepository itemRepository
    ) {
        this(
                itemRepository,
                new ItemValidator(),
                new AppLogger()
        );
    }

    public ItemService(
            ItemRepository itemRepository,
            ItemValidator itemValidator
    ) {
        this(
                itemRepository,
                itemValidator,
                new AppLogger()
        );
    }

    public ItemService(
            ItemRepository itemRepository,
            ItemValidator itemValidator,
            AppLogger logger
    ) {
        this.itemRepository = itemRepository;
        this.itemValidator = itemValidator;
        this.logger = logger;
    }

    public Item listItem(
            int ownerId,
            String name,
            String description,
            double costPerDay
    ) {
        try {
            itemValidator.validate(
                    ownerId,
                    name,
                    description,
                    costPerDay
            );
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "item.validation.failed",
                    "userId=" + ownerId
                            + " reason=" + safeMessage(e)
            );

            throw e;
        }

        Item item = new Item(
                0,
                ownerId,
                name.trim(),
                description.trim(),
                costPerDay,
                ItemStatus.AVAILABLE
        );

        Item savedItem =
                itemRepository.save(item);

        logger.info(
                "item.listed",
                "userId=" + ownerId
                        + " itemId=" + savedItem.getId()
        );

        return savedItem;
    }

    public List<Item> getInventory(
            int ownerId
    ) {
        itemValidator.validateOwnerId(ownerId);

        return itemRepository
                .findByOwnerId(ownerId)
                .stream()
                .filter(item ->
                        item.getStatus()
                                != ItemStatus.UNLISTED
                )
                .toList();
    }

    public List<Item> getAvailableItems(
            int ownerId
    ) {
        itemValidator.validateOwnerId(ownerId);

        return itemRepository
                .findAvailableExcludingOwner(
                        ownerId
                );
    }

    /*
     * Existing method retained for backwards compatibility
     * with the current test suite.
     */
    public void changeStatus(
            Item item,
            ItemStatus nextStatus
    ) {
        if (item == null) {
            throw new IllegalArgumentException(
                    "Item cannot be null."
            );
        }

        if (nextStatus == null) {
            throw new IllegalArgumentException(
                    "Item status cannot be null."
            );
        }

        itemValidator.validateOwnerId(
                item.getOwnerId()
        );

        validateStatusTransition(
                item,
                nextStatus
        );

        itemRepository.updateStatus(
                item.getId(),
                nextStatus
        );

        item.setStatus(nextStatus);
    }

    /*
     * Ownership-aware version.
     *
     * This is the method application code should use when
     * a logged-in user changes an item status.
     */
    public void changeStatus(
            int currentUserId,
            Item item,
            ItemStatus nextStatus
    ) {
        itemValidator.validateOwnerId(
                currentUserId
        );

        if (item == null) {
            throw new IllegalArgumentException(
                    "Item cannot be null."
            );
        }

        if (nextStatus == null) {
            throw new IllegalArgumentException(
                    "Item status cannot be null."
            );
        }

        if (item.getOwnerId() != currentUserId) {
            throw new IllegalStateException(
                    "You can only manage your own items."
            );
        }

        validateStatusTransition(
                item,
                nextStatus
        );

        itemRepository.updateStatus(
                item.getId(),
                nextStatus
        );

        item.setStatus(nextStatus);
    }

    private void validateStatusTransition(
            Item item,
            ItemStatus nextStatus
    ) {
        if (!item.getStatus()
                .canTransitionTo(nextStatus)) {

            throw new IllegalStateException(
                    "Invalid status transition: "
                            + item.getStatus()
                            + " -> "
                            + nextStatus
            );
        }
    }

    private String safeMessage(
            Exception exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "validation-error";
        }

        return message
                .replace(" ", "_")
                .replace(
                        System.lineSeparator(),
                        ""
                );
    }
}
