package com.wardanger.excalibur.solarium.shared;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SolariumApiExceptionHandler {
    @ExceptionHandler(SolariumApiException.class)
    ResponseEntity<Map<String, Object>> handle(SolariumApiException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of(
                "code", exception.code(),
                "message", exception.getMessage(),
                "timestamp", Instant.now().toString()));
    }
}
