package com.wardanger.excalibur.visit.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.visit.domain.GuestCheckIn;

public interface GuestCheckInRepository {

    void insert(GuestCheckIn checkIn);

    List<GuestCheckIn> findByGuestId(UUID guestId);

    Optional<GuestCheckIn> findByIdForGuest(UUID checkInId, UUID guestId);

    boolean reverse(UUID checkInId, Instant reversedAt, UUID employeeId, String reason);

    boolean existsActiveForGuestSince(UUID guestId, Instant fromInclusive);

    long countActive(Instant fromInclusive, Instant toExclusive);
}
