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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RentalServiceTest {

    @Test
    void recordsRentalForAvailableItem() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();
        FakeUserRepository users = new FakeUserRepository();

        Item item = new Item(
                1,
                1,
                "Ladder",
                "6-step aluminium ladder",
                5.0,
                ItemStatus.AVAILABLE
        );

        items.item = item;

        RentalService service =
                new RentalService(rentals, items, users);

        Rental rental = service.recordRental(
                item,
                "james",
                3
        );

        assertNotNull(rental);
        assertEquals(RentalStatus.ACTIVE, rental.getStatus());
        assertEquals(ItemStatus.RENTED, item.getStatus());
        assertEquals("james", users.user.getUsername());
        assertEquals(3, java.time.Duration.between(
                rental.getStartTime(),
                rental.getEndTime()
        ).toDays());
    }

    @Test
    void rejectsInvalidRentalDuration() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();
        FakeUserRepository users = new FakeUserRepository();

        Item item = availableItem();

        RentalService service =
                new RentalService(rentals, items, users);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.recordRental(
                        item,
                        "james",
                        0
                )
        );
    }

    @Test
    void rejectsUnavailableItem() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();
        FakeUserRepository users = new FakeUserRepository();

        Item item = new Item(
                1,
                1,
                "Ladder",
                "6-step aluminium ladder",
                5.0,
                ItemStatus.RENTED
        );

        RentalService service =
                new RentalService(rentals, items, users);

        assertThrows(
                IllegalStateException.class,
                () -> service.recordRental(
                        item,
                        "james",
                        3
                )
        );
    }

    @Test
    void confirmsReturn() {
        FakeItemRepository items = new FakeItemRepository();
        FakeRentalRepository rentals = new FakeRentalRepository();
        FakeUserRepository users = new FakeUserRepository();

        Item item = availableItem();
        item.setStatus(ItemStatus.RENTED);
        items.item = item;

        Rental rental = new Rental(
                1,
                1,
                2,
                java.time.LocalDateTime.now().minusDays(1),
                java.time.LocalDateTime.now().plusDays(2),
                null,
                RentalStatus.ACTIVE
        );

        rentals.rental = rental;

        RentalService service =
                new RentalService(rentals, items, users);

        service.confirmReturn(item, rental);

        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
        assertEquals(1, rentals.closedRentalId);
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
            return Optional.ofNullable(item);
        }

        @Override
        public java.util.List<Item> findByOwnerId(int ownerId) {
            return java.util.List.of(item);
        }

        @Override
        public java.util.List<Item> findByOwnerIdAndStatus(
                int ownerId,
                ItemStatus status
        ) {
            if (item != null && item.getStatus() == status) {
                return java.util.List.of(item);
            }

            return java.util.List.of();
        }

        @Override
        public void updateStatus(
                int itemId,
                ItemStatus status
        ) {
            item.setStatus(status);
        }
    }

    private static class FakeRentalRepository
            implements RentalRepository {

        private Rental rental;
        private int closedRentalId;

        @Override
        public Rental save(Rental rental) {
            this.rental = rental;
            return rental;
        }

        @Override
        public Optional<Rental> findById(int id) {
            return Optional.ofNullable(rental);
        }

        @Override
        public java.util.List<Rental> findActiveByOwnerId(
                int ownerId
        ) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<Rental> findByOwnerId(
                int ownerId
        ) {
            return java.util.List.of();
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
        public User save(User user) {
            this.user = new User(2, user.getUsername());
            return this.user;
        }

        @Override
        public Optional<User> findById(int id) {
            return Optional.ofNullable(user);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(user);
        }

        @Override
        public Optional<User> findOwner() {
            return Optional.ofNullable(user);
        }
    }
}