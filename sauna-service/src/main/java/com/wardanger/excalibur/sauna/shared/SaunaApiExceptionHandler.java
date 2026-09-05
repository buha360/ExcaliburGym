package com.wardanger.excalibur.sauna.shared;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SaunaApiExceptionHandler {
    @ExceptionHandler(SaunaApiException.class)
    ResponseEntity<Map<String, Object>> handle(SaunaApiException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of(
                "code", exception.code(),
                "message", exception.getMessage(),
                "timestamp", Instant.now().toString()));
    }
}
