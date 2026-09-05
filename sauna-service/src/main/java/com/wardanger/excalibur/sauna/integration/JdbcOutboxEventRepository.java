package com.wardanger.excalibur.sauna.integration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOutboxEventRepository implements OutboxEventRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void append(OutboxEvent event) {
        jdbcTemplate.update("""
                INSERT INTO outbox_event (id, topic, aggregate_id, payload, occurred_at, attempts)
                VALUES (?, ?, ?, ?, ?, ?)
                """, event.id(), event.topic(), event.aggregateId(), event.payload(),
                Timestamp.from(event.occurredAt()), event.attempts());
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return jdbcTemplate.query("""
                SELECT id, topic, aggregate_id, payload, occurred_at, attempts
                FROM outbox_event WHERE published_at IS NULL
                ORDER BY occurred_at, id LIMIT ?
                """, JdbcOutboxEventRepository::map, limit);
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        jdbcTemplate.update("""
                UPDATE outbox_event SET published_at = ?, last_error = NULL
                WHERE id = ? AND published_at IS NULL
                """, Timestamp.from(publishedAt), eventId);
    }

    @Override
    public void markFailed(UUID eventId, String error) {
        var safeError = error == null ? "Ismeretlen Kafka publikációs hiba" : error.substring(0, Math.min(error.length(), 1000));
        jdbcTemplate.update("""
                UPDATE outbox_event SET attempts = attempts + 1, last_error = ?
                WHERE id = ? AND published_at IS NULL
                """, safeError, eventId);
    }

    private static OutboxEvent map(ResultSet rs, int row) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                rs.getString("topic"),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("payload"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getInt("attempts"));
    }
}
