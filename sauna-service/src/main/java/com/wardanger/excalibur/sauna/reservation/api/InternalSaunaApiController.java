package com.wardanger.excalibur.sauna.reservation.api;

import com.wardanger.excalibur.sauna.generated.api.InternalSaunaApi;
import com.wardanger.excalibur.sauna.generated.model.InternalCancelSaunaReservationRequest;
import com.wardanger.excalibur.sauna.generated.model.InternalCreateSaunaReservationRequest;
import com.wardanger.excalibur.sauna.generated.model.InternalSaunaReservation;
import com.wardanger.excalibur.sauna.generated.model.InternalSaunaReservationStatus;
import com.wardanger.excalibur.sauna.generated.model.InternalSaunaSchedule;
import com.wardanger.excalibur.sauna.reservation.application.SaunaReservationService;
import com.wardanger.excalibur.sauna.reservation.domain.SaunaReservation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalSaunaApiController implements InternalSaunaApi {
    private final SaunaReservationService reservations;
    private final Clock clock;
    private final byte[] expectedApiKey;

    public InternalSaunaApiController(
            SaunaReservationService reservations,
            Clock clock,
            @Value("${excalibur.internal-api-key}") String expectedApiKey) {
        this.reservations = reservations;
        this.clock = clock;
        this.expectedApiKey = expectedApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ResponseEntity<InternalSaunaSchedule> getInternalSaunaSchedule(String apiKey, LocalDate date) {
        if (!authorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<InternalSaunaReservation> items = reservations.schedule(date).stream().map(this::map).toList();
        return ResponseEntity.ok(new InternalSaunaSchedule(date, items));
    }

    @Override
    public ResponseEntity<InternalSaunaReservation> createInternalSaunaReservation(
            String apiKey,
            InternalCreateSaunaReservationRequest request) {
        if (!authorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var created = reservations.create(
                request.getGuestId(),
                request.getGuestName(),
                request.getStartAt().toInstant(),
                request.getPartySize(),
                request.getDurationMinutes(),
                request.getEmployeeId(),
                request.getEmployeeName());
        return ResponseEntity.status(HttpStatus.CREATED).body(map(created));
    }

    @Override
    public ResponseEntity<InternalSaunaReservation> cancelInternalSaunaReservation(
            String apiKey,
            UUID reservationId,
            InternalCancelSaunaReservationRequest request) {
        if (!authorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(map(reservations.cancel(
                reservationId,
                request.getReason(),
                request.getEmployeeId(),
                request.getEmployeeName())));
    }

    private boolean authorized(String apiKey) {
        return apiKey != null && MessageDigest.isEqual(
                expectedApiKey, apiKey.getBytes(StandardCharsets.UTF_8));
    }

    private InternalSaunaReservation map(SaunaReservation reservation) {
        var result = new InternalSaunaReservation(
                reservation.id(),
                reservation.guestId(),
                reservation.guestName(),
                OffsetDateTime.ofInstant(reservation.startAt(), clock.getZone()),
                OffsetDateTime.ofInstant(reservation.endAt(), clock.getZone()),
                reservation.partySize(),
                InternalSaunaReservationStatus.valueOf(reservation.status().name()),
                OffsetDateTime.ofInstant(reservation.createdAt(), clock.getZone()),
                reservation.createdByEmployeeId(),
                reservation.createdByEmployeeName());
        if (reservation.cancelledAt() != null) {
            result.setCancelledAt(OffsetDateTime.ofInstant(reservation.cancelledAt(), clock.getZone()));
            result.setCancelledByEmployeeId(reservation.cancelledByEmployeeId());
            result.setCancelledByEmployeeName(reservation.cancelledByEmployeeName());
            result.setCancellationReason(reservation.cancellationReason());
        }
        return result;
    }
}
