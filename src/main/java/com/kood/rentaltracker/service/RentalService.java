package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.RentalStatus;
import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.AtomicRentalOperations;
import com.kood.rentaltracker.logging.AppLogger;
import com.kood.rentaltracker.repository.ItemRepository;
import com.kood.rentaltracker.repository.RentalRepository;
import com.kood.rentaltracker.repository.UserRepository;
import com.kood.rentaltracker.validation.RentalValidator;

import java.time.LocalDateTime;
import java.util.List;

public class RentalService {

    private final RentalRepository rentalRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final RentalValidator rentalValidator;
    private final AppLogger logger;
    private AtomicRentalOperations atomicOperations;

    public RentalService(
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            UserRepository userRepository
    ) {
        this(
                rentalRepository,
                itemRepository,
                userRepository,
                new RentalValidator(),
                new AppLogger()
        );
    }

    public RentalService(
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            RentalValidator rentalValidator
    ) {
        this(
                rentalRepository,
                itemRepository,
                userRepository,
                rentalValidator,
                new AppLogger()
        );
    }

    public RentalService(
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            RentalValidator rentalValidator,
            AppLogger logger
    ) {
        this.rentalRepository = rentalRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.rentalValidator = rentalValidator;
        this.logger = logger;
    }

    public RentalService(
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            AtomicRentalOperations atomicOperations
    ) {
        this(rentalRepository, itemRepository, userRepository);
        this.atomicOperations = java.util.Objects.requireNonNull(
                atomicOperations
        );
    }

    public Rental recordRental(
            Item item,
            int renterId,
            int durationDays
    ) {
        if (item == null) {
            logger.warn(
                    "rental.validation.failed",
                    "userId=" + renterId
                            + " reason=item_required"
            );

            throw new IllegalArgumentException(
                    "Item cannot be null."
            );
        }

        try {
            rentalValidator.validate(
                    item.getId(),
                    renterId,
                    durationDays
            );
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "rental.validation.failed",
                    "userId=" + renterId
                            + " itemId=" + item.getId()
                            + " reason=" + safeMessage(e)
            );

            throw e;
        }

        if (item.getStatus() != ItemStatus.AVAILABLE) {
            logger.warn(
                    "rental.failed",
                    "userId=" + renterId
                            + " itemId=" + item.getId()
                            + " reason=item_not_available"
            );

            throw new IllegalStateException(
                    "Item is not available for rental."
            );
        }

        if (item.getOwnerId() == renterId) {
            logger.warn(
                    "rental.ownership.denied",
                    "userId=" + renterId
                            + " itemId=" + item.getId()
                            + " reason=owner_cannot_rent_own_item"
            );

            throw new IllegalStateException(
                    "You cannot rent your own item."
            );
        }

        User renter =
                userRepository.findById(renterId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Renter account not found."
                                )
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

        Rental savedRental;

        if (atomicOperations != null) {
            savedRental = atomicOperations.createRental(rental);
        } else {
            savedRental = rentalRepository.save(rental);

            itemRepository.updateStatus(
                    item.getId(),
                    ItemStatus.RENTED
            );
        }

        item.setStatus(ItemStatus.RENTED);

        logger.info(
                "rental.created",
                "userId=" + renterId
                        + " itemId=" + item.getId()
                        + " rentalId=" + savedRental.getId()
        );

