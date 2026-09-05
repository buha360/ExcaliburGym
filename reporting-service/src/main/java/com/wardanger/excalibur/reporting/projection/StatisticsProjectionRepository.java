package com.wardanger.excalibur.reporting.projection;

import java.time.Instant;

public interface StatisticsProjectionRepository {
    boolean beginEvent(String eventId, String eventType, Instant occurredAt, Instant processedAt);
    void appendFact(String eventId, String eventType, Instant occurredAt, StatisticsDelta delta);
    StatisticsTotals summarize(Instant fromInclusive, Instant toExclusive);

    record StatisticsDelta(long visits, long newGuests, long soldPasses, long saunaReservations,
            long cashRevenue, long bankCardRevenue, long prepaidBalanceRevenue) {
        public static StatisticsDelta zero() { return new StatisticsDelta(0, 0, 0, 0, 0, 0, 0); }
    }

    record StatisticsTotals(long visits, long newGuests, long soldPasses, long saunaReservations,
            long cashRevenue, long bankCardRevenue, long prepaidBalanceRevenue) {
        public long totalRevenue() { return cashRevenue + bankCardRevenue; }
    }
}
