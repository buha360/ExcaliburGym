package com.wardanger.excalibur.retail.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.retail.application.RetailRepository;
import com.wardanger.excalibur.retail.domain.BalanceTransaction;
import com.wardanger.excalibur.retail.domain.RetailProduct;
import com.wardanger.excalibur.retail.domain.RetailSale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.instant;
import static com.wardanger.excalibur.shared.persistence.JdbcTemporalSupport.timestamp;

@Repository
@RequiredArgsConstructor
public class JdbcRetailRepository implements RetailRepository {

    private final JdbcTemplate jdbc;

    @Override
    public List<RetailProduct> findActiveProducts() {
        return jdbc.query("SELECT * FROM retail_product WHERE active = TRUE ORDER BY LOWER(name)", JdbcRetailRepository::mapProduct);
    }

    @Override
    public List<RetailProduct> findAllProducts() {
        return jdbc.query("SELECT * FROM retail_product ORDER BY active DESC, LOWER(name)", JdbcRetailRepository::mapProduct);
    }

    @Override
    public Optional<RetailProduct> findProduct(UUID productId) {
        return jdbc.query("SELECT * FROM retail_product WHERE id = ?", JdbcRetailRepository::mapProduct, productId.toString())
                .stream().findFirst();
    }

    @Override
    public Optional<RetailProduct> findActiveProduct(UUID productId) {
        return jdbc.query("SELECT * FROM retail_product WHERE id = ? AND active = TRUE", JdbcRetailRepository::mapProduct, productId.toString())
                .stream().findFirst();
    }

    @Override
    public void insertProduct(RetailProduct product) {
        jdbc.update("""
                INSERT INTO retail_product (id, code, name, default_price, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, product.id().toString(), product.code(), product.name(), product.defaultPrice(), product.active(),
                timestamp(product.createdAt()), timestamp(product.updatedAt()));
    }

    @Override
    public void updateProduct(RetailProduct product) {
        jdbc.update("UPDATE retail_product SET name = ?, default_price = ?, active = ?, updated_at = ? WHERE id = ?",
                product.name(), product.defaultPrice(), product.active(), timestamp(product.updatedAt()), product.id().toString());
    }

    @Override
    public boolean deactivateProduct(UUID productId, Instant updatedAt) {
        return jdbc.update("UPDATE retail_product SET active = FALSE, updated_at = ? WHERE id = ? AND active = TRUE",
                timestamp(updatedAt), productId.toString()) > 0;
    }

    @Override
    public void ensureBalanceAccount(UUID guestId, Instant updatedAt) {
        jdbc.update("""
                INSERT INTO guest_balance_account (guest_id, balance, updated_at)
                VALUES (?, 0, ?)
                ON CONFLICT (guest_id) DO NOTHING
                """, guestId.toString(), timestamp(updatedAt));
    }

    @Override
    public long balance(UUID guestId) {
        var value = jdbc.queryForObject("SELECT COALESCE((SELECT balance FROM guest_balance_account WHERE guest_id = ?), 0)", Long.class, guestId.toString());
        return value == null ? 0 : value;
    }

    @Override
    public void addBalance(UUID guestId, long amount, Instant updatedAt) {
        jdbc.update("UPDATE guest_balance_account SET balance = balance + ?, updated_at = ? WHERE guest_id = ?",
                amount, timestamp(updatedAt), guestId.toString());
    }

    @Override
    public boolean subtractBalance(UUID guestId, long amount, Instant updatedAt) {
        return jdbc.update("""
                UPDATE guest_balance_account
                SET balance = balance - ?, updated_at = ?
                WHERE guest_id = ? AND balance >= ?
                """, amount, timestamp(updatedAt), guestId.toString(), amount) == 1;
    }

    @Override
    public void insertBalanceTransaction(BalanceTransaction transaction) {
        jdbc.update("""
                INSERT INTO guest_balance_transaction
                    (id, guest_id, guest_name, transaction_type, amount_delta, balance_after, payment_method,
                     reference_id, employee_id, employee_name, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                transaction.id().toString(), transaction.guestId().toString(), transaction.guestName(),
                transaction.type().name(), transaction.amountDelta(), transaction.balanceAfter(),
                transaction.paymentMethod() == null ? null : transaction.paymentMethod().name(),
                transaction.referenceId() == null ? null : transaction.referenceId().toString(),
                transaction.employeeId().toString(), transaction.employeeName(), timestamp(transaction.occurredAt()));
    }

    @Override
    public List<BalanceTransaction> findBalanceTransactions(UUID guestId) {
        return jdbc.query("""
                SELECT * FROM guest_balance_transaction
                WHERE guest_id = ? ORDER BY occurred_at DESC, id DESC
                """, JdbcRetailRepository::mapTransaction, guestId.toString());
    }

    @Override
    public void insertSale(RetailSale sale) {
        jdbc.update("""
                INSERT INTO retail_sale
                    (id, guest_id, guest_name, total_price, payment_method, employee_id, employee_name, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, sale.id().toString(), sale.guestId() == null ? null : sale.guestId().toString(), sale.guestName(),
                sale.totalPrice(), sale.paymentMethod().name(), sale.employeeId().toString(), sale.employeeName(),
                timestamp(sale.occurredAt()));
        for (var item : sale.items()) {
            jdbc.update("""
                    INSERT INTO retail_sale_item
                        (id, sale_id, product_id, product_name, unit_price, quantity, line_total)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, item.id().toString(), sale.id().toString(), item.productId().toString(), item.productName(),
                    item.unitPrice(), item.quantity(), item.lineTotal());
        }
    }

