package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.repository.ItemRepository;
import com.kood.rentaltracker.service.ItemService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ItemServiceTest {

    @Test
    void listsItemAsAvailable() {
        FakeItemRepository repository =
                new FakeItemRepository();

        ItemService service =
                new ItemService(repository);

        Item item = service.listItem(
                1,
                "Ladder",
                "6-step aluminium ladder",
                5.0
        );

        assertEquals(
                "Ladder",
                item.getName()
        );
        assertEquals(
                ItemStatus.AVAILABLE,
                item.getStatus()
        );
        assertEquals(
                1,
                item.getOwnerId()
        );
        assertEquals(
                item,
                repository.savedItem
        );
    }

    @Test
    void getsInventory() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item expected = createItem();

        repository.items = List.of(expected);

        ItemService service =
                new ItemService(repository);

        List<Item> result =
                service.getInventory(1);

        assertEquals(
                1,
                result.size()
        );
        assertEquals(
                expected,
                result.get(0)
        );
    }

    @Test
    void getsAvailableItemsExcludingCurrentUsersOwnItems() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item ownItem = createItem();

        Item otherUsersItem = new Item(
                2,
                2,
                "Camera",
                "Digital camera",
                10.0,
                ItemStatus.AVAILABLE
        );

        repository.items =
                List.of(
                        ownItem,
                        otherUsersItem
                );

        ItemService service =
                new ItemService(repository);

        List<Item> result =
                service.getAvailableItems(1);

        assertEquals(
                1,
                result.size()
        );
        assertEquals(
                otherUsersItem,
                result.get(0)
        );
        assertNotEquals(
                1,
                result.get(0).getOwnerId()
        );
        assertEquals(
                ItemStatus.AVAILABLE,
                result.get(0).getStatus()
        );
    }

    @Test
    void changesAvailableItemToUnlisted() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        service.changeStatus(
                item,
                ItemStatus.UNLISTED
        );

        assertEquals(
                ItemStatus.UNLISTED,
                item.getStatus()
        );

        assertEquals(
                ItemStatus.UNLISTED,
                repository.updatedStatus
        );
    }

    @Test
    void changesUnlistedItemToAvailable() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();
        item.setStatus(ItemStatus.UNLISTED);

        ItemService service =
                new ItemService(repository);

        service.changeStatus(
                item,
                ItemStatus.AVAILABLE
        );

        assertEquals(
                ItemStatus.AVAILABLE,
                item.getStatus()
        );
    }

    @Test
    void changesRentedItemToUnlisted() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();
        item.setStatus(ItemStatus.RENTED);

        ItemService service =
                new ItemService(repository);

        service.changeStatus(
                item,
                ItemStatus.UNLISTED
        );

        assertEquals(
                ItemStatus.UNLISTED,
                item.getStatus()
        );
    }

    @Test
    void rejectsInvalidStatusTransition() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        assertThrows(
                IllegalStateException.class,
                () -> service.changeStatus(
                        item,
                        ItemStatus.AVAILABLE
                )
        );
    }

    @Test
    void rejectsNullName() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        null,
                        "description",
                        5.0
                )
        );
    }

    @Test
    void rejectsBlankName() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        " ",
                        "description",
                        5.0
                )
        );
    }

    @Test
    void rejectsNullDescription() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        "Ladder",
                        null,
                        5.0
                )
        );
    }

    @Test
    void rejectsBlankDescription() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        "Ladder",
                        " ",
                        5.0
                )
        );
    }

    @Test
    void rejectsNegativeCost() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        "Ladder",
                        "description",
                        -1.0
                )
        );
    }

    @Test
    void rejectsZeroCost() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        "Ladder",
                        "description",
                        0.0
                )
        );
    }

    @Test
    void rejectsNaNCost() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        "Ladder",
                        "description",
                        Double.NaN
                )
        );
    }

    @Test
    void rejectsInfiniteCost() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        1,
                        "Ladder",
                        "description",
                        Double.POSITIVE_INFINITY
                )
        );
    }

    @Test
    void rejectsInvalidOwnerId() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listItem(
                        0,
                        "Ladder",
                        "description",
                        5.0
                )
        );
    }

    @Test
    void trimsItemFields() {
        FakeItemRepository repository =
                new FakeItemRepository();

        ItemService service =
                new ItemService(repository);

        Item item = service.listItem(
                1,
                " Ladder ",
                " description ",
                5.0
        );

        assertEquals(
                "Ladder",
                item.getName()
        );

        assertEquals(
                "description",
                item.getDescription()
        );
    }

    @Test
    void ownerCanChangeOwnItemStatus() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        service.changeStatus(
                1,
                item,
                ItemStatus.UNLISTED
        );

        assertEquals(
                ItemStatus.UNLISTED,
                item.getStatus()
        );

        assertEquals(
                ItemStatus.UNLISTED,
                repository.updatedStatus
        );
    }

    @Test
    void nonOwnerCannotChangeItemStatus() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.changeStatus(
                                2,
                                item,
                                ItemStatus.UNLISTED
                        )
                );

        assertEquals(
                "You can only manage your own items.",
                exception.getMessage()
        );

        assertNull(
                repository.updatedStatus
        );

        assertEquals(
                ItemStatus.AVAILABLE,
                item.getStatus()
        );
    }

    @Test
    void ownershipCheckHappensBeforeStatusUpdate() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        assertThrows(
                IllegalStateException.class,
                () -> service.changeStatus(
                        99,
                        item,
                        ItemStatus.RENTED
                )
        );

        assertNull(
                repository.updatedStatus
        );
    }

    @Test
    void rejectsInvalidCurrentUserId() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStatus(
                        0,
                        item,
                        ItemStatus.UNLISTED
                )
        );
    }

    @Test
    void rejectsNullItemForOwnershipAwareChange() {
        FakeItemRepository repository =
                new FakeItemRepository();

        ItemService service =
                new ItemService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStatus(
                        1,
                        null,
                        ItemStatus.UNLISTED
                )
        );
    }

    @Test
    void rejectsNullStatusForOwnershipAwareChange() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStatus(
                        1,
                        item,
                        null
                )
        );
    }

    @Test
    void ownershipAwareChangeRejectsInvalidTransition() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item item = createItem();

        ItemService service =
                new ItemService(repository);

        assertThrows(
                IllegalStateException.class,
                () -> service.changeStatus(
                        1,
                        item,
                        ItemStatus.AVAILABLE
                )
        );

        assertNull(
                repository.updatedStatus
        );
    }


    @Test
    void inventoryExcludesUnlistedItems() {
        FakeItemRepository repository =
                new FakeItemRepository();

        Item available = createItem();

        Item unlisted = new Item(
                2,
                1,
                "Old item",
                "No longer listed",
                10.0,
                ItemStatus.UNLISTED
        );

        repository.items =
                List.of(available, unlisted);

        ItemService service =
                new ItemService(repository);

        List<Item> result =
                service.getInventory(1);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                available,
                result.get(0)
        );
    }

    @Test
    void legacyChangeStatusRejectsNullItem() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStatus(
                        null,
                        ItemStatus.UNLISTED
                )
        );
    }

    @Test
    void legacyChangeStatusRejectsNullStatus() {
        ItemService service =
                new ItemService(
                        new FakeItemRepository()
                );

        Item item = createItem();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStatus(
                        item,
                        null
                )
        );
    }

    private Item createItem() {
        return new Item(
                1,
                1,
                "Ladder",
                "6-step aluminium ladder",
                5.0,
                ItemStatus.AVAILABLE
        );
    }

    private static class FakeItemRepository
            implements ItemRepository {

        private Item savedItem;
        private ItemStatus updatedStatus;
        private List<Item> items = List.of();

        @Override
        public Item save(Item item) {
            savedItem = new Item(
                    1,
                    item.getOwnerId(),
                    item.getName(),
                    item.getDescription(),
                    item.getCostPerDay(),
                    item.getListingDate(),
                    item.getStatus()
            );

            return savedItem;
        }

        @Override
        public Optional<Item> findById(int id) {
            return items.stream()
                    .filter(item ->
                            item.getId() == id
                    )
                    .findFirst();
        }

        @Override
        public List<Item> findByOwnerId(
                int ownerId
        ) {
            return items;
        }

        @Override
        public List<Item> findByOwnerIdAndStatus(
                int ownerId,
                ItemStatus status
        ) {
            return items.stream()
                    .filter(item ->
                            item.getStatus() == status
                    )
                    .toList();
        }

        @Override
        public List<Item> findAvailableExcludingOwner(
                int ownerId
        ) {
            return items.stream()
                    .filter(item ->
                            item.getOwnerId() != ownerId
                    )
                    .filter(item ->
                            item.getStatus()
                                    == ItemStatus.AVAILABLE
                    )
                    .toList();
        }

        @Override
        public void updateStatus(
                int itemId,
                ItemStatus status
        ) {
            updatedStatus = status;
        }

        @Override
        public void updateStatusValue(
                int itemId,
                String status
        ) {
            updatedStatus =
                    ItemStatus.valueOf(
                            status.toUpperCase()
                    );
        }
    }
}
