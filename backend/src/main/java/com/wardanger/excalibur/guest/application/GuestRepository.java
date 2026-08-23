package com.wardanger.excalibur.guest.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.guest.domain.Guest;

public interface GuestRepository {

    void insert(Guest guest, String normalizedName);

    Optional<Guest> findById(UUID id);

    List<Guest> findQuickSearch(String normalizedQuery, LocalDate today);

    long countNewGuests(Instant fromInclusive, Instant toExclusive);
}
