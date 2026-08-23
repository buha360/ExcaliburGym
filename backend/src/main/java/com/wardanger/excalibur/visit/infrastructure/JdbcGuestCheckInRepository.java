package com.wardanger.excalibur.visit.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.visit.application.GuestCheckInRepository;
import com.wardanger.excalibur.visit.domain.GuestCheckIn;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcGuestCheckInRepository implements GuestCheckInRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(GuestCheckIn checkIn) {
        jdbcTemplate.update("""
                INSERT INTO guest_check_in
                    (id, guest_id, guest_pass_id, checked_in_at, checked_in_by_employee_id, consumed_entry)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                checkIn.id().toString(), checkIn.guestId().toString(), checkIn.guestPassId().toString(),
                checkIn.checkedInAt().toString(), checkIn.checkedInByEmployeeId().toString(), checkIn.consumedEntry());
    }

    @Override
    public List<GuestCheckIn> findByGuestId(UUID guestId) {
        return jdbcTemplate.query("""
                        SELECT ci.*, gp.product_name_snapshot AS pass_name,
                               ea.display_name AS checked_in_by_employee_name,
                               reversed_by.display_name AS reversed_by_employee_name
                        FROM guest_check_in ci
                        JOIN guest_pass gp ON gp.id = ci.guest_pass_id
                        JOIN employee_account ea ON ea.id = ci.checked_in_by_employee_id
                        LEFT JOIN employee_account reversed_by ON reversed_by.id = ci.reversed_by_employee_id
                        WHERE ci.guest_id = ?
                        ORDER BY ci.checked_in_at DESC
                        """,
                JdbcGuestCheckInRepository::mapCheckIn,
                guestId.toString());
    }

    @Override
    public Optional<GuestCheckIn> findByIdForGuest(UUID checkInId, UUID guestId) {
        return jdbcTemplate.query("""
                        SELECT ci.*, gp.product_name_snapshot AS pass_name,
                               ea.display_name AS checked_in_by_employee_name,
                               reversed_by.display_name AS reversed_by_employee_name
                        FROM guest_check_in ci
                        JOIN guest_pass gp ON gp.id = ci.guest_pass_id
                        JOIN employee_account ea ON ea.id = ci.checked_in_by_employee_id
                        LEFT JOIN employee_account reversed_by ON reversed_by.id = ci.reversed_by_employee_id
                        WHERE ci.id = ? AND ci.guest_id = ?
                        """,
                JdbcGuestCheckInRepository::mapCheckIn,
                checkInId.toString(),
                guestId.toString()).stream().findFirst();
    }

    @Override
    public boolean reverse(UUID checkInId, Instant reversedAt, UUID employeeId, String reason) {
        return jdbcTemplate.update("""
                UPDATE guest_check_in
                SET reversed_at = ?, reversed_by_employee_id = ?, reversal_reason = ?
                WHERE id = ? AND reversed_at IS NULL
                """,
                reversedAt.toString(), employeeId.toString(), reason, checkInId.toString()) > 0;
    }

    @Override
    public boolean existsActiveForGuestSince(UUID guestId, Instant fromInclusive) {
        var count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM guest_check_in
                        WHERE guest_id = ? AND reversed_at IS NULL AND checked_in_at >= ?
                        """,
                Long.class,
                guestId.toString(),
                fromInclusive.toString());
        return count != null && count > 0;
    }

    @Override
    public long countActive(Instant fromInclusive, Instant toExclusive) {
        var count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM guest_check_in
                        WHERE reversed_at IS NULL AND checked_in_at >= ? AND checked_in_at < ?
                        """,
                Long.class,
                fromInclusive.toString(),
                toExclusive.toString());
        return count == null ? 0 : count;
    }

    private static GuestCheckIn mapCheckIn(ResultSet resultSet, int rowNumber) throws SQLException {
        var reversedAtValue = resultSet.getString("reversed_at");
        var reversedByIdValue = resultSet.getString("reversed_by_employee_id");
        return new GuestCheckIn(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("guest_id")),
                UUID.fromString(resultSet.getString("guest_pass_id")),
                resultSet.getString("pass_name"),
                Instant.parse(resultSet.getString("checked_in_at")),
                UUID.fromString(resultSet.getString("checked_in_by_employee_id")),
                resultSet.getString("checked_in_by_employee_name"),
                resultSet.getBoolean("consumed_entry"),
                reversedAtValue == null ? null : Instant.parse(reversedAtValue),
                reversedByIdValue == null ? null : UUID.fromString(reversedByIdValue),
                resultSet.getString("reversed_by_employee_name"),
                resultSet.getString("reversal_reason"));
    }
}
