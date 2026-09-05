package com.wardanger.excalibur.solarium.account.application;

import com.wardanger.excalibur.solarium.account.domain.SolariumAccount;
import com.wardanger.excalibur.solarium.account.domain.SolariumProduct;
import com.wardanger.excalibur.solarium.account.domain.SolariumTransaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SolariumRepository {
    List<SolariumProduct> findProducts(boolean includeInactive);
    SolariumProduct findProduct(UUID id);
    SolariumProduct updateProduct(UUID id, String name, long price, boolean active, Instant updatedAt);
    int lockOrCreateAccount(UUID guestId, String guestName, Instant now);
    void updateBalance(UUID guestId, String guestName, int remainingMinutes, Instant now);
    void appendTransaction(UUID id, UUID guestId, SolariumTransaction.Type type, int minutesDelta, int balanceAfter,
            UUID productId, String productName, Integer quantity, Long pricePaid, String paymentMethod,
            Instant occurredAt, UUID employeeId, String employeeName);
    SolariumAccount getAccount(UUID guestId, String guestName);
}