package com.wardanger.excalibur.product.domain;

import java.util.UUID;

public record ProductDefinition(
        UUID id,
        String code,
        String name,
        int validityDays,
        Integer entryLimit,
        long defaultPrice,
        boolean active) {
}
