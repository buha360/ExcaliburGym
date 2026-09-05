package com.wardanger.excalibur.solarium.account.domain;

import java.time.Instant;
import java.util.UUID;

public record SolariumTransaction(
        UUID id,
        Type type,
        int minutesDelta,
        int balanceAfter,
        String productName,
        Integer quantity,
        Long pricePaid,
        String paymentMethod,
        Instant occurredAt,
        String employeeName) {
    public enum Type { PURCHASE, USAGE }
}