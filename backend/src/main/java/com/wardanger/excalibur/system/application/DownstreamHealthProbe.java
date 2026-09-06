package com.wardanger.excalibur.system.application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
class DownstreamHealthProbe {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(1500);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(750))
            .build();

    ProbeResult probe(String baseUrl) {
        var startedAt = System.nanoTime();
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(normalize(baseUrl) + "/actuator/health/readiness"))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            var elapsed = elapsedMillis(startedAt);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ProbeResult(true, elapsed, "A szolgáltatás fogadja a kéréseket.");
            }
            return new ProbeResult(false, elapsed, "A health ellenőrzés HTTP " + response.statusCode() + " választ adott.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ProbeResult(false, elapsedMillis(startedAt), "Az állapotellenőrzés megszakadt.");
        } catch (IllegalArgumentException exception) {
            return new ProbeResult(false, elapsedMillis(startedAt), "A szolgáltatás címe hibásan van beállítva.");
        } catch (Exception exception) {
            return new ProbeResult(false, elapsedMillis(startedAt), "A szolgáltatás nem érhető el.");
        }
    }

    private static String normalize(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static long elapsedMillis(long startedAt) {
        return Math.max(0L, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }

    record ProbeResult(boolean up, long responseTimeMs, String message) {
    }
}
