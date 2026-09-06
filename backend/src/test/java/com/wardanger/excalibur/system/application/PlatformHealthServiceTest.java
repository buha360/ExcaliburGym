package com.wardanger.excalibur.system.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.wardanger.excalibur.generated.model.PlatformHealth;
import com.wardanger.excalibur.generated.model.ServiceHealth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlatformHealthServiceTest {

    private final DownstreamHealthProbe probe = mock(DownstreamHealthProbe.class);
    private final PlatformHealthService service = new PlatformHealthService(
            Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneId.of("Europe/Budapest")),
            probe);

    @BeforeEach
    void configureTargets() {
        ReflectionTestUtils.setField(service, "reportingMode", "remote");
        ReflectionTestUtils.setField(service, "reportingBaseUrl", "http://reporting");
        ReflectionTestUtils.setField(service, "saunaBaseUrl", "http://sauna");
        ReflectionTestUtils.setField(service, "solariumBaseUrl", "http://solarium");
    }

    @Test
    void reportsUpWhenEveryRequiredServiceIsAvailable() {
        when(probe.probe(anyString())).thenReturn(
                new DownstreamHealthProbe.ProbeResult(true, 12L, "Elérhető."));

        var health = service.getHealth();

        assertThat(health.getStatus()).isEqualTo(PlatformHealth.StatusEnum.UP);
        assertThat(health.getServices()).hasSize(4);
        assertThat(health.getServices()).allMatch(item -> item.getStatus() == ServiceHealth.StatusEnum.UP);
    }

    @Test
    void reportsDegradedWhenARequiredServiceIsUnavailable() {
        when(probe.probe(anyString())).thenAnswer(invocation -> {
            var url = invocation.getArgument(0, String.class);
            return new DownstreamHealthProbe.ProbeResult(!url.contains("sauna"), 23L, "Ellenőrzés.");
        });

        var health = service.getHealth();

        assertThat(health.getStatus()).isEqualTo(PlatformHealth.StatusEnum.DEGRADED);
        assertThat(health.getServices()).anyMatch(item ->
                item.getId() == ServiceHealth.IdEnum.SAUNA
                        && item.getStatus() == ServiceHealth.StatusEnum.DOWN);
    }

    @Test
    void marksReportingAsNotRequiredWhenLocalStatisticsAreActive() {
        ReflectionTestUtils.setField(service, "reportingMode", "local");
        when(probe.probe(anyString())).thenAnswer(invocation -> {
            var url = invocation.getArgument(0, String.class);
            return new DownstreamHealthProbe.ProbeResult(!url.contains("reporting"), 8L, "Ellenőrzés.");
        });

        var health = service.getHealth();

        assertThat(health.getStatus()).isEqualTo(PlatformHealth.StatusEnum.UP);
        assertThat(health.getServices()).anyMatch(item ->
                item.getId() == ServiceHealth.IdEnum.REPORTING
                        && item.getStatus() == ServiceHealth.StatusEnum.NOT_REQUIRED);
    }
}
