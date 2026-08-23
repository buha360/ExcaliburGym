package com.wardanger.excalibur.shared.error;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.generated.model.ApiError;
import com.wardanger.excalibur.shared.time.TimeConfiguration;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(error(
                exception.code(),
                exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> handleValidationException(Exception exception) {
        return ResponseEntity.badRequest().body(error(
                "VALIDATION_ERROR",
                "Ellenőrizd a megadott adatokat. A PIN-kód 4–8 számjegyből állhat."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(
                "INTERNAL_ERROR",
                "Váratlan hiba történt."));
    }

    private ApiError error(String code, String message) {
        return new ApiError(
                code,
                message,
                OffsetDateTime.ofInstant(clock.instant(), TimeConfiguration.APPLICATION_ZONE));
    }
}
