package com.kood.rentaltracker.transport;

import com.kood.rentaltracker.importer.DatabaseImporter;
import com.kood.rentaltracker.importer.ImportResult;
import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.ItemRepositoryImpl;
import com.kood.rentaltracker.repository.RentalRepositoryImpl;
import com.kood.rentaltracker.repository.UserRepositoryImpl;
import com.kood.rentaltracker.security.PasswordHasher;
import com.kood.rentaltracker.service.ItemService;
import com.kood.rentaltracker.service.RentalService;
import com.kood.rentaltracker.service.UserService;

import java.util.List;
import java.util.Scanner;

public class Main {

    private static final int PAGE_SIZE = 5;

    public static void main(String[] args) {
        if (args.length == 2 && args[0].equals("import")) {
            ImportResult result = new DatabaseImporter().importDirectory(
                    java.nio.file.Path.of(args[1])
            );
            System.out.printf(
                    "Import complete: inserted=%d, matched=%d, rejected=%d%n",
                    result.getInserted(),
                    result.getMatched(),
                    result.getRejected()
            );

            for (String rejection :
                    result.getRejectionDetails()) {

                System.out.println(
                        "Rejected: " + rejection
                );
            }

            return;
        }
        DatabaseInitializer.initialize();

        try (Scanner scanner = new Scanner(System.in)) {

            UserRepositoryImpl userRepository =
                    new UserRepositoryImpl();

            ItemRepositoryImpl itemRepository =
                    new ItemRepositoryImpl();

            RentalRepositoryImpl rentalRepository =
                    new RentalRepositoryImpl();

            UserService userService =
                    new UserService(
                            userRepository,
                            new PasswordHasher()
                    );

            ItemService itemService =
                    new ItemService(itemRepository);

            RentalService rentalService =
                    new RentalService(
                            rentalRepository,
                            itemRepository,
                            userRepository
                    );

            User currentUser =
                    authenticate(
                            scanner,
                            userService
                    );

            if (currentUser == null) {
                return;
            }

            runMenu(
                    scanner,
                    currentUser,
                    itemService,
                    rentalService
            );
        }
    }

