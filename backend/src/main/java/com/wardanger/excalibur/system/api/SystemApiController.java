package com.wardanger.excalibur.system.api;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.generated.api.SystemApi;
import com.wardanger.excalibur.generated.model.SystemStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemApiController implements SystemApi {

    private final Clock clock;

    public SystemApiController(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ResponseEntity<SystemStatus> getSystemStatus() {
        var status = new SystemStatus(
                "excalibur",
                SystemStatus.StatusEnum.UP,
                OffsetDateTime.now(clock));

        return ResponseEntity.ok(status);
    }
}
