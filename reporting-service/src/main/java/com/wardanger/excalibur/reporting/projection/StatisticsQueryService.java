package com.wardanger.excalibur.reporting.projection;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsQueryService {

    private final StatisticsProjectionRepository projections;
    private final Clock clock;

    public Snapshot current(Period period) {
        var today = LocalDate.now(clock);
        var startDate = switch (period) {
            case DAY -> today;
            case WEEK -> today.with(previousOrSame(MONDAY));
            case MONTH -> today.withDayOfMonth(1);
            case YEAR -> today.withDayOfYear(1);
        };
        var endDate = switch (period) {
            case DAY -> startDate.plusDays(1);
            case WEEK -> startDate.plusWeeks(1);
            case MONTH -> startDate.plusMonths(1);
            case YEAR -> startDate.plusYears(1);
        };
        var from = startDate.atStartOfDay(clock.getZone()).toInstant();
        var to = endDate.atStartOfDay(clock.getZone()).toInstant();
        return new Snapshot(period, from, to, projections.summarize(from, to));
    }

    public enum Period {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public record Snapshot(
            Period period,
            Instant from,
            Instant to,
            StatisticsProjectionRepository.StatisticsTotals totals) {
    }
}