    private static User authenticate(
            Scanner scanner,
            UserService userService
    ) {
        while (true) {
            System.out.println();
            System.out.println("=== Rental tracker ===");
            System.out.println("1) Register");
            System.out.println("2) Login");
            System.out.println("3) Exit");
            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                System.out.println("Goodbye.");
                return null;
            }

            String choice =
                    scanner.nextLine().trim();

            switch (choice) {

                case "1" ->
                        register(
                                scanner,
                                userService
                        );

                case "2" -> {
                    User user =
                            login(
                                    scanner,
                                    userService
                            );

                    if (user != null) {
                        System.out.println();
                        System.out.println(
                                "Welcome, "
                                        + user.getUsername()
                                        + "!"
                        );

                        return user;
                    }
                }

                case "3" -> {
                    System.out.println("Goodbye.");
                    return null;
                }

                default ->
                        System.out.println(
                                "Invalid option."
                        );
            }
        }
    }

    private static void register(
            Scanner scanner,
            UserService userService
    ) {
        System.out.println();
        System.out.println("=== Register ===");

        System.out.print("Username: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String username =
                scanner.nextLine();

        System.out.print("Password: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String password =
                scanner.nextLine();

        System.out.print("Confirm password: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String confirmation =
                scanner.nextLine();

        if (!password.equals(confirmation)) {
            System.out.println(
                    "Passwords do not match."
            );
            return;
        }

        try {
            User user =
                    userService.register(
                            username,
                            password
                    );

            System.out.println(
                    "Account created successfully for "
                            + user.getUsername()
                            + "."
            );

        } catch (IllegalArgumentException e) {
            System.out.println(
                    e.getMessage()
            );
        }
    }

    private static User login(
            Scanner scanner,
            UserService userService
    ) {
        System.out.println();
        System.out.println("=== Login ===");

        System.out.print("Username: ");

        if (!scanner.hasNextLine()) {
            return null;
        }

        String username =
                scanner.nextLine();

        System.out.print("Password: ");

        if (!scanner.hasNextLine()) {
            return null;
        }

        String password =
                scanner.nextLine();

        try {

            return userService.authenticate(
                    username,
                    password
            );

        } catch (IllegalArgumentException e) {
            System.out.println(
                    e.getMessage()
            );

            return null;
        }
    }

    private static void runMenu(
            Scanner scanner,
            User currentUser,
            ItemService itemService,
            RentalService rentalService
    ) {
        boolean running = true;

        while (running) {
            printMenu();

            if (!scanner.hasNextLine()) {
                System.out.println(
                        "Goodbye, "
                                + currentUser.getUsername()
                                + "."
                );
                return;
            }

            String choice =
                    scanner.nextLine().trim();

            switch (choice) {

                case "1" ->
                        listItem(
                                scanner,
                                currentUser,
                                itemService
                        );

                case "2" ->
                        viewInventory(
                                scanner,
                                currentUser,
                                itemService
                        );

                case "3" ->
                        browseListings(
                                scanner,
                                currentUser,
                                itemService,
                                rentalService
                        );

                case "4" ->
                        confirmReturn(
                                scanner,
                                currentUser,
                                rentalService,
                                itemService
                        );

                case "5" -> {
                    running = false;

                    System.out.println(
                            "Goodbye, "
                                    + currentUser.getUsername()
                                    + "."
                    );
                }

                default ->
                        System.out.println(
                                "Invalid option."
                        );
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Rental tracker ===");
        System.out.println("1) List an item");
        System.out.println("2) View my inventory");
        System.out.println("3) Browse listings");
        System.out.println("4) Confirm a return");
        System.out.println("5) Exit");
        System.out.print("> ");
    }

    private static void listItem(
            Scanner scanner,
            User currentUser,
            ItemService itemService
    ) {
        System.out.println();
        System.out.println("=== List an item ===");

        System.out.print("Name: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String name =
                scanner.nextLine();

        System.out.print("Description: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String description =
                scanner.nextLine();

        System.out.print("Cost per day: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String costInput =
                scanner.nextLine().trim();

        double costPerDay;

        try {
            costPerDay =
                    Double.parseDouble(costInput);

        } catch (NumberFormatException e) {
            System.out.println(
                    "Cost must be a valid number."
            );
            return;
        }

        try {
            itemService.listItem(
                    currentUser.getId(),
                    name,
                    description,
                    costPerDay
            );

            System.out.println(
                    "Item listed successfully."
            );

        } catch (IllegalArgumentException e) {
            System.out.println(
                    e.getMessage()
            );
        }
    }

    private static void viewInventory(
            Scanner scanner,
            User currentUser,
            ItemService itemService
    ) {
        List<Item> items =
                itemService.getInventory(
                        currentUser.getId()
                );

        if (items.isEmpty()) {
            System.out.println();
            System.out.println(
                    "Your inventory is empty."
            );
            return;
        }

        int page = 0;

        while (true) {
            int start =
                    page * PAGE_SIZE;

            int end =
                    Math.min(
                            start + PAGE_SIZE,
                            items.size()
                    );

            System.out.println();
            System.out.println(
                    "=== My inventory ==="
            );

            for (int i = start; i < end; i++) {
                Item item = items.get(i);

                System.out.printf(
                        "%d) %s - %.2f/day - %s%n",
                        i - start + 1,
                        item.getName(),
                        item.getCostPerDay(),
                        item.getStatus()
                );
            }

            System.out.println();
            printPaginationOptions(
                    page,
                    end,
                    items.size()
            );

            String choice =
                    readLine(
                            scanner,
                            ">"
                    );

            if (choice == null) {
                return;
            }

            choice = choice.trim();

            if (choice.equalsIgnoreCase("B")) {
                return;
            }

            if (choice.equalsIgnoreCase("N")) {
                if (end < items.size()) {
                    page++;
                } else {
                    System.out.println(
                            "Already on the last page."
                    );
                }

                continue;
            }

            if (choice.equalsIgnoreCase("P")) {
                if (page > 0) {
                    page--;
                } else {
                    System.out.println(
                            "Already on the first page."
                    );
                }

                continue;
            }

            Integer selected =
                    parseSelection(choice);

            if (selected == null
                    || selected < 1
                    || selected > end - start) {

                System.out.println(
                        "Invalid selection."
                );

                continue;
            }

            Item item =
                    items.get(
                            start + selected - 1
                    );

            showItemDetails(
                    item
            );
        }
    }

    private static void browseListings(
            Scanner scanner,
            User currentUser,
            ItemService itemService,
            RentalService rentalService
    ) {
        List<Item> items =
                itemService.getAvailableItems(
                        currentUser.getId()
                );

        if (items.isEmpty()) {
            System.out.println();
            System.out.println(
                    "No available listings found."
            );
            return;
        }

        int page = 0;

        while (true) {
            int start =
                    page * PAGE_SIZE;

            int end =
                    Math.min(
                            start + PAGE_SIZE,
                            items.size()
                    );

            System.out.println();
            System.out.println(
                    "=== Browse listings ==="
            );

            for (int i = start; i < end; i++) {
                Item item = items.get(i);

                System.out.printf(
                        "%d) %s - %.2f/day%n",
                        i - start + 1,
                        item.getName(),
                        item.getCostPerDay()
                );
            }

            System.out.println();
            printPaginationOptions(
                    page,
                    end,
                    items.size()
            );

            String choice =
                    readLine(
                            scanner,
                            ">"
                    );

            if (choice == null) {
                return;
            }

            choice = choice.trim();

            if (choice.equalsIgnoreCase("B")) {
                return;
            }

            if (choice.equalsIgnoreCase("N")) {
                if (end < items.size()) {
                    page++;
                } else {
                    System.out.println(
                            "Already on the last page."
                    );
                }

                continue;
            }

            if (choice.equalsIgnoreCase("P")) {
                if (page > 0) {
                    page--;
                } else {
                    System.out.println(
                            "Already on the first page."
                    );
                }

                continue;
            }

            Integer selected =
                    parseSelection(choice);

            if (selected == null
                    || selected < 1
                    || selected > end - start) {

                System.out.println(
                        "Invalid selection."
                );

                continue;
            }

            Item item =
                    items.get(
                            start + selected - 1
                    );

            showListingDetails(
                    scanner,
                    item,
                    currentUser,
                    rentalService
            );

            items =
                    itemService.getAvailableItems(
                            currentUser.getId()
                    );

            if (items.isEmpty()) {
                System.out.println();
                System.out.println(
                        "No more available listings."
                );
                return;
            }

            if (page * PAGE_SIZE >= items.size()) {
                page =
                        Math.max(
                                0,
                                (items.size() - 1)
                                        / PAGE_SIZE
                        );
            }
        }
    }

    private static void showListingDetails(
            Scanner scanner,
            Item item,
            User currentUser,
            RentalService rentalService
    ) {
        System.out.println();
        System.out.println("=== Item details ===");
        System.out.println(
                "Name: " + item.getName()
        );
        System.out.println(
                "Description: "
                        + item.getDescription()
        );
        System.out.printf(
                "Cost per day: %.2f%n",
                item.getCostPerDay()
        );
        System.out.println(
                "Status: " + item.getStatus()
        );

        System.out.println();
        System.out.println(
                "Enter number of rental days."
        );
        System.out.println(
                "Enter 0 to go back."
        );
        System.out.print("> ");

        String input =
                readLine(
                        scanner,
                        null
                );

        if (input == null) {
            return;
        }

        int durationDays;

        try {
            durationDays =
                    Integer.parseInt(
                            input.trim()
                    );

        } catch (NumberFormatException e) {
            System.out.println(
                    "Rental duration must be a whole number."
            );
            return;
        }

        if (durationDays == 0) {
            return;
        }

        try {
            rentalService.recordRental(
                    item,
                    currentUser.getId(),
                    durationDays
            );

            System.out.println(
                    "Rental created successfully."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            System.out.println(
                    e.getMessage()
            );
        }
    }

    private static void confirmReturn(
            Scanner scanner,
            User currentUser,
            RentalService rentalService,
            ItemService itemService
    ) {
        List<Rental> rentals =
                rentalService.getActiveRentals(
                        currentUser.getId()
                );

        if (rentals.isEmpty()) {
            System.out.println();
            System.out.println(
                    "You have no active rentals to return."
            );
            return;
        }

        System.out.println();
        System.out.println(
                "=== Confirm a return ==="
        );

        for (int i = 0; i < rentals.size(); i++) {
            Rental rental =
                    rentals.get(i);

            Item item =
                    rentalService.getItem(
                            rental
                    );

            User renter =
                    rentalService.getRenter(
                            rental
                    );

            System.out.printf(
                    "%d) %s - renter: %s - ends: %s%n",
                    i + 1,
                    item.getName(),
                    renter.getUsername(),
                    rental.getEndTime()
            );
        }

        System.out.println();
        System.out.println(
                "Select rental number or B to go back."
        );
        System.out.print("> ");

        String input =
                readLine(
                        scanner,
                        null
                );

        if (input == null
                || input.equalsIgnoreCase("B")) {
            return;
        }

        int selected;

        try {
            selected =
                    Integer.parseInt(
                            input.trim()
                    );

        } catch (NumberFormatException e) {
            System.out.println(
                    "Invalid selection."
            );
            return;
        }

        if (selected < 1
                || selected > rentals.size()) {

            System.out.println(
                    "Invalid selection."
            );
            return;
        }

        Rental rental =
                rentals.get(
                        selected - 1
                );

        Item item =
                rentalService.getItem(
                        rental
                );

        try {
            rentalService.confirmReturn(
                    item,
                    rental,
                    currentUser.getId()
            );

            System.out.println(
                    "Return confirmed successfully."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            System.out.println(
                    e.getMessage()
            );
        }
    }

    private static void showItemDetails(
            Item item
    ) {
        System.out.println();
        System.out.println("=== Item details ===");
        System.out.println(
                "Name: " + item.getName()
        );
        System.out.println(
                "Description: "
                        + item.getDescription()
        );
        System.out.printf(
                "Cost per day: %.2f%n",
                item.getCostPerDay()
        );
        System.out.println(
                "Status: " + item.getStatus()
        );
        System.out.println();
    }

    private static void printPaginationOptions(
            int page,
            int end,
            int total
    ) {
        System.out.println(
                "N) Next page"
        );
        System.out.println(
                "P) Previous page"
        );
        System.out.println(
                "B) Back"
        );
        System.out.println(
                "Select an item number."
        );
    }

    private static String readPaginationChoice(
            Scanner scanner,
            int page,
            int end,
            int total
    ) {
        System.out.print("> ");

        if (!scanner.hasNextLine()) {
            return "B";
        }

        return scanner.nextLine();
    }

    private static Integer parseSelection(
            String input
    ) {
        try {
            return Integer.parseInt(
                    input.trim()
            );

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readLine(
            Scanner scanner,
            String prompt
    ) {
        if (prompt != null) {
            System.out.print(prompt + " ");
        }

        if (!scanner.hasNextLine()) {
            return null;
        }

        return scanner.nextLine();
    }
}
