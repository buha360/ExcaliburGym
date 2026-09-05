package com.wardanger.excalibur.solarium.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {
    void append(OutboxEvent event);
    List<OutboxEvent> findPending(int limit);
    void markPublished(UUID eventId, Instant publishedAt);
    void markFailed(UUID eventId, String error);
}
