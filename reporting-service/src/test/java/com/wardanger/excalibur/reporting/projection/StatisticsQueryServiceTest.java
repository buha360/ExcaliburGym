package com.wardanger.excalibur.reporting.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class StatisticsQueryServiceTest {

    @Test
    void weeklyRangeUsesBudapestMondayBoundary() {
        var repository = mock(StatisticsProjectionRepository.class);
        var clock = Clock.fixed(Instant.parse("2026-08-20T10:15:30Z"), ZoneId.of("Europe/Budapest"));
        var from = Instant.parse("2026-08-16T22:00:00Z");
        var to = Instant.parse("2026-08-23T22:00:00Z");
        when(repository.summarize(from, to)).thenReturn(new StatisticsProjectionRepository.StatisticsTotals(
                13, 4, 7, 2, 31_000, 44_500, 9_000));

        var result = new StatisticsQueryService(repository, clock)
                .current(StatisticsQueryService.Period.WEEK);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.totals().totalRevenue()).isEqualTo(75_500);
    }
}
