package com.wardanger.excalibur.guest.domain;

import java.time.Instant;
import java.util.UUID;

public record Guest(
        UUID id,
        String fullName,
        GuestRegistrationType registrationType,
        Instant createdAt,
        UUID createdByEmployeeId) {
}
