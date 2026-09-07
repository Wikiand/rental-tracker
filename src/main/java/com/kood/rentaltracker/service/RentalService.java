package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.RentalStatus;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.repository.ItemRepository;
import com.kood.rentaltracker.repository.RentalRepository;
import com.kood.rentaltracker.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class RentalService {

    private final RentalRepository rentalRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public RentalService(
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            UserRepository userRepository
    ) {
        this.rentalRepository = rentalRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    public Rental recordRental(
            Item item,
            String renterUsername,
            int durationDays
    ) {
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Item is not available for rental"
            );
        }

        if (durationDays <= 0) {
            throw new IllegalArgumentException(
                    "Rental duration must be greater than zero"
            );
        }

        User renter = findOrCreateRenter(renterUsername);

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(durationDays);

        Rental rental = new Rental(
                0,
                item.getId(),
                renter.getId(),
                startTime,
                endTime,
                null,
                RentalStatus.ACTIVE
        );

        Rental savedRental = rentalRepository.save(rental);

        itemRepository.updateStatus(
                item.getId(),
                ItemStatus.RENTED
        );

        return savedRental;
    }

    public void confirmReturn(Item item, Rental rental) {
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Rental is already closed"
            );
        }

        if (item.getStatus() != ItemStatus.RENTED) {
            throw new IllegalStateException(
                    "Item is not currently rented"
            );
        }

        LocalDateTime returnedAt = LocalDateTime.now();

        rentalRepository.closeRental(
                rental.getId(),
                returnedAt.toString()
        );

        itemRepository.updateStatus(
                item.getId(),
                ItemStatus.AVAILABLE
        );
    }

    private User findOrCreateRenter(String username) {
        Optional<User> existingUser =
                userRepository.findByUsername(username);

        return existingUser.orElseGet(
                () -> userRepository.save(new User(0, username))
        );
    }
}