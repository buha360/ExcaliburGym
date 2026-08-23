package com.wardanger.excalibur.pass.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.pass.application.GuestPassRepository;
import com.wardanger.excalibur.pass.domain.GuestPass;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JdbcGuestPassRepository implements GuestPassRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(GuestPass guestPass) {
        jdbcTemplate.update("""
                INSERT INTO guest_pass
                    (id, guest_id, product_definition_id, product_name_snapshot, purchased_at,
                     valid_from, valid_until, total_entries, remaining_entries, price_paid,
                     payment_method, issued_by_employee_id, invalidated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                guestPass.id().toString(),
                guestPass.guestId().toString(),
                guestPass.productDefinitionId().toString(),
                guestPass.productName(),
                guestPass.purchasedAt().toString(),
                guestPass.validFrom().toString(),
                guestPass.validUntil().toString(),
                guestPass.totalEntries(),
                guestPass.remainingEntries(),
                guestPass.pricePaid(),
                guestPass.paymentMethod().name(),
                guestPass.issuedByEmployeeId().toString(),
                guestPass.invalidatedAt() == null ? null : guestPass.invalidatedAt().toString());
    }

    @Override
    public List<GuestPass> findByGuestId(UUID guestId) {
        return jdbcTemplate.query("""
                        SELECT gp.*, ea.display_name AS issued_by_employee_name
                        FROM guest_pass gp
                        JOIN employee_account ea ON ea.id = gp.issued_by_employee_id
                        WHERE gp.guest_id = ?
                        ORDER BY gp.purchased_at DESC
                        """,
                JdbcGuestPassRepository::mapGuestPass,
                guestId.toString());
    }

    @Override
    public boolean consumeEntry(UUID passId) {
        return jdbcTemplate.update("""
                UPDATE guest_pass SET remaining_entries = remaining_entries - 1
                WHERE id = ? AND remaining_entries IS NOT NULL AND remaining_entries > 0
                """, passId.toString()) > 0;
    }

    @Override
    public boolean restoreEntry(UUID passId) {
        return jdbcTemplate.update("""
                UPDATE guest_pass SET remaining_entries = remaining_entries + 1
                WHERE id = ? AND remaining_entries IS NOT NULL AND total_entries IS NOT NULL
                  AND remaining_entries < total_entries
                """, passId.toString()) > 0;
    }

    @Override
    public long countSoldPasses(Instant fromInclusive, Instant toExclusive) {
        var result = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM guest_pass
                        WHERE invalidated_at IS NULL
                          AND purchased_at >= ?
                          AND purchased_at < ?
                        """,
                Long.class,
                fromInclusive.toString(),
                toExclusive.toString());
        return result == null ? 0 : result;
    }

    @Override
    public RevenueTotals sumRevenue(Instant fromInclusive, Instant toExclusive) {
        var values = new EnumMap<PaymentMethod, Long>(PaymentMethod.class);
        jdbcTemplate.query("""
                        SELECT payment_method, COALESCE(SUM(price_paid), 0) AS revenue
                        FROM guest_pass
                        WHERE invalidated_at IS NULL
                          AND purchased_at >= ?
                          AND purchased_at < ?
                        GROUP BY payment_method
                        """,
                (RowCallbackHandler) resultSet -> values.put(
                        PaymentMethod.valueOf(resultSet.getString("payment_method")),
                        resultSet.getLong("revenue")),
                fromInclusive.toString(),
                toExclusive.toString());
        return new RevenueTotals(
                values.getOrDefault(PaymentMethod.CASH, 0L),
                values.getOrDefault(PaymentMethod.BANK_CARD, 0L),
                values.getOrDefault(PaymentMethod.PREPAID_BALANCE, 0L));
    }

    private static GuestPass mapGuestPass(ResultSet resultSet, int rowNumber) throws SQLException {
        var totalEntriesValue = resultSet.getObject("total_entries");
        var remainingEntriesValue = resultSet.getObject("remaining_entries");
        var invalidatedAtValue = resultSet.getString("invalidated_at");
        return new GuestPass(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("guest_id")),
                UUID.fromString(resultSet.getString("product_definition_id")),
                resultSet.getString("product_name_snapshot"),
                Instant.parse(resultSet.getString("purchased_at")),
                LocalDate.parse(resultSet.getString("valid_from")),
                LocalDate.parse(resultSet.getString("valid_until")),
                totalEntriesValue == null ? null : resultSet.getInt("total_entries"),
                remainingEntriesValue == null ? null : resultSet.getInt("remaining_entries"),
                resultSet.getLong("price_paid"),
                PaymentMethod.valueOf(resultSet.getString("payment_method")),
                UUID.fromString(resultSet.getString("issued_by_employee_id")),
                resultSet.getString("issued_by_employee_name"),
                invalidatedAtValue == null ? null : Instant.parse(invalidatedAtValue));
    }
}