    @Override
    public RevenueTotals sumRevenue(Instant fromInclusive, Instant toExclusive) {
        return jdbc.query("""
                SELECT
                    COALESCE(SUM(CASE WHEN payment_method = 'CASH' THEN total_price ELSE 0 END), 0) AS cash,
                    COALESCE(SUM(CASE WHEN payment_method = 'BANK_CARD' THEN total_price ELSE 0 END), 0) AS bank_card,
                    COALESCE(SUM(CASE WHEN payment_method = 'PREPAID_BALANCE' THEN total_price ELSE 0 END), 0) AS prepaid
                FROM retail_sale
                WHERE occurred_at >= ? AND occurred_at < ?
                """, rs -> rs.next()
                        ? new RevenueTotals(rs.getLong("cash"), rs.getLong("bank_card"), rs.getLong("prepaid"))
                        : new RevenueTotals(0, 0, 0),
                timestamp(fromInclusive), timestamp(toExclusive));
    }

    @Override
    public DepositTotals sumDeposits(Instant fromInclusive, Instant toExclusive) {
        return jdbc.query("""
                SELECT
                    COALESCE(SUM(CASE WHEN payment_method = 'CASH' THEN amount_delta ELSE 0 END), 0) AS cash,
                    COALESCE(SUM(CASE WHEN payment_method = 'BANK_CARD' THEN amount_delta ELSE 0 END), 0) AS bank_card
                FROM guest_balance_transaction
                WHERE transaction_type = 'DEPOSIT' AND occurred_at >= ? AND occurred_at < ?
                """, rs -> rs.next()
                        ? new DepositTotals(rs.getLong("cash"), rs.getLong("bank_card"))
                        : new DepositTotals(0, 0),
                timestamp(fromInclusive), timestamp(toExclusive));
    }
    private static RetailProduct mapProduct(ResultSet rs, int row) throws SQLException {
        return new RetailProduct(UUID.fromString(rs.getString("id")), rs.getString("code"), rs.getString("name"),
                rs.getLong("default_price"), rs.getBoolean("active"), instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private static BalanceTransaction mapTransaction(ResultSet rs, int row) throws SQLException {
        var payment = rs.getString("payment_method");
        var reference = rs.getString("reference_id");
        return new BalanceTransaction(
                UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("guest_id")), rs.getString("guest_name"),
                BalanceTransaction.Type.valueOf(rs.getString("transaction_type")), rs.getLong("amount_delta"),
                rs.getLong("balance_after"), payment == null ? null : PaymentMethod.valueOf(payment),
                reference == null ? null : UUID.fromString(reference), UUID.fromString(rs.getString("employee_id")),
                rs.getString("employee_name"), instant(rs, "occurred_at"));
    }
}