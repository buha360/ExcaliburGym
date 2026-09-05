package com.wardanger.excalibur.solarium.account.infrastructure;

import com.wardanger.excalibur.solarium.account.application.SolariumRepository;
import com.wardanger.excalibur.solarium.account.domain.SolariumAccount;
import com.wardanger.excalibur.solarium.account.domain.SolariumProduct;
import com.wardanger.excalibur.solarium.account.domain.SolariumTransaction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcSolariumRepository implements SolariumRepository {
    private final JdbcTemplate jdbc;

    @Override public List<SolariumProduct> findProducts(boolean includeInactive) {
        return jdbc.query("SELECT id, code, name, minutes_per_unit, default_price, active, updated_at FROM solarium_product "
                + (includeInactive ? "" : "WHERE active = TRUE ") + "ORDER BY minutes_per_unit, id", JdbcSolariumRepository::mapProduct);
    }
    @Override public SolariumProduct findProduct(UUID id) {
        try { return jdbc.queryForObject("SELECT id, code, name, minutes_per_unit, default_price, active, updated_at FROM solarium_product WHERE id = ?", JdbcSolariumRepository::mapProduct, id); }
        catch (EmptyResultDataAccessException e) { return null; }
    }
    @Override public SolariumProduct updateProduct(UUID id, String name, long price, boolean active, Instant updatedAt) {
        if (jdbc.update("UPDATE solarium_product SET name = ?, default_price = ?, active = ?, updated_at = ? WHERE id = ?", name, price, active, Timestamp.from(updatedAt), id) != 1) return null;
        return findProduct(id);
    }
    @Override public int lockOrCreateAccount(UUID guestId, String guestName, Instant now) {
        jdbc.update("INSERT INTO solarium_account (guest_id, guest_name, remaining_minutes, updated_at) VALUES (?, ?, 0, ?) ON CONFLICT (guest_id) DO NOTHING", guestId, guestName, Timestamp.from(now));
        return jdbc.queryForObject("SELECT remaining_minutes FROM solarium_account WHERE guest_id = ? FOR UPDATE", Integer.class, guestId);
    }
    @Override public void updateBalance(UUID guestId, String guestName, int remainingMinutes, Instant now) {
        jdbc.update("UPDATE solarium_account SET guest_name = ?, remaining_minutes = ?, updated_at = ? WHERE guest_id = ?", guestName, remainingMinutes, Timestamp.from(now), guestId);
    }
    @Override public void appendTransaction(UUID id, UUID guestId, SolariumTransaction.Type type, int minutesDelta, int balanceAfter, UUID productId, String productName, Integer quantity, Long pricePaid, String paymentMethod, Instant occurredAt, UUID employeeId, String employeeName) {
        jdbc.update("INSERT INTO solarium_transaction (id, guest_id, type, minutes_delta, balance_after, product_id, product_name, quantity, price_paid, payment_method, occurred_at, employee_id, employee_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, guestId, type.name(), minutesDelta, balanceAfter, productId, productName, quantity, pricePaid, paymentMethod, Timestamp.from(occurredAt), employeeId, employeeName);
    }
    @Override public SolariumAccount getAccount(UUID guestId, String guestName) {
        Integer balance;
        try { balance = jdbc.queryForObject("SELECT remaining_minutes FROM solarium_account WHERE guest_id = ?", Integer.class, guestId); }
        catch (EmptyResultDataAccessException e) { balance = 0; }
        var transactions = jdbc.query("SELECT id, type, minutes_delta, balance_after, product_name, quantity, price_paid, payment_method, occurred_at, employee_name FROM solarium_transaction WHERE guest_id = ? ORDER BY occurred_at DESC, id DESC LIMIT 100", JdbcSolariumRepository::mapTransaction, guestId);
        return new SolariumAccount(guestId, guestName, balance, transactions);
    }
    private static SolariumProduct mapProduct(ResultSet rs, int row) throws SQLException { return new SolariumProduct(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getInt("minutes_per_unit"), rs.getLong("default_price"), rs.getBoolean("active"), rs.getTimestamp("updated_at").toInstant()); }
    private static SolariumTransaction mapTransaction(ResultSet rs, int row) throws SQLException { return new SolariumTransaction(rs.getObject("id", UUID.class), SolariumTransaction.Type.valueOf(rs.getString("type")), rs.getInt("minutes_delta"), rs.getInt("balance_after"), rs.getString("product_name"), (Integer) rs.getObject("quantity"), (Long) rs.getObject("price_paid"), rs.getString("payment_method"), rs.getTimestamp("occurred_at").toInstant(), rs.getString("employee_name")); }
}