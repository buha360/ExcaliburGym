package com.wardanger.excalibur.visit.domain;

import java.time.Instant;
import java.util.UUID;

public record GuestCheckIn(
        UUID id,
        UUID guestId,
        UUID guestPassId,
        String passName,
        Instant checkedInAt,
        UUID checkedInByEmployeeId,
        String checkedInByEmployeeName,
        boolean consumedEntry,
        Instant reversedAt,
        UUID reversedByEmployeeId,
        String reversedByEmployeeName,
        String reversalReason) {

    public boolean reversed() {
        return reversedAt != null;
    }
}
