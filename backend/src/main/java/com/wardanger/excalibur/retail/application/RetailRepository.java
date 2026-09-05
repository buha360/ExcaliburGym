package com.wardanger.excalibur.retail.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.retail.domain.BalanceTransaction;
import com.wardanger.excalibur.retail.domain.RetailProduct;
import com.wardanger.excalibur.retail.domain.RetailSale;

public interface RetailRepository {

    List<RetailProduct> findActiveProducts();

    List<RetailProduct> findAllProducts();

    Optional<RetailProduct> findProduct(UUID productId);

    Optional<RetailProduct> findActiveProduct(UUID productId);

    void insertProduct(RetailProduct product);

    void updateProduct(RetailProduct product);

    boolean deactivateProduct(UUID productId, Instant updatedAt);

    void ensureBalanceAccount(UUID guestId, Instant updatedAt);

    long balance(UUID guestId);

    void addBalance(UUID guestId, long amount, Instant updatedAt);

    boolean subtractBalance(UUID guestId, long amount, Instant updatedAt);

    void insertBalanceTransaction(BalanceTransaction transaction);

    List<BalanceTransaction> findBalanceTransactions(UUID guestId);

    void insertSale(RetailSale sale);

    RevenueTotals sumRevenue(Instant fromInclusive, Instant toExclusive);

    DepositTotals sumDeposits(Instant fromInclusive, Instant toExclusive);

    record RevenueTotals(long cash, long bankCard, long prepaidBalance) {
        public long total() {
            return cash + bankCard;
        }
    }

    record DepositTotals(long cash, long bankCard) {
        public long total() {
            return cash + bankCard;
        }
    }
}
