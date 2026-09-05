package com.wardanger.excalibur.reporting.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.reporting.generated.api.InternalStatisticsApi;
import com.wardanger.excalibur.reporting.generated.model.StatisticsPeriod;
import com.wardanger.excalibur.reporting.generated.model.StatisticsSummary;
import com.wardanger.excalibur.reporting.projection.StatisticsQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalStatisticsApiController implements InternalStatisticsApi {

    private final StatisticsQueryService statistics;
    private final Clock clock;
    private final byte[] expectedApiKey;

    public InternalStatisticsApiController(
            StatisticsQueryService statistics,
            Clock clock,
            @Value("${excalibur.internal-api-key}") String expectedApiKey) {
        this.statistics = statistics;
        this.clock = clock;
        this.expectedApiKey = expectedApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ResponseEntity<StatisticsSummary> getInternalStatisticsSummary(
            String internalApiKey,
            StatisticsPeriod period) {
        if (internalApiKey == null || !MessageDigest.isEqual(
                expectedApiKey,
                internalApiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var snapshot = statistics.current(StatisticsQueryService.Period.valueOf(period.name()));
        var totals = snapshot.totals();
        return ResponseEntity.ok(new StatisticsSummary(
                period,
                OffsetDateTime.ofInstant(snapshot.from(), clock.getZone()),
                OffsetDateTime.ofInstant(snapshot.to(), clock.getZone()),
                totals.visits(),
                totals.newGuests(),
                totals.soldPasses(),
                totals.saunaReservations(),
                totals.totalRevenue(),
                totals.cashRevenue(),
                totals.bankCardRevenue(),
                totals.prepaidBalanceRevenue()));
    }
}