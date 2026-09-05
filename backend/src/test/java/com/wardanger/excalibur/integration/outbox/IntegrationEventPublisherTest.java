package com.wardanger.excalibur.integration.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.wardanger.excalibur.events.DomainEventEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class IntegrationEventPublisherTest {

    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntegrationEventPublisher publisher = new IntegrationEventPublisher(outbox, objectMapper);

    @Test
    void appendsVersionedEnvelopeWithoutContactingKafka() throws Exception {
        var aggregateId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var occurredAt = Instant.parse("2026-08-31T12:00:00Z");

        publisher.publish(
                "excalibur.pass-events.v1",
                "PASS_SOLD",
                "GUEST_PASS",
                aggregateId,
                employeeId,
                occurredAt,
                Map.of("pricePaid", 15_000, "paymentMethod", "CASH"));

        var eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).append(eventCaptor.capture());
        var stored = eventCaptor.getValue();
        var envelope = objectMapper.readValue(stored.payload(), DomainEventEnvelope.class);

        assertThat(stored.topic()).isEqualTo("excalibur.pass-events.v1");
        assertThat(stored.aggregateId()).isEqualTo(aggregateId);
        assertThat(stored.occurredAt()).isEqualTo(occurredAt);
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.eventType()).isEqualTo("PASS_SOLD");
        assertThat(envelope.aggregateId()).isEqualTo(aggregateId);
        assertThat(envelope.actorEmployeeId()).isEqualTo(employeeId);
        assertThat(envelope.payload()).containsEntry("paymentMethod", "CASH");
    }
}

