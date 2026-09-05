package com.wardanger.excalibur.statistics.api;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.generated.api.StatisticsApi;
import com.wardanger.excalibur.generated.model.StatisticsPeriod;
import com.wardanger.excalibur.generated.model.StatisticsSummary;
import com.wardanger.excalibur.statistics.application.StatisticsQuery;
import com.wardanger.excalibur.statistics.application.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatisticsApiController implements StatisticsApi {

    private final StatisticsQuery statistics;
    private final Clock clock;

    @Override
    public ResponseEntity<StatisticsSummary> getStatisticsSummary(StatisticsPeriod period) {
        var snapshot = statistics.current(StatisticsService.Period.valueOf(period.name()));
        return ResponseEntity.ok(new StatisticsSummary(
                period,
                OffsetDateTime.ofInstant(snapshot.from(), clock.getZone()),
                OffsetDateTime.ofInstant(snapshot.to(), clock.getZone()),
                snapshot.visits(),
                snapshot.newGuests(),
                snapshot.soldPasses(),
                snapshot.saunaReservations(),
                snapshot.totalRevenue(),
                snapshot.cashRevenue(),
                snapshot.bankCardRevenue(),
                snapshot.prepaidBalanceRevenue()));
    }
}
