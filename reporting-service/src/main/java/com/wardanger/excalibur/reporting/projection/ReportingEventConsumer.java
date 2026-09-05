package com.wardanger.excalibur.reporting.projection;

import com.wardanger.excalibur.events.DomainEventEnvelope;
import com.wardanger.excalibur.events.EventTopics;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingEventConsumer {
    private final ObjectMapper objectMapper;
    private final StatisticsProjectionRepository projections;
    private final Clock clock;

    @Transactional
    @KafkaListener(topics = {EventTopics.GUEST_EVENTS, EventTopics.PASS_EVENTS, EventTopics.VISIT_EVENTS,
            EventTopics.SAUNA_EVENTS, EventTopics.SOLARIUM_EVENTS, EventTopics.RETAIL_EVENTS}, groupId = "excalibur-reporting-v2")
    public void consume(String json) throws JacksonException {
        var event = objectMapper.readValue(json, DomainEventEnvelope.class);
        if (event.eventVersion() != 1) throw new IllegalArgumentException("Nem támogatott eseményverzió: " + event.eventVersion());
        var projectionEventId = "GUEST_BALANCE_DEPOSITED".equals(event.eventType())
                ? event.eventId() + ":revenue-v2"
                : event.eventId().toString();
        if (!projections.beginEvent(projectionEventId, event.eventType(), event.occurredAt(), clock.instant())) {
            log.debug("Duplicate event {} ignored", event.eventId());
            return;
        }
        var delta = toDelta(event);
        if (!delta.equals(StatisticsProjectionRepository.StatisticsDelta.zero())) {
            projections.appendFact(projectionEventId, event.eventType(), event.occurredAt(), delta);
        }
    }

    private static StatisticsProjectionRepository.StatisticsDelta toDelta(DomainEventEnvelope event) {
        return switch (event.eventType()) {
            case "GUEST_CREATED" -> new StatisticsProjectionRepository.StatisticsDelta(0, "NEW".equals(event.payload().get("registrationType")) ? 1 : 0, 0, 0, 0, 0, 0);
            case "PASS_SOLD" -> passSoldDelta(event);
            case "GUEST_CHECKED_IN" -> new StatisticsProjectionRepository.StatisticsDelta(1, 0, 0, 0, 0, 0, 0);
            case "CHECK_IN_REVERSED" -> new StatisticsProjectionRepository.StatisticsDelta(-1, 0, 0, 0, 0, 0, 0);
            case "SAUNA_RESERVED" -> new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 1, 0, 0, 0);
            case "SAUNA_RESERVATION_CANCELLED" -> new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, -1, 0, 0, 0);
            case "SOLARIUM_MINUTES_PURCHASED" -> solariumPurchaseDelta(event);
            case "GUEST_BALANCE_DEPOSITED" -> guestBalanceDepositDelta(event);
            case "RETAIL_SALE_COMPLETED" -> retailSaleDelta(event);
            default -> StatisticsProjectionRepository.StatisticsDelta.zero();
        };
    }

    private static StatisticsProjectionRepository.StatisticsDelta passSoldDelta(DomainEventEnvelope event) {
        var price = ((Number) event.payload().get("pricePaid")).longValue();
        var method = String.valueOf(event.payload().get("paymentMethod"));
        return new StatisticsProjectionRepository.StatisticsDelta(0, 0, 1, 0,
                "CASH".equals(method) ? price : 0,
                "BANK_CARD".equals(method) ? price : 0,
                "PREPAID_BALANCE".equals(method) ? price : 0);
    }

    private static StatisticsProjectionRepository.StatisticsDelta guestBalanceDepositDelta(DomainEventEnvelope event) {
        var amount = ((Number) event.payload().get("amount")).longValue();
        var method = String.valueOf(event.payload().get("paymentMethod"));
        return new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 0,
                "CASH".equals(method) ? amount : 0,
                "BANK_CARD".equals(method) ? amount : 0,
                0);
    }
    private static StatisticsProjectionRepository.StatisticsDelta retailSaleDelta(DomainEventEnvelope event) {
        var price = ((Number) event.payload().get("totalPrice")).longValue();
        var method = String.valueOf(event.payload().get("paymentMethod"));
        return new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 0,
                "CASH".equals(method) ? price : 0,
                "BANK_CARD".equals(method) ? price : 0,
                "PREPAID_BALANCE".equals(method) ? price : 0);
    }
    private static StatisticsProjectionRepository.StatisticsDelta solariumPurchaseDelta(DomainEventEnvelope event) {
        var price = ((Number) event.payload().get("pricePaid")).longValue();
        var method = String.valueOf(event.payload().get("paymentMethod"));
        return new StatisticsProjectionRepository.StatisticsDelta(0, 0, 0, 0,
                "CASH".equals(method) ? price : 0,
                "BANK_CARD".equals(method) ? price : 0,
                "PREPAID_BALANCE".equals(method) ? price : 0);
    }
}
