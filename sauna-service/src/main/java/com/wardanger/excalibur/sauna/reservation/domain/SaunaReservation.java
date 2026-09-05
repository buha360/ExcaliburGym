package com.wardanger.excalibur.sauna.reservation.domain;

import java.time.Instant;
import java.util.UUID;

public record SaunaReservation(
        UUID id,
        UUID guestId,
        String guestName,
        Instant startAt,
        Instant endAt,
        int partySize,
        Status status,
        Instant createdAt,
        UUID createdByEmployeeId,
        String createdByEmployeeName,
        Instant cancelledAt,
        UUID cancelledByEmployeeId,
        String cancelledByEmployeeName,
        String cancellationReason) {

    public enum Status {
        RESERVED,
        CANCELLED
    }

    public boolean cancelled() {
        return status == Status.CANCELLED;
    }
}
