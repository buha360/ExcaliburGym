package com.wardanger.excalibur.pass.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.pass.domain.GuestPass;

public interface GuestPassRepository {

    void insert(GuestPass guestPass);

    List<GuestPass> findByGuestId(UUID guestId);

    boolean consumeEntry(UUID passId);

    boolean restoreEntry(UUID passId);

    long countSoldPasses(Instant fromInclusive, Instant toExclusive);

    RevenueTotals sumRevenue(Instant fromInclusive, Instant toExclusive);

    record RevenueTotals(long cash, long bankCard, long prepaidBalance) {
        public long total() {
            return cash + bankCard + prepaidBalance;
        }
    }
}
