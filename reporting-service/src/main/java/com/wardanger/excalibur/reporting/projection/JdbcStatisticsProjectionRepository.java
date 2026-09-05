package com.wardanger.excalibur.reporting.projection;

import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcStatisticsProjectionRepository implements StatisticsProjectionRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean beginEvent(String eventId, String eventType, Instant occurredAt, Instant processedAt) {
        return jdbcTemplate.update("""
                INSERT INTO processed_event (event_id, event_type, occurred_at, processed_at)
                VALUES (?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType, Timestamp.from(occurredAt), Timestamp.from(processedAt)) == 1;
    }

    @Override
    public void appendFact(String eventId, String eventType, Instant occurredAt, StatisticsDelta delta) {
        jdbcTemplate.update("""
                INSERT INTO statistics_fact
                    (event_id, event_type, occurred_at, visits_delta, new_guests_delta, sold_passes_delta,
                     sauna_reservations_delta, cash_revenue_delta, bank_card_revenue_delta, prepaid_balance_revenue_delta)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, eventId, eventType, Timestamp.from(occurredAt), delta.visits(), delta.newGuests(),
                delta.soldPasses(), delta.saunaReservations(), delta.cashRevenue(),
                delta.bankCardRevenue(), delta.prepaidBalanceRevenue());
    }

    @Override
    public StatisticsTotals summarize(Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(visits_delta), 0) AS visits,
                       COALESCE(SUM(new_guests_delta), 0) AS new_guests,
                       COALESCE(SUM(sold_passes_delta), 0) AS sold_passes,
                       COALESCE(SUM(sauna_reservations_delta), 0) AS sauna_reservations,
                       COALESCE(SUM(cash_revenue_delta), 0) AS cash_revenue,
                       COALESCE(SUM(bank_card_revenue_delta), 0) AS bank_card_revenue,
                       COALESCE(SUM(prepaid_balance_revenue_delta), 0) AS prepaid_balance_revenue
                FROM statistics_fact WHERE occurred_at >= ? AND occurred_at < ?
                """, (rs, row) -> new StatisticsTotals(
                        rs.getLong("visits"), rs.getLong("new_guests"), rs.getLong("sold_passes"),
                        rs.getLong("sauna_reservations"), rs.getLong("cash_revenue"),
                        rs.getLong("bank_card_revenue"), rs.getLong("prepaid_balance_revenue")),
                Timestamp.from(fromInclusive), Timestamp.from(toExclusive));
    }
}
