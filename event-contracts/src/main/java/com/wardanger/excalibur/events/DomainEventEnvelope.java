package com.wardanger.excalibur.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateType,
        UUID aggregateId,
        UUID actorEmployeeId,
        Instant occurredAt,
        Map<String, Object> payload) {
}
