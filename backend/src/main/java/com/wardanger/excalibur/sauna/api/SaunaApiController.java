package com.wardanger.excalibur.sauna.api;

import com.wardanger.excalibur.generated.api.SaunaApi;
import com.wardanger.excalibur.generated.model.CancelSaunaReservationRequest;
import com.wardanger.excalibur.generated.model.CreateSaunaReservationRequest;
import com.wardanger.excalibur.generated.model.SaunaReservation;
import com.wardanger.excalibur.generated.model.SaunaReservationStatus;
import com.wardanger.excalibur.generated.model.SaunaSchedule;
import com.wardanger.excalibur.guest.application.GuestService;
import com.wardanger.excalibur.sauna.application.SaunaBookingClient;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import com.wardanger.excalibur.shared.error.ApiException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SaunaApiController implements SaunaApi {
    private final SaunaBookingClient sauna;
    private final GuestService guests;
    private final SessionAuthenticationService sessions;

    @Override
    public ResponseEntity<SaunaSchedule> getSaunaSchedule(LocalDate date) {
        var schedule = sauna.schedule(date);
        return ResponseEntity.ok(new SaunaSchedule(
                schedule.date(), schedule.reservations().stream().map(SaunaApiController::map).toList()));
    }

    @Override
    public ResponseEntity<SaunaReservation> createSaunaReservation(CreateSaunaReservationRequest request) {
        var guest = guests.getGuest(request.getGuestId()).guest();
        var created = sauna.create(
                guest.id(), guest.fullName(), request.getStartAt(), request.getPartySize(), request.getDurationMinutes(), currentEmployee());
        return ResponseEntity.status(HttpStatus.CREATED).body(map(created));
    }

    @Override
    public ResponseEntity<SaunaReservation> cancelSaunaReservation(
            UUID reservationId,
            CancelSaunaReservationRequest request) {
        return ResponseEntity.ok(map(sauna.cancel(reservationId, request.getReason(), currentEmployee())));
    }

    private GymUserPrincipal currentEmployee() {
        return sessions.currentPrincipal().orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "A művelethez bejelentkezés szükséges."));
    }

    private static SaunaReservation map(SaunaBookingClient.Reservation source) {
        var result = new SaunaReservation(
                source.id(),
                source.guestId(),
                source.guestName(),
                source.startAt(),
                source.endAt(),
                source.partySize(),
                SaunaReservationStatus.valueOf(source.status()),
                source.createdAt(),
                source.createdByEmployeeName());
        result.setCancelledAt(source.cancelledAt());
        result.setCancelledByEmployeeName(source.cancelledByEmployeeName());
        result.setCancellationReason(source.cancellationReason());
        return result;
    }
}
