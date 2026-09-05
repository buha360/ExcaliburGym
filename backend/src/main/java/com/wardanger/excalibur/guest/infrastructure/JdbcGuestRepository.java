package com.wardanger.excalibur.guest.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.guest.application.GuestRepository;
import com.wardanger.excalibur.guest.domain.Guest;
import com.wardanger.excalibur.guest.domain.GuestRegistrationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.date;
import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.instant;
import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.timestamp;

@Repository
@RequiredArgsConstructor
public class JdbcGuestRepository implements GuestRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(Guest guest, String normalizedName) {
        jdbcTemplate.update("""
                INSERT INTO guest
                    (id, full_name, full_name_search, registration_type, created_at, created_by_employee_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                guest.id().toString(),
                guest.fullName(),
                normalizedName,
                guest.registrationType().name(),
                timestamp(guest.createdAt()),
                guest.createdByEmployeeId().toString());
    }

    @Override
    public Optional<Guest> findById(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM guest WHERE id = ?",
                JdbcGuestRepository::mapGuest,
                id.toString()).stream().findFirst();
    }

    @Override
    public List<Guest> findQuickSearch(String normalizedQuery, LocalDate today) {
        if (normalizedQuery.isBlank()) {
            return jdbcTemplate.query("""
                            SELECT g.*,
                                   (SELECT MIN(gp.valid_until)
                                    FROM guest_pass gp
                                    WHERE gp.guest_id = g.id
                                      AND gp.invalidated_at IS NULL
                                      AND gp.valid_from <= ? AND gp.valid_until >= ?
                                      AND (gp.remaining_entries IS NULL OR gp.remaining_entries > 0)) AS active_expiry
                            FROM guest g
                            ORDER BY active_expiry NULLS LAST, g.full_name_search
                            LIMIT 6
                            """,
                    JdbcGuestRepository::mapGuest,
                    date(today), date(today));
        }
        return jdbcTemplate.query("""
                        SELECT g.*,
                               (SELECT MIN(gp.valid_until)
                                FROM guest_pass gp
                                WHERE gp.guest_id = g.id
                                  AND gp.invalidated_at IS NULL
                                  AND gp.valid_from <= ? AND gp.valid_until >= ?
                                  AND (gp.remaining_entries IS NULL OR gp.remaining_entries > 0)) AS active_expiry
                        FROM guest g
                        WHERE g.full_name_search LIKE ?
                        ORDER BY active_expiry NULLS LAST,
                                 CASE WHEN g.full_name_search = ? THEN 0 ELSE 1 END,
                                 g.full_name_search
                        LIMIT 6
                        """,
                JdbcGuestRepository::mapGuest,
                date(today),
                date(today),
                "%" + normalizedQuery + "%",
                normalizedQuery);
    }

    @Override
    public long countNewGuests(Instant fromInclusive, Instant toExclusive) {
        var result = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM guest
                        WHERE registration_type = 'NEW'
                          AND created_at >= ?
                          AND created_at < ?
                        """,
                Long.class,
                timestamp(fromInclusive),
                timestamp(toExclusive));
        return result == null ? 0 : result;
    }

    private static Guest mapGuest(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Guest(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("full_name"),
                GuestRegistrationType.valueOf(resultSet.getString("registration_type")),
                instant(resultSet, "created_at"),
                UUID.fromString(resultSet.getString("created_by_employee_id")));
    }
}
