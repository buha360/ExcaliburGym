package com.wardanger.excalibur.sauna.reservation.infrastructure;

import com.wardanger.excalibur.sauna.reservation.application.SaunaReservationRepository;
import com.wardanger.excalibur.sauna.reservation.domain.SaunaReservation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcSaunaReservationRepository implements SaunaReservationRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(SaunaReservation reservation) {
        jdbcTemplate.update("""
                INSERT INTO sauna_reservation
                    (id, guest_id, guest_name, start_at, end_at, party_size, status, created_at,
                     created_by_employee_id, created_by_employee_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                reservation.id(), reservation.guestId(), reservation.guestName(),
                Timestamp.from(reservation.startAt()), Timestamp.from(reservation.endAt()),
                reservation.partySize(), reservation.status().name(), Timestamp.from(reservation.createdAt()),
                reservation.createdByEmployeeId(), reservation.createdByEmployeeName());
    }

    @Override
    public List<SaunaReservation> findBetween(Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query("""
                SELECT * FROM sauna_reservation
                WHERE start_at < ? AND end_at > ?
                ORDER BY start_at, created_at
                """, JdbcSaunaReservationRepository::map, Timestamp.from(toExclusive), Timestamp.from(fromInclusive));
    }

    @Override
    public Optional<SaunaReservation> findById(UUID id) {
        return jdbcTemplate.query("SELECT * FROM sauna_reservation WHERE id = ?",
                JdbcSaunaReservationRepository::map, id).stream().findFirst();
    }

    @Override
    public boolean cancel(UUID id, Instant cancelledAt, UUID employeeId, String employeeName, String reason) {
        return jdbcTemplate.update("""
                UPDATE sauna_reservation
                SET status = 'CANCELLED', cancelled_at = ?, cancelled_by_employee_id = ?,
                    cancelled_by_employee_name = ?, cancellation_reason = ?
                WHERE id = ? AND status = 'RESERVED'
                """, Timestamp.from(cancelledAt), employeeId, employeeName, reason, id) == 1;
    }

    private static SaunaReservation map(ResultSet rs, int row) throws SQLException {
        return new SaunaReservation(
                rs.getObject("id", UUID.class),
                rs.getObject("guest_id", UUID.class),
                rs.getString("guest_name"),
                rs.getTimestamp("start_at").toInstant(),
                rs.getTimestamp("end_at").toInstant(),
                rs.getInt("party_size"),
                SaunaReservation.Status.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getObject("created_by_employee_id", UUID.class),
                rs.getString("created_by_employee_name"),
                instantOrNull(rs, "cancelled_at"),
                uuidOrNull(rs, "cancelled_by_employee_id"),
                rs.getString("cancelled_by_employee_name"),
                rs.getString("cancellation_reason"));
    }

    private static Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static UUID uuidOrNull(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }
}
