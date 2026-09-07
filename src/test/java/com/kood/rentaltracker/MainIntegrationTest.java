package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.repository.ItemRepositoryImpl;
import com.kood.rentaltracker.repository.UserRepositoryImpl;
import com.kood.rentaltracker.support.DatabaseTestSupport;
import com.kood.rentaltracker.transport.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainIntegrationTest {

    private Path database;

    @AfterEach
    void tearDown() throws IOException {
        if (database != null) {
            DatabaseTestSupport.cleanup(database);
        }
    }

    @Test
    void firstLaunchPromptsForUsernameAndExits() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        String output = runApp("""
                christine
                6
                """);

        assertTrue(output.contains("Enter your username:"));
        assertTrue(output.contains("Welcome, christine!"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void existingAccountLoadsWithoutUsernamePrompt() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        createUser("christine");

        String output = runApp("""
                6
                """);

        assertFalse(output.contains("Enter your username:"));
        assertTrue(output.contains("=== Rental tracker ==="));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void listingItemReturnsToMainMenu() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        int ownerId = createUser("christine");

        String output = runApp("""
                1
                Drill
                Cordless drill
                8
                6
                """);

        assertTrue(output.contains("Item listed successfully."));
        assertTrue(output.contains("=== Rental tracker ==="));
        assertTrue(output.contains("Goodbye."));

        assertItemStatus(ownerId, 1, ItemStatus.AVAILABLE);
    }

    @Test
    void inventorySupportsPaginationAndItemDetails() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        int ownerId = createUser("christine");
        createItems(ownerId, 6);

        String output = runApp("""
                2
                N
                1
                B
                B
                6
                """);

        assertTrue(output.contains("Item 6"));
        assertTrue(output.contains("description    Test description"));
        assertTrue(output.contains("cost per day   5.0"));
        assertTrue(output.contains("status         available"));
    }

    @Test
    void itemCanBeDelistedAndRelisted() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        int ownerId = createUser("christine");

        createItem(
                ownerId,
                "Drill",
                "Cordless drill",
                8.0
        );

        String output = runApp("""
                2
                1
                1
                1
                2
                B
                6
                """);

        assertTrue(output.contains("Item delisted."));
        assertTrue(output.contains("Item relisted."));

        assertItemStatus(
                ownerId,
                1,
                ItemStatus.AVAILABLE
        );
    }

    @Test
    void rentalCanBeRecordedAndReturned() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        int ownerId = createUser("christine");

        createItem(
                ownerId,
                "Drill",
                "Cordless drill",
                8.0
        );

        String output = runApp("""
                3
                1
                james
                3
                4
                1
                1
                B
                6
                """);

        assertTrue(output.contains("Rental recorded successfully."));
        assertTrue(output.contains("Return confirmed successfully."));

        assertItemStatus(
                ownerId,
                1,
                ItemStatus.AVAILABLE
        );
    }

    @Test
    void rentedItemCanBeDelistedBeforeReturn() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        int ownerId = createUser("christine");

        createItem(
                ownerId,
                "Drill",
                "Cordless drill",
                8.0
        );

        String output = runApp("""
                3
                1
                james
                3
                2
                1
                1
                2
                B
                4
                1
                1
                B
                6
                """);

        assertTrue(output.contains("Rental recorded successfully."));
        assertTrue(output.contains("Item delisted."));
        assertTrue(output.contains("Return confirmed successfully."));

        assertItemStatus(
                ownerId,
                1,
                ItemStatus.UNLISTED
        );
    }

    @Test
    void rentalHistoryCanBeViewed() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        int ownerId = createUser("christine");

        createItem(
                ownerId,
                "Drill",
                "Cordless drill",
                8.0
        );

        String output = runApp("""
                3
                1
                james
                3
                4
                1
                1
                B
                5
                6
                """);

        assertTrue(output.contains("Rental recorded successfully."));
        assertTrue(output.contains("=== Rental history ==="));
        assertTrue(output.contains("Item Drill"));
        assertTrue(output.contains("Renter james"));
        assertTrue(output.contains("closed"));
    }

    @Test
    void invalidInputDoesNotCrashApplication() throws Exception {
        database = DatabaseTestSupport.createDatabase();

        createUser("christine");

        String output = runApp("""
                invalid
                9
                6
                """);

        assertTrue(output.contains("Invalid option."));
        assertTrue(output.contains("Goodbye."));
    }

    private int createUser(String username) {
        UserRepositoryImpl repository =
                new UserRepositoryImpl();

        User user = repository.save(
                new User(0, username)
        );

        return user.getId();
    }

    private Item createItem(
            int ownerId,
            String name,
            String description,
            double costPerDay
    ) {
        ItemRepositoryImpl repository =
                new ItemRepositoryImpl();

        return repository.save(
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

    private void createItems(
            int ownerId,
            int count
    ) {
        for (int i = 1; i <= count; i++) {
            createItem(
                    ownerId,
                    "Item " + i,
                    "Test description",
                    5.0
            );
        }
    }

    private void assertItemStatus(
            int ownerId,
            int itemId,
            ItemStatus expectedStatus
    ) {
        ItemRepositoryImpl repository =
                new ItemRepositoryImpl();

        Item item = repository.findById(itemId)
                .orElseThrow();

        assertEquals(ownerId, item.getOwnerId());
        assertEquals(expectedStatus, item.getStatus());
    }

    private String runApp(String input)
            throws Exception {

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        ByteArrayInputStream testInput =
                new ByteArrayInputStream(
                        input.getBytes(StandardCharsets.UTF_8)
                );

        ByteArrayOutputStream testOutput =
                new ByteArrayOutputStream();

        try {
            System.setIn(testInput);
            System.setOut(
                    new PrintStream(
                            testOutput,
                            true,
                            StandardCharsets.UTF_8
                    )
            );

            Main.main(new String[0]);

            return testOutput.toString(
                    StandardCharsets.UTF_8
            );
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }
}
