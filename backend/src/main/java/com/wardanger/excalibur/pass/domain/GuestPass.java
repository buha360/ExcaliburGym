package com.wardanger.excalibur.pass.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GuestPass(
        UUID id,
        UUID guestId,
        UUID productDefinitionId,
        String productName,
        Instant purchasedAt,
        LocalDate validFrom,
        LocalDate validUntil,
        Integer totalEntries,
        Integer remainingEntries,
        long pricePaid,
        PaymentMethod paymentMethod,
        UUID issuedByEmployeeId,
        String issuedByEmployeeName,
        Instant invalidatedAt) {

    public Status status(LocalDate today) {
        if (invalidatedAt != null) {
            return Status.INVALIDATED;
        }
        if (today.isBefore(validFrom)) {
            return Status.PENDING;
        }
        if (today.isAfter(validUntil) || remainingEntries != null && remainingEntries <= 0) {
            return Status.EXPIRED;
        }
        return Status.ACTIVE;
    }

    public enum Status {
        ACTIVE,
        PENDING,
        EXPIRED,
        INVALIDATED
    }
}
