package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.*;
import com.kood.rentaltracker.infrastructure.*;
import com.kood.rentaltracker.repository.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AtomicRentalOperationsTest {

    private Path database;
    private UserRepositoryImpl users;
    private ItemRepositoryImpl items;
    private RentalRepositoryImpl rentals;
    private AtomicRentalOperations operations;

    private static final String PASSWORD_HASH =
            "$argon2id$v=19$m=1024,t=3,p=2$test-salt$test-hash";

    @BeforeEach
    void setUp() throws IOException {
        database = Files.createTempFile("atomic-rental-test-", ".db");
        Files.deleteIfExists(database);

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + database
        );

        DatabaseInitializer.initialize();

        users = new UserRepositoryImpl();
        items = new ItemRepositoryImpl();
        rentals = new RentalRepositoryImpl();

        operations = new AtomicRentalOperations(
                new RentalTransactionManager(),
                rentals,
                items
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("rental.tracker.db.url");
        Files.deleteIfExists(database);
    }

    private User createUser(String name) {
        return users.save(new User(0, name), PASSWORD_HASH);
    }

    private Item createItem(int ownerId) {
        return items.save(new Item(
                0,
                ownerId,
                "Camera",
                "Test camera",
                5.0,
                ItemStatus.AVAILABLE
        ));
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

    @Test
    void creatingRentalUpdatesBothRecords() {
        User owner = createUser("owner");
        User renter = createUser("renter");
        Item item = createItem(owner.getId());

        Rental saved = operations.createRental(
                createRental(item.getId(), renter.getId())
        );

        assertTrue(saved.getId() > 0);
        assertEquals(
                ItemStatus.RENTED,
                items.findById(item.getId()).orElseThrow().getStatus()
        );
        assertEquals(
                RentalStatus.ACTIVE,
                rentals.findById(saved.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void failedRentalInsertRollsBackItemStatus() {
        User owner = createUser("owner");
        Item item = createItem(owner.getId());

        assertThrows(
                RuntimeException.class,
                () -> operations.createRental(
                        createRental(item.getId(), 999999)
                )
        );

        assertEquals(
                ItemStatus.AVAILABLE,
                items.findById(item.getId()).orElseThrow().getStatus()
        );
        assertTrue(rentals.findActiveByOwnerId(owner.getId()).isEmpty());
    }

    @Test
    void successfulReturnClosesRentalAndRestoresAvailability() {
        User owner = createUser("owner");
        User renter = createUser("renter");
        Item item = createItem(owner.getId());

        Rental rental = operations.createRental(
                createRental(item.getId(), renter.getId())
        );

        operations.confirmReturn(
                rental.getId(),
                item.getId(),
                LocalDateTime.now(),
                ItemStatus.AVAILABLE
        );

        assertEquals(
                RentalStatus.CLOSED,
                rentals.findById(rental.getId()).orElseThrow().getStatus()
        );
        assertEquals(
                ItemStatus.AVAILABLE,
                items.findById(item.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void failedReturnRollsBackRentalClosure() {
        User owner = createUser("owner");
        User renter = createUser("renter");
        Item item = createItem(owner.getId());

        Rental rental = operations.createRental(
                createRental(item.getId(), renter.getId())
        );

        assertThrows(
                RuntimeException.class,
                () -> operations.confirmReturn(
                        rental.getId(),
                        item.getId(),
                        LocalDateTime.now(),
                        ItemStatus.UNLISTED
                )
        );

        assertEquals(
                RentalStatus.ACTIVE,
                rentals.findById(rental.getId()).orElseThrow().getStatus()
        );
        assertEquals(
                ItemStatus.RENTED,
                items.findById(item.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void returningUnlistedItemPreservesUnlistedStatus() {
        User owner = createUser("owner");
        User renter = createUser("renter");
        Item item = createItem(owner.getId());

        Rental rental = operations.createRental(
                createRental(item.getId(), renter.getId())
        );

        items.updateStatus(item.getId(), ItemStatus.UNLISTED);

        operations.confirmReturn(
                rental.getId(),
                item.getId(),
                LocalDateTime.now(),
                ItemStatus.UNLISTED
        );

        assertEquals(
                RentalStatus.CLOSED,
                rentals.findById(rental.getId()).orElseThrow().getStatus()
        );
        assertEquals(
                ItemStatus.UNLISTED,
                items.findById(item.getId()).orElseThrow().getStatus()
        );
    }


    @Test
    void cannotRentAlreadyRentedItem() {
        User owner = createUser("owner");
        User firstRenter = createUser("firstRenter");
        User secondRenter = createUser("secondRenter");
        Item item = createItem(owner.getId());

        Rental firstRental = operations.createRental(
                createRental(item.getId(), firstRenter.getId())
        );

        assertThrows(
                IllegalStateException.class,
                () -> operations.createRental(
                        createRental(item.getId(), secondRenter.getId())
                )
        );

        assertEquals(
                ItemStatus.RENTED,
                items.findById(item.getId()).orElseThrow().getStatus()
        );

        assertEquals(
                RentalStatus.ACTIVE,
                rentals.findById(firstRental.getId()).orElseThrow().getStatus()
        );

        assertEquals(
                1,
                rentals.findActiveByOwnerId(owner.getId()).size()
        );
    }

}
