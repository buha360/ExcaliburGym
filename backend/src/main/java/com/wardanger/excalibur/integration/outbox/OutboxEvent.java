package com.wardanger.excalibur.integration.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String topic,
        UUID aggregateId,
        String payload,
        Instant occurredAt,
        int attempts) {
}
