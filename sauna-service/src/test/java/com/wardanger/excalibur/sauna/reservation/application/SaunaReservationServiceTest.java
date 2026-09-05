package com.wardanger.excalibur.sauna.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wardanger.excalibur.sauna.integration.IntegrationEventPublisher;
import com.wardanger.excalibur.sauna.reservation.domain.SaunaReservation;
import com.wardanger.excalibur.sauna.shared.SaunaApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class SaunaReservationServiceTest {
    private final SaunaReservationRepository repository = mock(SaunaReservationRepository.class);
    private final IntegrationEventPublisher events = mock(IntegrationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-05T08:00:00Z"), ZoneId.of("Europe/Budapest"));
    private final SaunaReservationService service = new SaunaReservationService(repository, events, clock);

    @Test
    void createsASelectedDurationReservationAndPublishesAnEvent() {
        var reservation = service.create(
                UUID.randomUUID(), "Kovács Anna", Instant.parse("2026-09-05T10:00:00Z"),
                2, 30, UUID.randomUUID(), "Buha Milán");

        assertThat(reservation.endAt()).isEqualTo(reservation.startAt().plusSeconds(1800));
        assertThat(reservation.status()).isEqualTo(SaunaReservation.Status.RESERVED);
        verify(repository).insert(reservation);
        verify(events).publish(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void mapsDatabaseOverlapToAConflict() {
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("overlap"))
                .when(repository).insert(any());

        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(), "Kovács Anna", Instant.parse("2026-09-05T10:00:00Z"),
                2, 60, UUID.randomUUID(), "Buha Milán"))
                .isInstanceOf(SaunaApiException.class)
                .hasMessage("A szauna ebben az időpontban már foglalt.");
    }

    @Test
    void rejectsUnsupportedDuration() {
        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(), "Kovács Anna", Instant.parse("2026-09-05T10:00:00Z"),
                2, 20, UUID.randomUUID(), "Buha Milán"))
                .isInstanceOf(SaunaApiException.class)
                .hasMessageContaining("15, 30, 45 vagy 60");
    }

    @Test
    void cancellationRequiresAReasonAndPreservesTheReservation() {
        var id = UUID.randomUUID();
        var reservation = new SaunaReservation(
                id, UUID.randomUUID(), "Kovács Anna",
                Instant.parse("2026-09-05T10:00:00Z"), Instant.parse("2026-09-05T11:00:00Z"),
                1, SaunaReservation.Status.RESERVED, clock.instant(),
                UUID.randomUUID(), "Buha Milán", null, null, null, null);
        when(repository.findById(id)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(id, "x", UUID.randomUUID(), "Buha Milán"))
                .isInstanceOf(SaunaApiException.class)
                .hasMessageContaining("3–500");
    }
}