
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
import java.util.List;

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
                    "Item is not available for rental."
            );
        }

        if (durationDays <= 0) {
            throw new IllegalArgumentException(
                    "Rental duration must be greater than zero."
            );
        }

        if (renterUsername == null
                || renterUsername.isBlank()) {
            throw new IllegalArgumentException(
                    "Renter username cannot be empty."
            );
        }

        User renter = findOrCreateRenter(
                renterUsername.trim()
        );

        LocalDateTime startTime =
                LocalDateTime.now();

        LocalDateTime endTime =
                startTime.plusDays(durationDays);

        Rental rental = new Rental(
                0,
                item.getId(),
                renter.getId(),
                startTime,
                endTime,
                null,
                RentalStatus.ACTIVE
        );

        Rental savedRental =
                rentalRepository.save(rental);

        itemRepository.updateStatus(
                item.getId(),
                ItemStatus.RENTED
        );

        item.setStatus(ItemStatus.RENTED);

        return savedRental;
    }

    public void confirmReturn(
            Item item,
            Rental rental
    ) {
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Rental is already closed."
            );
        }

        if (item.getStatus() != ItemStatus.RENTED
                && item.getStatus() != ItemStatus.UNLISTED) {
            throw new IllegalStateException(
                    "Item is not currently rented."
            );
        }

        LocalDateTime returnedAt =
                LocalDateTime.now();

        rentalRepository.closeRental(
                rental.getId(),
                returnedAt.toString()
        );

        rental.setReturnedAt(returnedAt);
        rental.setStatus(RentalStatus.CLOSED);

        ItemStatus returnStatus =
                item.getStatus() == ItemStatus.UNLISTED
                        ? ItemStatus.UNLISTED
                        : ItemStatus.AVAILABLE;

        itemRepository.updateStatus(
                item.getId(),
                returnStatus
        );

        item.setStatus(returnStatus);
    }

    public List<Rental> getActiveRentals(int ownerId) {
        return rentalRepository.findActiveByOwnerId(ownerId);
    }

    public List<Rental> getRentalHistory(int ownerId) {
        return rentalRepository.findByOwnerId(ownerId);
    }

    public Rental getRental(int rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rental not found: " + rentalId
                        )
                );
    }

    public User getRenter(Rental rental) {
        return userRepository.findById(
                        rental.getRenterId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Renter not found: "
                                        + rental.getRenterId()
                        )
                );
    }

    public Item getItem(Rental rental) {
        return itemRepository.findById(
                        rental.getItemId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found: "
                                        + rental.getItemId()
                        )
                );
    }

    private User findOrCreateRenter(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() ->
                        userRepository.save(
                                new User(0, username)
                        )
                );
    }
}
