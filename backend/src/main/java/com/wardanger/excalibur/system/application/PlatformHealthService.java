package com.wardanger.excalibur.system.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.wardanger.excalibur.generated.model.PlatformHealth;
import com.wardanger.excalibur.generated.model.ServiceHealth;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformHealthService {

    private final Clock clock;
    private final DownstreamHealthProbe probe;

    @Value("${excalibur.reporting.mode:local}")
    private String reportingMode;
    @Value("${excalibur.reporting.base-url:http://localhost:8081}")
    private String reportingBaseUrl;
    @Value("${excalibur.sauna.base-url}")
    private String saunaBaseUrl;
    @Value("${excalibur.solarium.base-url}")
    private String solariumBaseUrl;

    public PlatformHealth getHealth() {
        var checkedAt = OffsetDateTime.now(clock);
        var reporting = asyncHealth(ServiceHealth.IdEnum.REPORTING, "Statisztikai szolgáltatás", reportingBaseUrl, checkedAt);
        var sauna = asyncHealth(ServiceHealth.IdEnum.SAUNA, "Szaunafoglalás", saunaBaseUrl, checkedAt);
        var solarium = asyncHealth(ServiceHealth.IdEnum.SOLARIUM, "Szolárium-nyilvántartás", solariumBaseUrl, checkedAt);
        var services = List.of(coreHealth(checkedAt), reporting.join(), sauna.join(), solarium.join());
        var degraded = services.stream().anyMatch(health -> health.getStatus() == ServiceHealth.StatusEnum.DOWN);
        return new PlatformHealth(
                degraded ? PlatformHealth.StatusEnum.DEGRADED : PlatformHealth.StatusEnum.UP,
                checkedAt,
                services);
    }

    private CompletableFuture<ServiceHealth> asyncHealth(
            ServiceHealth.IdEnum id,
            String name,
            String baseUrl,
            OffsetDateTime checkedAt) {
        return CompletableFuture.supplyAsync(() -> downstreamHealth(id, name, baseUrl, checkedAt));
    }

    private ServiceHealth coreHealth(OffsetDateTime checkedAt) {
        return new ServiceHealth(
                ServiceHealth.IdEnum.CORE,
                "Központi API",
                ServiceHealth.StatusEnum.UP,
                checkedAt,
                "A központi API fogadja a kéréseket.")
                .responseTimeMs(0L);
    }

    private ServiceHealth downstreamHealth(
            ServiceHealth.IdEnum id,
            String name,
            String baseUrl,
            OffsetDateTime checkedAt) {
        var result = probe.probe(baseUrl);
        if (id == ServiceHealth.IdEnum.REPORTING && !result.up() && "local".equalsIgnoreCase(reportingMode)) {
            return new ServiceHealth(
                    id,
                    name,
                    ServiceHealth.StatusEnum.NOT_REQUIRED,
                    checkedAt,
                    "Helyi módban a statisztikát a core service számolja.")
                    .responseTimeMs(result.responseTimeMs());
        }
        return new ServiceHealth(
                id,
                name,
                result.up() ? ServiceHealth.StatusEnum.UP : ServiceHealth.StatusEnum.DOWN,
                checkedAt,
                result.message())
                .responseTimeMs(result.responseTimeMs());
    }
}
