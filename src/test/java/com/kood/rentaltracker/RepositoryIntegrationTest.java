package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.RentalStatus;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.ItemRepositoryImpl;
import com.kood.rentaltracker.repository.RentalRepositoryImpl;
import com.kood.rentaltracker.repository.UserRepositoryImpl;
import com.kood.rentaltracker.repository.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryIntegrationTest {

    private Path database;
    private UserRepositoryImpl userRepository;
    private ItemRepositoryImpl itemRepository;
    private RentalRepositoryImpl rentalRepository;

    @BeforeEach
    void setUp() throws IOException {
        database = Files.createTempFile(
                "rental-tracker-test-",
                ".db"
        );

        Files.deleteIfExists(database);

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + database
        );

        DatabaseInitializer.initialize();

        userRepository = new UserRepositoryImpl();
        itemRepository = new ItemRepositoryImpl();
        rentalRepository = new RentalRepositoryImpl();
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("rental.tracker.db.url");
        Files.deleteIfExists(database);
    }

    @Test
    void userInsertAndQueriesWork() {
        User saved = createUser("christine");

        User byId = userRepository.findById(saved.getId()).orElseThrow();
        User byUsername = userRepository
                .findByUsername("christine")
                .orElseThrow();
        User owner = userRepository.findOwner().orElseThrow();

        assertEquals(saved.getId(), byId.getId());
        assertEquals(saved.getId(), byUsername.getId());
        assertEquals(saved.getId(), owner.getId());
        assertEquals("christine", owner.getUsername());
    }

    @Test
    void missingUserReturnsEmpty() {
        assertTrue(userRepository.findById(9999).isEmpty());
        assertTrue(userRepository.findByUsername("missing").isEmpty());
        assertTrue(userRepository.findOwner().isEmpty());
    }

    @Test
    void itemInsertAndQueriesWork() {
        User owner = createUser("christine");

        Item saved = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        Item byId = itemRepository.findById(saved.getId()).orElseThrow();

        List<Item> ownerItems =
                itemRepository.findByOwnerId(owner.getId());

        List<Item> availableItems =
                itemRepository.findByOwnerIdAndStatus(
                        owner.getId(),
                        ItemStatus.AVAILABLE
                );

        assertEquals(saved.getId(), byId.getId());
        assertEquals(1, ownerItems.size());
        assertEquals("Ladder", ownerItems.get(0).getName());
        assertEquals(1, availableItems.size());
    }

    @Test
    void missingItemReturnsEmpty() {
        assertTrue(itemRepository.findById(9999).isEmpty());
    }

    @Test
    void itemStatusCanBeUpdated() {
        User owner = createUser("christine");

        Item item = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        itemRepository.updateStatus(
                item.getId(),
                ItemStatus.RENTED
        );

        Item updated = itemRepository.findById(
                item.getId()
        ).orElseThrow();

        assertEquals(ItemStatus.RENTED, updated.getStatus());
    }

    @Test
    void rentalInsertAndQueriesWork() {
        User owner = createUser("christine");
        User renter = createUser("james");

        Item item = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        Rental rental = rentalRepository.save(
                createRental(item.getId(), renter.getId())
        );

        Rental byId = rentalRepository.findById(
                rental.getId()
        ).orElseThrow();

        List<Rental> active =
                rentalRepository.findActiveByOwnerId(owner.getId());

        List<Rental> history =
                rentalRepository.findByOwnerId(owner.getId());

        assertEquals(rental.getId(), byId.getId());
        assertEquals(RentalStatus.ACTIVE, byId.getStatus());
        assertEquals(1, active.size());
        assertEquals(1, history.size());
    }

    @Test
    void missingRentalReturnsEmpty() {
        assertTrue(rentalRepository.findById(9999).isEmpty());
    }

    @Test
    void rentalCanBeClosed() {
        User owner = createUser("christine");
        User renter = createUser("james");

        Item item = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        Rental rental = rentalRepository.save(
                createRental(item.getId(), renter.getId())
        );

        rentalRepository.closeRental(
                rental.getId(),
                LocalDateTime.now().toString()
        );

        Rental closed = rentalRepository.findById(
                rental.getId()
        ).orElseThrow();

        assertEquals(RentalStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getReturnedAt());

        assertEquals(
                0,
                rentalRepository
                        .findActiveByOwnerId(owner.getId())
                        .size()
        );

        assertEquals(
                1,
                rentalRepository
                        .findByOwnerId(owner.getId())
                        .size()
        );
    }

    @Test
    void rentalWithReturnedAtCanBeSavedAndLoaded() {
        User owner = createUser("christine");
        User renter = createUser("james");

        Item item = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime returnedAt = start.plusDays(1);

        Rental rental = new Rental(
                0,
                item.getId(),
                renter.getId(),
                start,
                start.plusDays(3),
                returnedAt,
                RentalStatus.CLOSED
        );

        Rental saved = rentalRepository.save(rental);

        Rental loaded = rentalRepository.findById(
                saved.getId()
        ).orElseThrow();

        assertEquals(returnedAt, loaded.getReturnedAt());
        assertEquals(RentalStatus.CLOSED, loaded.getStatus());
    }

    @Test
    void closingMissingRentalThrowsException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rentalRepository.closeRental(
                        9999,
                        LocalDateTime.now().toString()
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Active rental not found"
                )
        );
    }

    @Test
    void updatingMissingItemThrowsException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> itemRepository.updateStatus(
                        9999,
                        ItemStatus.RENTED
                )
        );

        assertTrue(
                exception.getMessage().contains("Item not found")
        );
    }

    @Test
    void duplicateUsernameThrowsConstraintViolation() {
        createUser("christine");

        assertThrows(
                ConstraintViolationException.class,
                () -> createUser("christine")
        );
    }

    @Test
    void rentalWithMissingItemThrowsConstraintViolation() {
        User renter = createUser("james");
        Rental rental = createRental(9999, renter.getId());

        assertThrows(
                ConstraintViolationException.class,
                () -> rentalRepository.save(rental)
        );
    }

    @Test
    void itemMissingRequiredFieldThrowsConstraintViolation() {
        User owner = createUser("christine");

        Item invalidItem = new Item(
                0,
                owner.getId(),
                null,
                "Missing item name",
                5.0,
                ItemStatus.AVAILABLE
        );

        assertThrows(
                ConstraintViolationException.class,
                () -> itemRepository.save(invalidItem)
        );
    }

    @Test
    void invalidStatusThrowsConstraintViolation() {
        User owner = createUser("christine");

        Item item = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        assertThrows(
                ConstraintViolationException.class,
                () -> itemRepository.updateStatusValue(
                        item.getId(),
                        "invalid-status"
                )
        );
    }

    @Test
    void missingItemTableProducesDatabaseError() {
        dropTable("listed_items");

        assertDatabaseError(
                () -> itemRepository.findById(1)
        );
    }

    @Test
    void missingRentalTableProducesDatabaseError() {
        dropTable("rentals");

        assertDatabaseError(
                () -> rentalRepository.findById(1)
        );
    }

    @Test
    void missingUserTableProducesDatabaseErrorWhenFindingById() {
        dropTable("users");

        assertDatabaseError(
                () -> userRepository.findById(1)
        );
    }

    @Test
    void missingUserTableProducesDatabaseErrorWhenFindingByUsername() {
        dropTable("users");

        assertDatabaseError(
                () -> userRepository.findByUsername("christine")
        );
    }

    @Test
    void missingUserTableProducesDatabaseErrorWhenFindingOwner() {
        dropTable("users");

        assertDatabaseError(
                () -> userRepository.findOwner()
        );
    }

    @Test
    void missingItemTableProducesDatabaseErrorWhenFindingByOwner() {
        dropTable("listed_items");

        assertDatabaseError(
                () -> itemRepository.findByOwnerId(1)
        );
    }

    @Test
    void missingItemTableProducesDatabaseErrorWhenFindingByStatus() {
        dropTable("listed_items");

        assertDatabaseError(
                () -> itemRepository.findByOwnerIdAndStatus(
                        1,
                        ItemStatus.AVAILABLE
                )
        );
    }

    @Test
    void missingItemTableProducesDatabaseErrorWhenUpdatingStatus() {
        dropTable("listed_items");

        assertDatabaseError(
                () -> itemRepository.updateStatus(
                        1,
                        ItemStatus.RENTED
                )
        );
    }

    @Test
    void missingRentalTableProducesDatabaseErrorWhenClosingRental() {
        dropTable("rentals");

        assertDatabaseError(
                () -> rentalRepository.closeRental(
                        1,
                        LocalDateTime.now().toString()
                )
        );
    }

    @Test
    void missingRentalTableProducesDatabaseErrorWhenFindingActiveRentals() {
        dropTable("rentals");

        assertDatabaseError(
                () -> rentalRepository.findActiveByOwnerId(1)
        );
    }

    @Test
    void missingRentalTableProducesDatabaseErrorWhenFindingHistory() {
        dropTable("rentals");

        assertDatabaseError(
                () -> rentalRepository.findByOwnerId(1)
        );
    }

    @Test
    void blankCreatedAtMapsToNullListingDate() {
        User owner = createUser("christine");

        Item item = itemRepository.save(
                createItem(owner.getId(), "Ladder")
        );

        updateCreatedAt(item.getId(), "");

        Item loaded = itemRepository.findById(
                item.getId()
        ).orElseThrow();

        assertNull(loaded.getListingDate());
    }

    private User createUser(String username) {
        return userRepository.save(
                new User(0, username)
        );
    }

    private Item createItem(int ownerId, String name) {
        return new Item(
                0,
                ownerId,
                name,
                "Test description",
                5.0,
                ItemStatus.AVAILABLE
        );
    }

    private Rental createRental(int itemId, int renterId) {
        LocalDateTime start = LocalDateTime.now();

        return new Rental(
                0,
                itemId,
                renterId,
                start,
                start.plusDays(3),
                null,
                RentalStatus.ACTIVE
        );
    }

    private void updateCreatedAt(
            int itemId,
            String createdAt
    ) {
        try (var connection = Database.connect();
             var statement = connection.createStatement()) {

            statement.executeUpdate(
                    "UPDATE listed_items SET created_at = '"
                            + createdAt
                            + "' WHERE item_id = "
                            + itemId
            );

        } catch (Exception e) {
            fail(e);
        }
    }

    private void dropTable(String tableName) {
        try (var connection = Database.connect();
             var statement = connection.createStatement()) {

            statement.executeUpdate(
                    "DROP TABLE " + tableName
            );

        } catch (Exception e) {
            fail(e);
        }
    }

    private void assertDatabaseError(
            org.junit.jupiter.api.function.Executable action
    ) {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                action
        );

        assertEquals(
                "Database error",
                exception.getMessage()
        );
    }
}