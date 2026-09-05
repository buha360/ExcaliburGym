package com.wardanger.excalibur.sauna.reservation.application;

import com.wardanger.excalibur.events.EventTopics;
import com.wardanger.excalibur.sauna.integration.IntegrationEventPublisher;
import com.wardanger.excalibur.sauna.reservation.domain.SaunaReservation;
import com.wardanger.excalibur.sauna.shared.SaunaApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaunaReservationService {
    private final SaunaReservationRepository reservations;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public List<SaunaReservation> schedule(LocalDate date) {
        var from = date.atStartOfDay(clock.getZone()).toInstant();
        var to = date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        return reservations.findBetween(from, to);
    }

    @Transactional
    public SaunaReservation create(
            UUID guestId,
            String rawGuestName,
            Instant startAt,
            int partySize,
            int durationMinutes,
            UUID employeeId,
            String rawEmployeeName) {
        if (partySize < 1 || partySize > 3) {
            throw new SaunaApiException(HttpStatus.BAD_REQUEST, "INVALID_PARTY_SIZE", "A szaunafoglalás 1–3 főre rögzíthető.");
        }
        if (durationMinutes < 15 || durationMinutes > 60 || durationMinutes % 15 != 0) {
            throw new SaunaApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DURATION",
                    "A szaunafoglalás időtartama 15, 30, 45 vagy 60 perc lehet.");
        }
        if (startAt.isBefore(clock.instant())) {
            throw new SaunaApiException(HttpStatus.BAD_REQUEST, "RESERVATION_IN_THE_PAST", "Múltbeli időpontra nem rögzíthető foglalás.");
        }
        var guestName = cleanName(rawGuestName);
        var employeeName = cleanName(rawEmployeeName);
        var reservation = new SaunaReservation(
                UUID.randomUUID(), guestId, guestName, startAt, startAt.plusSeconds(durationMinutes * 60L),
                partySize, SaunaReservation.Status.RESERVED, clock.instant(), employeeId, employeeName,
                null, null, null, null);
        try {
            reservations.insert(reservation);
        } catch (DataIntegrityViolationException exception) {
            throw new SaunaApiException(
                    HttpStatus.CONFLICT,
                    "SAUNA_RESERVATION_CONFLICT",
                    "A szauna ebben az időpontban már foglalt.");
        }
        events.publish(
                EventTopics.SAUNA_EVENTS,
                "SAUNA_RESERVED",
                "SAUNA_RESERVATION",
                reservation.id(),
                employeeId,
                reservation.createdAt(),
                Map.of(
                        "reservationId", reservation.id().toString(),
                        "guestId", guestId.toString(),
                        "partySize", partySize,
                        "durationMinutes", durationMinutes));
        return reservation;
    }

    @Transactional
    public SaunaReservation cancel(UUID reservationId, String rawReason, UUID employeeId, String rawEmployeeName) {
        var reservation = reservations.findById(reservationId).orElseThrow(() -> new SaunaApiException(
                HttpStatus.NOT_FOUND, "SAUNA_RESERVATION_NOT_FOUND", "A szaunafoglalás nem található."));
        if (reservation.cancelled()) {
            throw new SaunaApiException(
                    HttpStatus.CONFLICT, "SAUNA_RESERVATION_ALREADY_CANCELLED", "A foglalást már lemondták.");
        }
        var reason = rawReason == null ? "" : rawReason.strip().replaceAll("\\s+", " ");
        if (reason.length() < 3 || reason.length() > 500) {
            throw new SaunaApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_CANCELLATION_REASON", "A lemondás indoka 3–500 karakter lehet.");
        }
        var now = clock.instant();
        if (!reservations.cancel(reservationId, now, employeeId, cleanName(rawEmployeeName), reason)) {
            throw new SaunaApiException(
                    HttpStatus.CONFLICT, "SAUNA_RESERVATION_ALREADY_CANCELLED", "A foglalást már lemondták.");
        }
        events.publish(
                EventTopics.SAUNA_EVENTS,
                "SAUNA_RESERVATION_CANCELLED",
                "SAUNA_RESERVATION",
                reservation.id(),
                employeeId,
                now,
                Map.of("reservationId", reservation.id().toString(), "guestId", reservation.guestId().toString()));
        return reservations.findById(reservationId).orElseThrow();
    }

    private static String cleanName(String value) {
        var clean = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (clean.length() < 2 || clean.length() > 160) {
            throw new SaunaApiException(HttpStatus.BAD_REQUEST, "INVALID_NAME", "A név 2–160 karakter lehet.");
        }
        return clean;
    }
}