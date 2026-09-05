package com.wardanger.excalibur.sauna.application;

import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class SaunaBookingClient {
    private final RestClient saunaRestClient;

    public Schedule schedule(LocalDate date) {
        try {
            var response = saunaRestClient.get()
                    .uri(builder -> builder.path("/internal/v1/sauna/reservations")
                            .queryParam("date", date).build())
                    .retrieve()
                    .body(Schedule.class);
            return response == null ? new Schedule(date, List.of()) : response;
        } catch (RestClientException exception) {
            throw map(exception);
        }
    }

    public Reservation create(
            UUID guestId,
            String guestName,
            OffsetDateTime startAt,
            int partySize,
            int durationMinutes,
            GymUserPrincipal employee) {
        try {
            var response = saunaRestClient.post()
                    .uri("/internal/v1/sauna/reservations")
                    .body(new CreateRequest(
                            guestId, guestName, startAt, partySize, durationMinutes, employee.id(), employee.displayName()))
                    .retrieve()
                    .body(Reservation.class);
            if (response == null) {
                throw unavailable();
            }
            return response;
        } catch (RestClientException exception) {
            throw map(exception);
        }
    }

    public Reservation cancel(UUID reservationId, String reason, GymUserPrincipal employee) {
        try {
            var response = saunaRestClient.post()
                    .uri("/internal/v1/sauna/reservations/{id}/cancellation", reservationId)
                    .body(new CancelRequest(reason, employee.id(), employee.displayName()))
                    .retrieve()
                    .body(Reservation.class);
            if (response == null) {
                throw unavailable();
            }
            return response;
        } catch (RestClientException exception) {
            throw map(exception);
        }
    }

    private static ApiException map(RestClientException exception) {
        if (exception instanceof HttpClientErrorException clientError) {
            if (clientError.getStatusCode().value() == 404) {
                return new ApiException(HttpStatus.NOT_FOUND, "SAUNA_RESERVATION_NOT_FOUND", "A szaunafoglalás nem található.");
            }
            if (clientError.getStatusCode().value() == 409) {
                return new ApiException(HttpStatus.CONFLICT, "SAUNA_RESERVATION_CONFLICT", "A kiválasztott szaunaidőpont már foglalt vagy a foglalást már lemondták.");
            }
            if (clientError.getStatusCode().value() == 400) {
                return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SAUNA_RESERVATION", "A szaunafoglalás adatai érvénytelenek.");
            }
        }
        return unavailable();
    }

    private static ApiException unavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SAUNA_SERVICE_UNAVAILABLE",
                "A szaunafoglalási szolgáltatás átmenetileg nem érhető el.");
    }

    public record Schedule(LocalDate date, List<Reservation> reservations) {}
    public record Reservation(
            UUID id,
            UUID guestId,
            String guestName,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int partySize,
            String status,
            OffsetDateTime createdAt,
            UUID createdByEmployeeId,
            String createdByEmployeeName,
            OffsetDateTime cancelledAt,
            UUID cancelledByEmployeeId,
            String cancelledByEmployeeName,
            String cancellationReason) {}
    private record CreateRequest(
            UUID guestId,
            String guestName,
            OffsetDateTime startAt,
            int partySize,
            int durationMinutes,
            UUID employeeId,
            String employeeName) {}
    private record CancelRequest(String reason, UUID employeeId, String employeeName) {}
}
