package com.wardanger.excalibur.integration.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.wardanger.excalibur.events.DomainEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntegrationEventPublisher {

    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    public void publish(
            String topic,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            UUID actorEmployeeId,
            Instant occurredAt,
            Map<String, Object> payload) {
        var envelope = new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                aggregateType,
                aggregateId,
                actorEmployeeId,
                occurredAt,
                Map.copyOf(payload));
        try {
            outbox.append(new OutboxEvent(
                    envelope.eventId(),
                    topic,
                    aggregateId,
                    objectMapper.writeValueAsString(envelope),
                    occurredAt,
                    0));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Az integrációs esemény szerializálása nem sikerült.", exception);
        }
    }
}
