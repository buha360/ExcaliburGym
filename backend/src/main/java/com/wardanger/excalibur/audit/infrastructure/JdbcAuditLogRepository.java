package com.wardanger.excalibur.audit.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogRepository;
import com.wardanger.excalibur.audit.domain.AuditEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.instant;
import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.timestamp;

@Repository
@RequiredArgsConstructor
public class JdbcAuditLogRepository implements AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(AuditEntry entry) {
        jdbcTemplate.update("""
                INSERT INTO audit_log
                    (id, employee_id, employee_name, action, entity_type, entity_id, summary, details, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entry.id().toString(), entry.employeeId().toString(), entry.employeeName(), entry.action(),
                entry.entityType(), entry.entityId() == null ? null : entry.entityId().toString(),
                entry.summary(), entry.details(), timestamp(entry.occurredAt()));
    }

    @Override
    public List<AuditEntry> findLatest(int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM audit_log
                ORDER BY occurred_at DESC, id DESC
                LIMIT ?
                """, JdbcAuditLogRepository::mapEntry, limit);
    }

    @Override
    public List<AuditEntry> findLatestByEmployee(UUID employeeId, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM audit_log
                WHERE employee_id = ?
                ORDER BY occurred_at DESC, id DESC
                LIMIT ?
                """, JdbcAuditLogRepository::mapEntry, employeeId.toString(), limit);
    }

    private static AuditEntry mapEntry(ResultSet resultSet, int rowNumber) throws SQLException {
        var entityId = resultSet.getString("entity_id");
        return new AuditEntry(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("employee_id")),
                resultSet.getString("employee_name"),
                resultSet.getString("action"),
                resultSet.getString("entity_type"),
                entityId == null ? null : UUID.fromString(entityId),
                resultSet.getString("summary"),
                resultSet.getString("details"),
                instant(resultSet, "occurred_at"));
    }
}
