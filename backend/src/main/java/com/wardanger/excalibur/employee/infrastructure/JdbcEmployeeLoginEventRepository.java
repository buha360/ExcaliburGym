package com.wardanger.excalibur.employee.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.wardanger.excalibur.employee.application.EmployeeLoginEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.timestamp;

@Repository
public class JdbcEmployeeLoginEventRepository implements EmployeeLoginEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEmployeeLoginEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(UUID eventId, UUID employeeId, LoginEventType eventType, Instant occurredAt) {
        jdbcTemplate.update("""
                INSERT INTO employee_login_event (id, employee_id, event_type, occurred_at)
                VALUES (?, ?, ?, ?)
                """,
                eventId.toString(),
                employeeId.toString(),
                eventType.name(),
                timestamp(occurredAt));
    }
}
