package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.RentalStatus;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.repository.ItemRepository;
import com.kood.rentaltracker.repository.RentalRepository;
import com.kood.rentaltracker.repository.UserRepository;
import com.kood.rentaltracker.service.RentalService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RentalServiceTest {

    @Test
    void recordsRentalForAvailableItem() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();
        FakeUserRepository users = new FakeUserRepository();

        Item item = availableItem();
        items.item = item;

        User renter = new User(2, "james");
        users.user = renter;

        RentalService service =
                new RentalService(
                        rentals,
                        items,
                        users
                );

        Rental rental = service.recordRental(
                item,
                renter.getId(),
                3
        );

        assertNotNull(rental);
        assertEquals(
                RentalStatus.ACTIVE,
                rental.getStatus()
        );
        assertEquals(
                renter.getId(),
                rental.getRenterId()
        );
        assertEquals(
                ItemStatus.RENTED,
                item.getStatus()
        );
        assertNotNull(users.user);
        assertEquals(
                "james",
                users.user.getUsername()
        );
        assertEquals(
                3,
                java.time.Duration.between(
                        rental.getStartTime(),
                        rental.getEndTime()
                ).toDays()
        );
    }

    @Test
    void rejectsInvalidRentalDuration() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.recordRental(
                        availableItem(),
                        2,
                        0
                )
        );
    }

    @Test
    void rejectsNegativeRentalDuration() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.recordRental(
                        availableItem(),
                        2,
                        -1
                )
        );
    }

    @Test
    void rejectsUnavailableItem() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Item item = new Item(
                1,
                1,
                "Ladder",
                "6-step aluminium ladder",
                5.0,
                ItemStatus.RENTED
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.recordRental(
                        item,
                        2,
                        3
                )
        );
    }

    @Test
    void rejectsOwnItem() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Item item = availableItem();

        assertThrows(
                IllegalStateException.class,
                () -> service.recordRental(
                        item,
                        item.getOwnerId(),
                        3
                )
        );
    }

    @Test
    void rejectsUnknownRenter() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.recordRental(
                        availableItem(),
                        999,
                        3
                )
        );
    }

    @Test
    void rejectsNullItem() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.recordRental(
                        null,
                        2,
                        3
                )
        );
    }

    @Test
    void confirmsReturn() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();

        Item item = availableItem();
        item.setStatus(ItemStatus.RENTED);
        items.item = item;

        Rental rental = activeRental();
        rentals.rental = rental;

        RentalService service =
                new RentalService(
                        rentals,
                        items,
                        new FakeUserRepository()
                );

        service.confirmReturn(
                item,
                rental,
                item.getOwnerId()
        );

        assertEquals(
                ItemStatus.AVAILABLE,
                item.getStatus()
        );
        assertEquals(
                RentalStatus.CLOSED,
                rental.getStatus()
        );
        assertNotNull(
                rental.getReturnedAt()
        );
        assertEquals(
                rental.getId(),
                rentals.closedRentalId
        );
    }

    @Test
    void confirmsReturnKeepsDelistedItemUnlisted() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();

        Item item = availableItem();
        item.setStatus(ItemStatus.UNLISTED);
        items.item = item;

        Rental rental = activeRental();
        rentals.rental = rental;

        RentalService service =
                new RentalService(
                        rentals,
                        items,
                        new FakeUserRepository()
                );

        service.confirmReturn(
                item,
                rental,
                item.getOwnerId()
        );

        assertEquals(
                ItemStatus.UNLISTED,
                item.getStatus()
        );
        assertEquals(
                RentalStatus.CLOSED,
                rental.getStatus()
        );
    }

    @Test
    void rejectsReturnByNonOwner() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Item item = availableItem();
        item.setStatus(ItemStatus.RENTED);

        Rental rental = activeRental();

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmReturn(
                        item,
                        rental,
                        999
                )
        );
    }

    @Test
    void rejectsReturnForDifferentItem() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Item item = availableItem();
        item.setStatus(ItemStatus.RENTED);

        Rental rental = new Rental(
                1,
                999,
                2,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().plusDays(1),
                null,
                RentalStatus.ACTIVE
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmReturn(
                        item,
                        rental,
                        item.getOwnerId()
                )
        );
    }

    @Test
    void rejectsClosedRentalReturn() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Item item = availableItem();
        item.setStatus(ItemStatus.RENTED);

        Rental rental = new Rental(
                1,
                1,
                2,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                RentalStatus.CLOSED
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmReturn(
                        item,
                        rental,
                        item.getOwnerId()
                )
        );
    }

    @Test
    void rejectsReturnForItemNotInReturnableState() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Item item = availableItem();

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmReturn(
                        item,
                        activeRental(),
                        item.getOwnerId()
                )
        );
    }

    @Test
    void getsActiveRentals() {
        FakeRentalRepository rentals =
                new FakeRentalRepository();

        Rental rental = activeRental();
        rentals.rental = rental;

        RentalService service =
                new RentalService(
                        rentals,
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        List<Rental> result =
                service.getActiveRentals(1);

        assertEquals(1, result.size());
        assertEquals(
                rental,
                result.get(0)
        );
        assertEquals(
                1,
                rentals.lastOwnerId
        );
    }

    @Test
    void getsRentalHistory() {
        FakeRentalRepository rentals =
                new FakeRentalRepository();

        Rental rental = activeRental();
        rentals.rental = rental;

        RentalService service =
                new RentalService(
                        rentals,
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        List<Rental> result =
                service.getRentalHistory(1);

        assertEquals(1, result.size());
        assertEquals(
                rental,
                result.get(0)
        );
        assertEquals(
                1,
                rentals.lastOwnerId
        );
    }

    @Test
    void getsRentalById() {
        FakeRentalRepository rentals =
                new FakeRentalRepository();

        Rental rental = activeRental();
        rentals.rental = rental;

        RentalService service =
                new RentalService(
                        rentals,
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        Rental result =
                service.getRental(1);

        assertEquals(
                rental,
                result
        );
    }

    @Test
    void missingRentalThrowsError() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRental(999)
        );
    }

    @Test
    void getsRenter() {
        FakeUserRepository users =
                new FakeUserRepository();

        users.user =
                new User(2, "james");

        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        users
                );

        User result =
                service.getRenter(activeRental());

        assertEquals(
                2,
                result.getId()
        );
        assertEquals(
                "james",
                result.getUsername()
        );
    }

    @Test
    void missingRenterThrowsError() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRenter(activeRental())
        );
    }

    @Test
    void getsItem() {
        FakeItemRepository items =
                new FakeItemRepository();

        Item item = availableItem();
        items.item = item;

        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        items,
                        new FakeUserRepository()
                );

        Item result =
                service.getItem(activeRental());

        assertEquals(
                item,
                result
        );
    }

    @Test
    void missingItemThrowsError() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getItem(activeRental())
        );
    }


    @Test
    void rejectsNullItemDuringReturnConfirmation() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmReturn(
                        null,
                        activeRental(),
                        1
                )
        );
    }

    @Test
    void rejectsNullRentalDuringReturnConfirmation() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmReturn(
                        availableItem(),
                        null,
                        1
                )
        );
    }

    @Test
    void rejectsInvalidRentalId() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRental(0)
        );
    }

    @Test
    void rejectsNullRentalWhenGettingRenter() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRenter(null)
        );
    }

    @Test
    void rejectsNullRentalWhenGettingItem() {
        RentalService service =
                new RentalService(
                        new FakeRentalRepository(),
                        new FakeItemRepository(),
                        new FakeUserRepository()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getItem(null)
        );
    }

    private static Item availableItem() {
        return new Item(
                1,
                1,
                "Ladder",
                "6-step aluminium ladder",
                5.0,
                ItemStatus.AVAILABLE
        );
    }

    private static Rental activeRental() {
        LocalDateTime start =
                LocalDateTime.now();

        return new Rental(
                1,
                1,
                2,
                start,
                start.plusDays(3),
                null,
                RentalStatus.ACTIVE
        );
    }

    private static class FakeItemRepository
            implements ItemRepository {

        private Item item;

        @Override
        public Item save(Item item) {
            this.item = item;
            return item;
        }

        @Override
        public Optional<Item> findById(int id) {
            if (item != null && item.getId() == id) {
                return Optional.of(item);
            }

            return Optional.empty();
        }

        @Override
        public List<Item> findByOwnerId(int ownerId) {
            return item == null
                    ? List.of()
                    : List.of(item);
        }

        @Override
        public List<Item> findByOwnerIdAndStatus(
                int ownerId,
                ItemStatus status
        ) {
            if (item != null
                    && item.getStatus() == status) {
                return List.of(item);
            }

            return List.of();
        }

        @Override
        public List<Item> findAvailableExcludingOwner(
                int ownerId
        ) {
            if (item == null
                    || item.getOwnerId() == ownerId
                    || item.getStatus() != ItemStatus.AVAILABLE) {
                return List.of();
            }

            return List.of(item);
        }

        @Override
        public void updateStatus(
                int itemId,
                ItemStatus status
        ) {
            if (item == null) {
                throw new IllegalStateException(
                        "Item not found"
                );
            }

            item.setStatus(status);
        }

        @Override
        public void updateStatusValue(
                int itemId,
                String status
        ) {
            if (item == null) {
                throw new IllegalStateException(
                        "Item not found"
                );
            }

            item.setStatus(
                    ItemStatus.valueOf(
                            status.toUpperCase()
                    )
            );
        }
    }

    private static class FakeRentalRepository
            implements RentalRepository {

        private Rental rental;
        private int closedRentalId;
        private int lastOwnerId;

        @Override
        public Rental save(Rental rental) {
            this.rental = rental;
            return rental;
        }

        @Override
        public Optional<Rental> findById(int id) {
            if (rental != null && rental.getId() == id) {
                return Optional.of(rental);
            }

            return Optional.empty();
        }

        @Override
        public List<Rental> findActiveByOwnerId(
                int ownerId
        ) {
            lastOwnerId = ownerId;

            if (rental == null
                    || rental.getStatus() != RentalStatus.ACTIVE) {
                return List.of();
            }

            return List.of(rental);
        }

        @Override
        public List<Rental> findByOwnerId(
                int ownerId
        ) {
            lastOwnerId = ownerId;

            return rental == null
                    ? List.of()
                    : List.of(rental);
        }

        @Override
        public void closeRental(
                int rentalId,
                String returnedAt
        ) {
            closedRentalId = rentalId;
        }
    }

    private static class FakeUserRepository
            implements UserRepository {

        private User user;

        @Override
        public User save(
                User user,
                String passwordHash
        ) {
            this.user = new User(
                    user.getId() == 0 ? 2 : user.getId(),
                    user.getUsername()
            );

            return this.user;
        }

        @Override
        public Optional<User> findById(int id) {
            if (user != null && user.getId() == id) {
                return Optional.of(user);
            }

            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsername(
                String username
        ) {
            if (user != null
                    && user.getUsername().equals(username)) {
                return Optional.of(user);
            }

            return Optional.empty();
        }

        @Override
        public Optional<String> findPasswordHashByUsername(
                String username
        ) {
            return Optional.empty();
        }
    }
}
