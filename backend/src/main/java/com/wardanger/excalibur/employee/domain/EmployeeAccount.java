package com.wardanger.excalibur.employee.domain;

import java.time.Instant;
import java.util.UUID;

public record EmployeeAccount(
        UUID id,
        String displayName,
        String pinHash,
        EmployeeRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {
}
