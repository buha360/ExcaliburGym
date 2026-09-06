package com.wardanger.excalibur.system.api;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.generated.api.SystemApi;
import com.wardanger.excalibur.generated.model.PlatformHealth;
import com.wardanger.excalibur.generated.model.SystemStatus;
import com.wardanger.excalibur.system.application.PlatformHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SystemApiController implements SystemApi {

    private final Clock clock;
    private final PlatformHealthService platformHealth;

    @Override
    public ResponseEntity<SystemStatus> getSystemStatus() {
        var status = new SystemStatus(
                "excalibur",
                SystemStatus.StatusEnum.UP,
                OffsetDateTime.now(clock));

        return ResponseEntity.ok(status);
    }

    @Override
    public ResponseEntity<PlatformHealth> getPlatformHealth() {
        return ResponseEntity.ok(platformHealth.getHealth());
    }
}
