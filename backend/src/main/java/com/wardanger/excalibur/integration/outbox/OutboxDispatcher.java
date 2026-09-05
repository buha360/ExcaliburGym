package com.wardanger.excalibur.integration.outbox;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "excalibur.events.publishing-enabled", havingValue = "true")
public class OutboxDispatcher {

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${excalibur.events.dispatch-delay-ms:500}")
    public void dispatchPendingEvents() {
        for (var event : outbox.findPending(100)) {
            try {
                kafkaTemplate.send(event.topic(), event.aggregateId().toString(), event.payload())
                        .get(10, TimeUnit.SECONDS);
                outbox.markPublished(event.id(), clock.instant());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                outbox.markFailed(event.id(), exception.getMessage());
                log.warn("Outbox dispatch interrupted while publishing event {}", event.id(), exception);
                return;
            } catch (Exception exception) {
                outbox.markFailed(event.id(), exception.getMessage());
                log.warn("Outbox event {} could not be published to {}", event.id(), event.topic(), exception);
            }
        }
    }
}
