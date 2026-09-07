package com.kood.rentaltracker.transport;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.ItemRepositoryImpl;
import com.kood.rentaltracker.repository.RentalRepositoryImpl;
import com.kood.rentaltracker.repository.UserRepositoryImpl;
import com.kood.rentaltracker.service.ItemService;
import com.kood.rentaltracker.service.RentalService;
import com.kood.rentaltracker.service.UserService;

import java.util.List;
import java.util.Optional;
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
                    itemRepository,
                    rentalRepository,
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
            ItemRepositoryImpl itemRepository,
            RentalRepositoryImpl rentalRepository,
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
                        itemRepository
                );

                case "3" -> recordRental(
                        scanner,
                        owner,
                        itemRepository,
                        rentalService
                );

                case "4" -> confirmReturn(
                        scanner,
                        owner,
                        itemRepository,
                        rentalRepository,
                        rentalService
                );

                case "5" -> viewRentalHistory(
                        owner,
                        rentalRepository
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

        if (name.isBlank() || description.isBlank()) {
            System.out.println(
                    "Name and description cannot be empty."
            );
            return;
        }

        if (costPerDay < 0) {
            System.out.println(
                    "Cost cannot be negative."
            );
            return;
        }

        itemService.listItem(
                owner.getId(),
                name,
                description,
                costPerDay
        );

        System.out.println("Item listed successfully.");
    }

    private static void viewInventory(
            Scanner scanner,
            User owner,
            ItemService itemService,
            ItemRepositoryImpl itemRepository
    ) {
        List<Item> items =
                itemRepository.findByOwnerId(owner.getId());

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

            printInventoryPage(items, start, end);

            System.out.println();
            System.out.println("B) Back to menu");

            if (page > 0) {
                System.out.println("P) Previous page");
            }

            if (end < items.size()) {
                System.out.println("N) Next page");
            }

            System.out.print("> ");

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
                int selected = Integer.parseInt(choice);

                if (selected >= 1
                        && selected <= end - start) {

                    Item item =
                            items.get(start + selected - 1);

                    showItemDetails(
                            scanner,
                            item,
                            itemService
                    );

                    items =
                            itemRepository.findByOwnerId(
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
            ItemService itemService
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

            if (item.getStatus() == ItemStatus.AVAILABLE) {
                System.out.println("1) Delist");
            } else if (
                    item.getStatus() == ItemStatus.UNLISTED
            ) {
                System.out.println("1) Relist");
            }

            System.out.println("2) Back to list");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("2")) {
                return;
            }

            if (choice.equals("1")) {
                if (item.getStatus() == ItemStatus.AVAILABLE) {
                    itemService.changeStatus(
                            item,
                            ItemStatus.UNLISTED
                    );

                    System.out.println("Item delisted.");
                    continue;
                }

                if (item.getStatus() == ItemStatus.UNLISTED) {
                    itemService.changeStatus(
                            item,
                            ItemStatus.AVAILABLE
                    );

                    System.out.println("Item relisted.");
                    continue;
                }

                System.out.println(
                        "A rented item cannot be changed here."
                );

                continue;
            }

            System.out.println("Invalid option.");
        }
    }

    private static void recordRental(
            Scanner scanner,
            User owner,
            ItemRepositoryImpl itemRepository,
            RentalService rentalService
    ) {
        List<Item> items =
                itemRepository.findByOwnerIdAndStatus(
                        owner.getId(),
                        ItemStatus.AVAILABLE
                );

        if (items.isEmpty()) {
            System.out.println();
            System.out.println("No available items.");
            return;
        }

        System.out.println();
        System.out.println("=== Record a rental ===");

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);

            System.out.printf(
                    "%d) %s    %.2f/day%n",
                    i + 1,
                    item.getName(),
                    item.getCostPerDay()
            );
        }

        System.out.print("Choose item: ");

        int choice;

        try {
            choice = Integer.parseInt(
                    scanner.nextLine().trim()
            );
        } catch (NumberFormatException e) {
            System.out.println("Invalid item number.");
            return;
        }

        if (choice < 1 || choice > items.size()) {
            System.out.println("Invalid item number.");
            return;
        }

        Item item = items.get(choice - 1);

        System.out.print("Renter username: ");
        String renterUsername =
                scanner.nextLine().trim();

        if (renterUsername.isBlank()) {
            System.out.println(
                    "Renter username cannot be empty."
            );
            return;
        }

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

        if (durationDays <= 0) {
            System.out.println(
                    "Duration must be greater than zero."
            );
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
                    "Item: " + item.getName()
            );
            System.out.println(
                    "Renter: " + renterUsername
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
            ItemRepositoryImpl itemRepository,
            RentalRepositoryImpl rentalRepository,
            RentalService rentalService
    ) {
        System.out.println();
        System.out.println("=== Confirm a return ===");

        System.out.print("Rental ID: ");

        int rentalId;

        try {
            rentalId = Integer.parseInt(
                    scanner.nextLine().trim()
            );
        } catch (NumberFormatException e) {
            System.out.println("Invalid rental ID.");
            return;
        }

        Optional<Rental> rental =
                rentalRepository.findById(rentalId);

        if (rental.isEmpty()) {
            System.out.println("Rental not found.");
            return;
        }

        Optional<Item> item =
                itemRepository.findById(
                        rental.get().getItemId()
                );

        if (item.isEmpty()) {
            System.out.println("Item not found.");
            return;
        }

        if (item.get().getOwnerId() != owner.getId()) {
            System.out.println(
                    "You do not own this item."
            );
            return;
        }

        try {
            rentalService.confirmReturn(
                    item.get(),
                    rental.get()
            );

            System.out.println(
                    "Return confirmed successfully."
            );

        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void viewRentalHistory(
            User owner,
            RentalRepositoryImpl rentalRepository
    ) {
        List<Rental> rentals =
                rentalRepository.findByOwnerId(
                        owner.getId()
                );

        System.out.println();
        System.out.println("=== Rental history ===");

        if (rentals.isEmpty()) {
            System.out.println("No rentals found.");
            return;
        }

        for (Rental rental : rentals) {
            System.out.printf(
                    "Rental %d | Item %d | Renter %d | %s | Ends: %s%n",
                    rental.getId(),
                    rental.getItemId(),
                    rental.getRenterId(),
                    rental.getStatus().name().toLowerCase(),
                    rental.getEndTime()
            );
        }
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