package com.wardanger.excalibur.reporting.projection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import com.wardanger.excalibur.events.DomainEventEnvelope;
import org.junit.jupiter.api.Test;

class ReportingEventConsumerTest {

    private final StatisticsProjectionRepository projections = mock(StatisticsProjectionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReportingEventConsumer consumer = new ReportingEventConsumer(objectMapper, projections, clock);

    @Test
    void projectsCardPassSaleExactlyOnce() throws Exception {
        var eventId = UUID.randomUUID();
        var event = new DomainEventEnvelope(
                eventId,
                "PASS_SOLD",
                1,
                "GUEST_PASS",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-31T11:59:00Z"),
                Map.of("pricePaid", 15_000, "paymentMethod", "BANK_CARD"));
        when(projections.beginEvent(eq(eventId.toString()), eq("PASS_SOLD"), any(), any())).thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(projections).appendFact(
                eq(eventId.toString()),
                eq("PASS_SOLD"),
                eq(event.occurredAt()),
                eq(new StatisticsProjectionRepository.StatisticsDelta(0, 0, 1, 0, 0, 15_000, 0)));
    }

    @Test
    void projectsSaunaReservationAndCancellation() throws Exception {
        var actorId = UUID.randomUUID();
        var reservationId = UUID.randomUUID();
        var reserved = new DomainEventEnvelope(
                UUID.randomUUID(), "SAUNA_RESERVED", 1, "SAUNA_RESERVATION", reservationId, actorId,
                Instant.parse("2026-08-31T11:50:00Z"), Map.of("partySize", 2));
        var cancelled = new DomainEventEnvelope(
                UUID.randomUUID(), "SAUNA_RESERVATION_CANCELLED", 1, "SAUNA_RESERVATION", reservationId, actorId,
                Instant.parse("2026-08-31T11:55:00Z"), Map.of());
        when(projections.beginEvent(any(), any(), any(), any())).thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(reserved));
        consumer.consume(objectMapper.writeValueAsString(cancelled));

        verify(projections).appendFact(
                eq(reserved.eventId().toString()), eq("SAUNA_RESERVED"), eq(reserved.occurredAt()),
                eq(new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 1, 0, 0, 0)));
        verify(projections).appendFact(
                eq(cancelled.eventId().toString()), eq("SAUNA_RESERVATION_CANCELLED"), eq(cancelled.occurredAt()),
                eq(new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, -1, 0, 0, 0)));
    }

        @Test
    void projectsSolariumPurchaseRevenueWithoutCountingItAsGymPass() throws Exception {
        var eventId = UUID.randomUUID();
        var event = new DomainEventEnvelope(
                eventId, "SOLARIUM_MINUTES_PURCHASED", 1, "SOLARIUM_ACCOUNT", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-08-31T11:58:00Z"),
                Map.of("minutes", 60, "pricePaid", 4_000, "paymentMethod", "CASH"));
        when(projections.beginEvent(eq(eventId.toString()), eq("SOLARIUM_MINUTES_PURCHASED"), any(), any()))
                .thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(projections).appendFact(
                eq(eventId.toString()), eq("SOLARIUM_MINUTES_PURCHASED"), eq(event.occurredAt()),
                eq(new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 0, 4_000, 0, 0)));
    }
    @Test
    void projectsGuestBalanceDepositAsCashRevenueWithVersionedIdempotencyKey() throws Exception {
        var eventId = UUID.randomUUID();
        var event = new DomainEventEnvelope(
                eventId, "GUEST_BALANCE_DEPOSITED", 1, "GUEST_BALANCE_TRANSACTION", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-08-31T11:56:00Z"),
                Map.of("amount", 5_000, "paymentMethod", "CASH"));
        var projectionEventId = eventId + ":revenue-v2";
        when(projections.beginEvent(eq(projectionEventId), eq("GUEST_BALANCE_DEPOSITED"), any(), any()))
                .thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(projections).appendFact(
                eq(projectionEventId), eq("GUEST_BALANCE_DEPOSITED"), eq(event.occurredAt()),
                eq(new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 0, 5_000, 0, 0)));
    }
    @Test
    void projectsRetailSaleRevenueByPaymentMethod() throws Exception {
        var eventId = UUID.randomUUID();
        var event = new DomainEventEnvelope(
                eventId, "RETAIL_SALE_COMPLETED", 1, "RETAIL_SALE", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-08-31T11:57:00Z"),
                Map.of("totalPrice", 2_500, "paymentMethod", "PREPAID_BALANCE", "itemCount", 2));
        when(projections.beginEvent(eq(eventId.toString()), eq("RETAIL_SALE_COMPLETED"), any(), any()))
                .thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(projections).appendFact(
                eq(eventId.toString()), eq("RETAIL_SALE_COMPLETED"), eq(event.occurredAt()),
                eq(new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 0, 0, 0, 2_500)));
    }

    @Test
    void ignoresAlreadyProcessedEvent() throws Exception {
        var event = new DomainEventEnvelope(
                UUID.randomUUID(),
                "GUEST_CHECKED_IN",
                1,
                "CHECK_IN",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-31T11:59:00Z"),
                Map.of());
        when(projections.beginEvent(any(), any(), any(), any())).thenReturn(false);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(projections, never()).appendFact(any(), any(), any(), any());
    }
}