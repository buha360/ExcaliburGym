package com.wardanger.excalibur.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        UUID employeeId,
        String employeeName,
        String action,
        String entityType,
        UUID entityId,
        String summary,
        String details,
        Instant occurredAt) {
}
