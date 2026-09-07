package com.kood.rentaltracker.repository;

import com.kood.rentaltracker.domain.Rental;

import java.util.List;
import java.util.Optional;

public interface RentalRepository {

    Rental save(Rental rental);

    Optional<Rental> findById(int id);

    List<Rental> findActiveByOwnerId(int ownerId);

    List<Rental> findByOwnerId(int ownerId);

    void closeRental(int rentalId, String returnedAt);
}