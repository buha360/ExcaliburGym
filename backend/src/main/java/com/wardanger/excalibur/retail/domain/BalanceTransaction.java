package com.wardanger.excalibur.retail.domain;

import java.time.Instant;
import java.util.UUID;

import com.wardanger.excalibur.pass.domain.PaymentMethod;

public record BalanceTransaction(
        UUID id,
        UUID guestId,
        String guestName,
        Type type,
        long amountDelta,
        long balanceAfter,
        PaymentMethod paymentMethod,
        UUID referenceId,
        UUID employeeId,
        String employeeName,
        Instant occurredAt) {

    public enum Type {
        DEPOSIT,
        PURCHASE
    }
}