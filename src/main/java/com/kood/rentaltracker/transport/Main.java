package com.kood.rentaltracker.transport;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.RentalStatus;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.ItemRepositoryImpl;
import com.kood.rentaltracker.repository.RentalRepositoryImpl;
import com.kood.rentaltracker.repository.UserRepositoryImpl;
import com.kood.rentaltracker.service.ItemService;
import com.kood.rentaltracker.service.RentalService;
import com.kood.rentaltracker.service.UserService;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        try (Scanner scanner = new Scanner(System.in)) {
            UserRepositoryImpl userRepository =
                    new UserRepositoryImpl();

            ItemRepositoryImpl itemRepository =
                    new ItemRepositoryImpl();

            RentalRepositoryImpl rentalRepository =
                    new RentalRepositoryImpl();

            UserService userService =
                    new UserService(userRepository);

            ItemService itemService =
                    new ItemService(itemRepository);

            RentalService rentalService =
                    new RentalService(
                            rentalRepository,
                            itemRepository,
                            userRepository
                    );

            User owner = getOwner(
                    userService,
                    scanner
            );

            runMenu(
                    scanner,
                    owner,
                    itemService,
                    rentalService
            );
        }
    }

    private static User getOwner(
            UserService userService,
            Scanner scanner
    ) {
        if (userService.hasOwner()) {
            return userService.getOwner();
        }

        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        User owner = userService.createOwner(username);

        System.out.println(
                "Welcome, " + owner.getUsername() + "!"
        );

        return owner;
    }

    private static void runMenu(
            Scanner scanner,
            User owner,
            ItemService itemService,
            RentalService rentalService
    ) {
        boolean running = true;

        while (running) {
            printMenu();

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> listItem(
                        scanner,
                        owner,
                        itemService
                );

                case "2" -> viewInventory(
                        scanner,
                        owner,
                        itemService,
                        rentalService
                );

                case "3" -> recordRental(
                        scanner,
                        owner,
                        itemService,
                        rentalService
                );

                case "4" -> confirmReturn(
                        scanner,
                        owner,
                        rentalService
                );

                case "5" -> viewRentalHistory(
                        owner,
                        rentalService
                );

                case "6" -> {
                    running = false;
                    System.out.println("Goodbye.");
                }

                default -> System.out.println(
                        "Invalid option."
                );
            }
        }
    }

    private static void listItem(
            Scanner scanner,
            User owner,
            ItemService itemService
    ) {
        System.out.println();
        System.out.println("=== List an item ===");

        System.out.print("Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        System.out.print("Cost per day: ");

        double costPerDay;

        try {
            costPerDay = Double.parseDouble(
                    scanner.nextLine().trim()
            );
        } catch (NumberFormatException e) {
            System.out.println("Invalid cost.");
            return;
        }

        try {
            itemService.listItem(
                    owner.getId(),
                    name,
                    description,
                    costPerDay
            );

            System.out.println(
                    "Item listed successfully."
            );

        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void viewInventory(
            Scanner scanner,
            User owner,
            ItemService itemService,
            RentalService rentalService
    ) {
        List<Item> items =
                itemService.getInventory(owner.getId());

        if (items.isEmpty()) {
            System.out.println();
            System.out.println("=== My inventory ===");
            System.out.println("No items found.");
            return;
        }

        int page = 0;
        int pageSize = 5;

        while (true) {
            int start = page * pageSize;
            int end = Math.min(
                    start + pageSize,
                    items.size()
            );

            printInventoryPage(
                    items,
                    start,
                    end
            );

            printPaginationOptions(
                    page,
                    end,
                    items.size()
            );

            String choice = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (choice.equals("B")) {
                return;
            }

            if (choice.equals("N")
                    && end < items.size()) {
                page++;
                continue;
            }

            if (choice.equals("P")
                    && page > 0) {
                page--;
                continue;
            }

            try {
                int selected =
                        Integer.parseInt(choice);

                if (selected >= 1
                        && selected <= end - start) {

                    Item item =
                            items.get(
                                    start + selected - 1
                            );

                    showItemDetails(
                            scanner,
                            item,
                            owner,
                            itemService,
                            rentalService
                    );

                    items =
                            itemService.getInventory(
                                    owner.getId()
                            );

                    continue;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input.
            }

            System.out.println("Invalid option.");
        }
    }

    private static void printInventoryPage(
            List<Item> items,
            int start,
            int end
    ) {
        System.out.println();
        System.out.println("=== My inventory ===");

        for (int i = start; i < end; i++) {
            Item item = items.get(i);

            System.out.printf(
                    "%d) %s    %s%n",
                    i - start + 1,
                    item.getName(),
                    item.getStatus().name().toLowerCase()
            );
        }
    }

    private static void showItemDetails(
            Scanner scanner,
            Item item,
            User owner,
            ItemService itemService,
            RentalService rentalService
    ) {
        while (true) {
            System.out.println();
            System.out.println(
                    "=== " + item.getName() + " ==="
            );
            System.out.println(
                    "description    " + item.getDescription()
            );
            System.out.println(
                    "cost per day   " + item.getCostPerDay()
            );
            System.out.println(
                    "status         " +
                    item.getStatus().name().toLowerCase()
            );
            System.out.println(
                    "owner          " + owner.getUsername()
            );
            System.out.println(
                    "listing date   " + item.getListingDate()
            );

            System.out.println();

            if (item.getStatus() == ItemStatus.AVAILABLE) {
                System.out.println("1) Delist");
            } else if (item.getStatus() == ItemStatus.UNLISTED) {
                System.out.println("1) Relist");
            }

            System.out.println("2) Back to list");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("2")) {
                return;
            }

            if (choice.equals("1")) {
                try {
                    if (item.getStatus() == ItemStatus.AVAILABLE) {
                        itemService.changeStatus(
                                item,
                                ItemStatus.UNLISTED
                        );

                        System.out.println(
                                "Item delisted."
                        );
                        continue;
                    }

                    if (item.getStatus() == ItemStatus.UNLISTED) {
                        itemService.changeStatus(
                                item,
                                ItemStatus.AVAILABLE
                        );

                        System.out.println(
                                "Item relisted."
                        );
                        continue;
                    }

                    System.out.println(
                            "This item cannot be changed here."
                    );

                } catch (IllegalStateException e) {
                    System.out.println(e.getMessage());
                }

                continue;
            }

            System.out.println("Invalid option.");
        }
    }

    private static void recordRental(
            Scanner scanner,
            User owner,
            ItemService itemService,
            RentalService rentalService
    ) {
        List<Item> items =
                itemService.getAvailableItems(
                        owner.getId()
                );

        if (items.isEmpty()) {
            System.out.println();
            System.out.println("No available items.");
            return;
        }

        int page = 0;
        int pageSize = 5;

        while (true) {
            System.out.println();
            System.out.println("=== Record a rental ===");

            int start = page * pageSize;
            int end = Math.min(
                    start + pageSize,
                    items.size()
            );

            for (int i = start; i < end; i++) {
                Item item = items.get(i);

                System.out.printf(
                        "%d) %-20s %.2f/day%n",
                        i - start + 1,
                        item.getName(),
                        item.getCostPerDay()
                );
            }

            printPaginationOptions(
                    page,
                    end,
                    items.size()
            );

            String choice = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (choice.equals("B")) {
                return;
            }

            if (choice.equals("N")
                    && end < items.size()) {
                page++;
                continue;
            }

            if (choice.equals("P")
                    && page > 0) {
                page--;
                continue;
            }

            try {
                int selected =
                        Integer.parseInt(choice);

                if (selected >= 1
                        && selected <= end - start) {

                    Item item =
                            items.get(
                                    start + selected - 1
                            );

                    createRental(
                            scanner,
                            item,
                            rentalService
                    );

                    return;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input.
            }

            System.out.println("Invalid option.");
        }
    }

    private static void createRental(
            Scanner scanner,
            Item item,
            RentalService rentalService
    ) {
        System.out.print("Renter username: ");
        String renterUsername =
                scanner.nextLine().trim();

        System.out.print("Duration in days: ");

        int durationDays;

        try {
            durationDays = Integer.parseInt(
                    scanner.nextLine().trim()
            );
        } catch (NumberFormatException e) {
            System.out.println("Invalid duration.");
            return;
        }

        try {
            Rental rental =
                    rentalService.recordRental(
                            item,
                            renterUsername,
                            durationDays
                    );

            System.out.println();
            System.out.println(
                    "Rental recorded successfully."
            );
            System.out.println(
                    "Rental ID: " + rental.getId()
            );
            System.out.println(
                    "Ends: " + rental.getEndTime()
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void confirmReturn(
            Scanner scanner,
            User owner,
            RentalService rentalService
    ) {
        List<Rental> rentals =
                rentalService.getActiveRentals(
                        owner.getId()
                );

        if (rentals.isEmpty()) {
            System.out.println();
            System.out.println(
                    "No active rentals."
            );
            return;
        }

        int page = 0;
        int pageSize = 5;

        while (true) {
            int start = page * pageSize;
            int end = Math.min(
                    start + pageSize,
                    rentals.size()
            );

            printRentalPage(
                    rentals,
                    start,
                    end,
                    rentalService
            );

            printPaginationOptions(
                    page,
                    end,
                    rentals.size()
            );

            String choice = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (choice.equals("B")) {
                return;
            }

            if (choice.equals("N")
                    && end < rentals.size()) {
                page++;
                continue;
            }

            if (choice.equals("P")
                    && page > 0) {
                page--;
                continue;
            }

            try {
                int selected =
                        Integer.parseInt(choice);

                if (selected >= 1
                        && selected <= end - start) {

                    Rental rental =
                            rentals.get(
                                    start + selected - 1
                            );

                    showRentalDetails(
                            scanner,
                            rental,
                            rentalService
                    );

                    rentals =
                            rentalService.getActiveRentals(
                                    owner.getId()
                            );

                    continue;
                }
            } catch (NumberFormatException ignored) {
                // Invalid input.
            }

            System.out.println("Invalid option.");
        }
    }

    private static void printRentalPage(
            List<Rental> rentals,
            int start,
            int end,
            RentalService rentalService
    ) {
        System.out.println();
        System.out.println(
                "=== Confirm a return ==="
        );

        for (int i = start; i < end; i++) {
            Rental rental = rentals.get(i);

            Item item =
                    rentalService.getItem(rental);

            User renter =
                    rentalService.getRenter(rental);

            System.out.printf(
                    "%d) %-20s %s%n",
                    i - start + 1,
                    item.getName(),
                    renter.getUsername()
            );
        }
    }

    private static void showRentalDetails(
            Scanner scanner,
            Rental rental,
            RentalService rentalService
    ) {
        Item item =
                rentalService.getItem(rental);

        User renter =
                rentalService.getRenter(rental);

        while (true) {
            System.out.println();
            System.out.println(
                    "=== Rental " + rental.getId() + " ==="
            );
            System.out.println(
                    "item           " + item.getName()
            );
            System.out.println(
                    "renter         " + renter.getUsername()
            );
            System.out.println(
                    "start time     " + rental.getStartTime()
            );
            System.out.println(
                    "end time       " + rental.getEndTime()
            );
            System.out.println(
                    "status         " +
                    rental.getStatus()
                            .name()
                            .toLowerCase()
            );

            if (rental.getStatus() == RentalStatus.ACTIVE) {
                System.out.println(
                        "1) Confirm return"
                );
            }

            System.out.println("2) Back to list");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("2")) {
                return;
            }

            if (choice.equals("1")
                    && rental.getStatus()
                    == RentalStatus.ACTIVE) {

                try {
                    rentalService.confirmReturn(
                            item,
                            rental
                    );

                    System.out.println(
                            "Return confirmed successfully."
                    );

                    return;

                } catch (IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    private static void viewRentalHistory(
            User owner,
            RentalService rentalService
    ) {
        List<Rental> rentals =
                rentalService.getRentalHistory(
                        owner.getId()
                );

        System.out.println();
        System.out.println("=== Rental history ===");

        if (rentals.isEmpty()) {
            System.out.println("No rentals found.");
            return;
        }

        for (Rental rental : rentals) {
            Item item =
                    rentalService.getItem(rental);

            User renter =
                    rentalService.getRenter(rental);

            System.out.printf(
                    "Rental %d | Item %s | Renter %s | %s | Ends: %s%n",
                    rental.getId(),
                    item.getName(),
                    renter.getUsername(),
                    rental.getStatus()
                            .name()
                            .toLowerCase(),
                    rental.getEndTime()
            );
        }
    }

    private static void printPaginationOptions(
            int page,
            int end,
            int totalItems
    ) {
        System.out.println();
        System.out.println("B) Back to menu");

        if (page > 0) {
            System.out.println("P) Previous page");
        }

        if (end < totalItems) {
            System.out.println("N) Next page");
        }

        System.out.print("> ");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Rental tracker ===");
        System.out.println("1) List an item");
        System.out.println("2) View my inventory");
        System.out.println("3) Record a rental");
        System.out.println("4) Confirm a return");
        System.out.println("5) View rental history");
        System.out.println("6) Exit");
        System.out.print("> ");
    }
}