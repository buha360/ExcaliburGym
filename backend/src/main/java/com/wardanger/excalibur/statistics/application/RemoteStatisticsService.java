package com.wardanger.excalibur.statistics.application;

import java.time.Instant;

import com.wardanger.excalibur.shared.error.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@ConditionalOnProperty(name = "excalibur.reporting.mode", havingValue = "remote")
public class RemoteStatisticsService implements StatisticsQuery {

    private final RestClient reportingRestClient;

    public RemoteStatisticsService(@Qualifier("reportingRestClient") RestClient reportingRestClient) {
        this.reportingRestClient = reportingRestClient;
    }

    @Override
    public StatisticsService.StatisticsSnapshot current(StatisticsService.Period period) {
        try {
            var response = reportingRestClient.get()
                    .uri(builder -> builder
                            .path("/internal/v1/statistics/summary")
                            .queryParam("period", period.name())
                            .build())
                    .retrieve()
                    .body(RemoteStatisticsSummary.class);
            if (response == null) {
                throw unavailable();
            }
            return new StatisticsService.StatisticsSnapshot(
                    period,
                    response.from(),
                    response.to(),
                    response.visits(),
                    response.newGuests(),
                    response.soldPasses(),
                    response.saunaReservations(),
                    response.totalRevenue(),
                    response.cashRevenue(),
                    response.bankCardRevenue(),
                    response.prepaidBalanceRevenue());
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private static ApiException unavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "REPORTING_SERVICE_UNAVAILABLE",
                "A statisztikai szolgáltatás átmenetileg nem érhető el.");
    }

    private record RemoteStatisticsSummary(
            String period,
            Instant from,
            Instant to,
            long visits,
            long newGuests,
            long soldPasses,
            long saunaReservations,
            long totalRevenue,
            long cashRevenue,
            long bankCardRevenue,
            long prepaidBalanceRevenue) {
    }
}
