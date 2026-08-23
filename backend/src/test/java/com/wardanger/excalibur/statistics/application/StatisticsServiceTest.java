package com.wardanger.excalibur.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.wardanger.excalibur.guest.application.GuestRepository;
import com.wardanger.excalibur.pass.application.GuestPassRepository;
import com.wardanger.excalibur.visit.application.GuestCheckInRepository;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {

    private final GuestRepository guests = mock(GuestRepository.class);
    private final GuestPassRepository passes = mock(GuestPassRepository.class);
    private final GuestCheckInRepository checkIns = mock(GuestCheckInRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-20T10:15:30Z"),
            ZoneId.of("Europe/Budapest"));
    private final StatisticsService service = new StatisticsService(guests, passes, checkIns, clock);

    @Test
    void weeklyStatisticsUseBudapestMondayBoundaryAndSplitRevenueByPaymentMethod() {
        var from = Instant.parse("2026-08-16T22:00:00Z");
        var to = Instant.parse("2026-08-23T22:00:00Z");
        when(guests.countNewGuests(from, to)).thenReturn(4L);
        when(passes.countSoldPasses(from, to)).thenReturn(7L);
        when(checkIns.countActive(from, to)).thenReturn(13L);
        when(passes.sumRevenue(from, to)).thenReturn(new GuestPassRepository.RevenueTotals(
                31_000,
                44_500,
                0));

        var result = service.current(StatisticsService.Period.WEEK);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.newGuests()).isEqualTo(4);
        assertThat(result.soldPasses()).isEqualTo(7);
        assertThat(result.visits()).isEqualTo(13);
        assertThat(result.cashRevenue()).isEqualTo(31_000);
        assertThat(result.bankCardRevenue()).isEqualTo(44_500);
        assertThat(result.totalRevenue()).isEqualTo(75_500);
    }
}
