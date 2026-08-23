package com.wardanger.excalibur.statistics.application;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.wardanger.excalibur.guest.application.GuestRepository;
import com.wardanger.excalibur.pass.application.GuestPassRepository;
import com.wardanger.excalibur.visit.application.GuestCheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final GuestRepository guests;
    private final GuestPassRepository passes;
    private final GuestCheckInRepository checkIns;
    private final Clock clock;

    public StatisticsSnapshot current(Period period) {
        var range = currentRange(period);
        var revenue = passes.sumRevenue(range.from(), range.to());
        return new StatisticsSnapshot(
                period,
                range.from(),
                range.to(),
                checkIns.countActive(range.from(), range.to()),
                guests.countNewGuests(range.from(), range.to()),
                passes.countSoldPasses(range.from(), range.to()),
                revenue.total(),
                revenue.cash(),
                revenue.bankCard(),
                revenue.prepaidBalance());
    }

    private TimeRange currentRange(Period period) {
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
        ZonedDateTime start = startDate.atStartOfDay(clock.getZone());
        ZonedDateTime end = endDate.atStartOfDay(clock.getZone());
        return new TimeRange(start.toInstant(), end.toInstant());
    }

    public enum Period {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public record StatisticsSnapshot(
            Period period,
            Instant from,
            Instant to,
            long visits,
            long newGuests,
            long soldPasses,
            long totalRevenue,
            long cashRevenue,
            long bankCardRevenue,
            long prepaidBalanceRevenue) {
    }

    private record TimeRange(Instant from, Instant to) {
    }
}
