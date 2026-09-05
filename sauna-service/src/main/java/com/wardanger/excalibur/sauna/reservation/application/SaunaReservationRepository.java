package com.wardanger.excalibur.sauna.reservation.application;

import com.wardanger.excalibur.sauna.reservation.domain.SaunaReservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaunaReservationRepository {
    void insert(SaunaReservation reservation);
    List<SaunaReservation> findBetween(Instant fromInclusive, Instant toExclusive);
    Optional<SaunaReservation> findById(UUID id);
    boolean cancel(UUID id, Instant cancelledAt, UUID employeeId, String employeeName, String reason);
}
