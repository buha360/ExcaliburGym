package com.wardanger.excalibur.system.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.wardanger.excalibur.generated.model.SystemStatus;
import org.junit.jupiter.api.Test;

class SystemApiControllerTest {

    @Test
    void returnsApplicationStatusUsingTheConfiguredClock() {
        var zone = ZoneId.of("Europe/Budapest");
        var instant = Instant.parse("2026-08-20T18:30:00Z");
        var controller = new SystemApiController(Clock.fixed(instant, zone));

        var response = controller.getSystemStatus();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getApplication()).isEqualTo("excalibur");
        assertThat(response.getBody().getStatus()).isEqualTo(SystemStatus.StatusEnum.UP);
        assertThat(response.getBody().getTimestamp()).isEqualTo(OffsetDateTime.ofInstant(instant, zone));
    }
}
