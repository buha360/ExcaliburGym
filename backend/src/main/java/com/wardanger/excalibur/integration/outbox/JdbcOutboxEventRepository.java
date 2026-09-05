package com.wardanger.excalibur.integration.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.instant;
import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.timestamp;

@Repository
@RequiredArgsConstructor
public class JdbcOutboxEventRepository implements OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void append(OutboxEvent event) {
        jdbcTemplate.update("""
                INSERT INTO outbox_event
                    (id, topic, aggregate_id, payload, occurred_at, attempts)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                event.id().toString(), event.topic(), event.aggregateId().toString(), event.payload(),
                timestamp(event.occurredAt()), event.attempts());
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return jdbcTemplate.query("""
                SELECT id, topic, aggregate_id, payload, occurred_at, attempts
                FROM outbox_event
                WHERE published_at IS NULL
                ORDER BY occurred_at, id
                LIMIT ?
                """, JdbcOutboxEventRepository::mapEvent, limit);
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        jdbcTemplate.update("""
                UPDATE outbox_event
                SET published_at = ?, last_error = NULL
                WHERE id = ? AND published_at IS NULL
                """, timestamp(publishedAt), eventId.toString());
    }

    @Override
    public void markFailed(UUID eventId, String error) {
        var safeError = error == null ? "Ismeretlen Kafka publikációs hiba" : error.substring(0, Math.min(error.length(), 1000));
        jdbcTemplate.update("""
                UPDATE outbox_event
                SET attempts = attempts + 1, last_error = ?
                WHERE id = ? AND published_at IS NULL
                """, safeError, eventId.toString());
    }

    private static OutboxEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxEvent(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("topic"),
                UUID.fromString(resultSet.getString("aggregate_id")),
                resultSet.getString("payload"),
                instant(resultSet, "occurred_at"),
                resultSet.getInt("attempts"));
    }
}
