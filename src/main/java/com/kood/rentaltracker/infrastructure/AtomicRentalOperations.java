package com.kood.rentaltracker.infrastructure;

import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.repository.ItemRepositoryImpl;
import com.kood.rentaltracker.repository.RentalRepositoryImpl;

import java.time.LocalDateTime;

public final class AtomicRentalOperations {

    private final RentalTransactionManager transactionManager;
    private final RentalRepositoryImpl rentalRepository;
    private final ItemRepositoryImpl itemRepository;

    public AtomicRentalOperations(
            RentalTransactionManager transactionManager,
            RentalRepositoryImpl rentalRepository,
            ItemRepositoryImpl itemRepository
    ) {
        this.transactionManager = transactionManager;
        this.rentalRepository = rentalRepository;
        this.itemRepository = itemRepository;
    }

    public Rental createRental(Rental rental) {
        return transactionManager.execute(connection -> {
            itemRepository.updateStatusIfCurrent(
                    connection,
                    rental.getItemId(),
                    ItemStatus.AVAILABLE,
                    ItemStatus.RENTED
            );

            return rentalRepository.save(connection, rental);
        });
    }

    public void confirmReturn(
            int rentalId,
            int itemId,
            LocalDateTime returnedAt,
            ItemStatus returnStatus
    ) {
        transactionManager.execute(connection -> {
            rentalRepository.closeRentalForItem(
                    connection,
                    rentalId,
                    itemId,
                    returnedAt.toString()
            );

            ItemStatus expectedStatus =
                    returnStatus == ItemStatus.UNLISTED
                            ? ItemStatus.UNLISTED
                            : ItemStatus.RENTED;

            itemRepository.updateStatusIfCurrent(
                    connection,
                    itemId,
                    expectedStatus,
                    returnStatus
            );

            return null;
        });
    }
}