        return savedRental;
    }

    public void confirmReturn(
            Item item,
            Rental rental,
            int currentUserId
    ) {
        if (item == null || rental == null) {
            logger.warn(
                    "return.validation.failed",
                    "userId=" + currentUserId
                            + " reason=item_or_rental_required"
            );

            throw new IllegalArgumentException(
                    "Item and rental are required."
            );
        }

        rentalValidator.validateItemId(
                item.getId()
        );

        rentalValidator.validateRenterId(
                rental.getRenterId()
        );

        rentalValidator.validateRenterId(
                currentUserId
        );

        if (item.getOwnerId() != currentUserId) {
            logger.warn(
                    "return.ownership.denied",
                    "userId=" + currentUserId
                            + " itemId=" + item.getId()
                            + " rentalId=" + rental.getId()
            );

            throw new IllegalStateException(
                    "You can only confirm returns for your own items."
            );
        }

        if (rental.getItemId() != item.getId()) {
            logger.warn(
                    "return.failed",
                    "userId=" + currentUserId
                            + " itemId=" + item.getId()
                            + " rentalId=" + rental.getId()
                            + " reason=rental_item_mismatch"
            );

            throw new IllegalStateException(
                    "Rental does not belong to this item."
            );
        }

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            logger.warn(
                    "return.failed",
                    "userId=" + currentUserId
                            + " itemId=" + item.getId()
                            + " rentalId=" + rental.getId()
                            + " reason=rental_already_closed"
            );

            throw new IllegalStateException(
                    "Rental is already closed."
            );
        }

        if (item.getStatus() != ItemStatus.RENTED
                && item.getStatus() != ItemStatus.UNLISTED) {
            logger.warn(
                    "return.failed",
                    "userId=" + currentUserId
                            + " itemId=" + item.getId()
                            + " rentalId=" + rental.getId()
                            + " reason=item_not_rented"
            );

            throw new IllegalStateException(
                    "Item is not currently rented."
            );
        }

        LocalDateTime returnedAt =
                LocalDateTime.now();

        ItemStatus returnStatus =
                item.getStatus() == ItemStatus.UNLISTED
                        ? ItemStatus.UNLISTED
                        : ItemStatus.AVAILABLE;

        if (atomicOperations != null) {
            atomicOperations.confirmReturn(
                    rental.getId(),
                    item.getId(),
                    returnedAt,
                    returnStatus
            );
        } else {
            rentalRepository.closeRental(
                    rental.getId(),
                    returnedAt.toString()
            );

            itemRepository.updateStatus(
                    item.getId(),
                    returnStatus
            );
        }

        rental.setReturnedAt(returnedAt);
        rental.setStatus(RentalStatus.CLOSED);
        item.setStatus(returnStatus);

        logger.info(
                "return.confirmed",
                "userId=" + currentUserId
                        + " itemId=" + item.getId()
                        + " rentalId=" + rental.getId()
        );
    }

    public List<Rental> getActiveRentals(
            int ownerId
    ) {
        rentalValidator.validateRenterId(ownerId);

        return rentalRepository.findActiveByOwnerId(
                ownerId
        );
    }

    public List<Rental> getRentalHistory(
            int ownerId
    ) {
        rentalValidator.validateRenterId(ownerId);

        return rentalRepository.findByOwnerId(
                ownerId
        );
    }

    public Rental getRental(
            int rentalId
    ) {
        if (rentalId <= 0) {
            throw new IllegalArgumentException(
                    "Rental reference is invalid."
            );
        }

        return rentalRepository.findById(rentalId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rental not found."
                        )
                );
    }

    public User getRenter(
            Rental rental
    ) {
        if (rental == null) {
            throw new IllegalArgumentException(
                    "Rental cannot be null."
            );
        }

        rentalValidator.validateRenterId(
                rental.getRenterId()
        );

        return userRepository.findById(
                        rental.getRenterId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Renter account not found."
                        )
                );
    }

    public Item getItem(
            Rental rental
    ) {
        if (rental == null) {
            throw new IllegalArgumentException(
                    "Rental cannot be null."
            );
        }

        rentalValidator.validateItemId(
                rental.getItemId()
        );

        return itemRepository.findById(
                        rental.getItemId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found."
                        )
                );
    }

    private String safeMessage(
            Exception exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "validation-error";
        }

        return message
                .replace(" ", "_")
                .replace(
                        System.lineSeparator(),
                        ""
                );
    }
}
