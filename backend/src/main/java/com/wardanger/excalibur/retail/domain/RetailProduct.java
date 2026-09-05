package com.wardanger.excalibur.retail.domain;

import java.time.Instant;
import java.util.UUID;

public record RetailProduct(
        UUID id,
        String code,
        String name,
        long defaultPrice,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}