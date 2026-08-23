package com.wardanger.excalibur.guest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.employee.domain.EmployeeRole;
import com.wardanger.excalibur.guest.domain.Guest;
import com.wardanger.excalibur.guest.domain.GuestRegistrationType;
import com.wardanger.excalibur.pass.application.GuestPassRepository;
import com.wardanger.excalibur.pass.domain.GuestPass;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.product.application.ProductDefinitionRepository;
import com.wardanger.excalibur.product.domain.ProductDefinition;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import com.wardanger.excalibur.visit.application.GuestCheckInRepository;
import com.wardanger.excalibur.visit.domain.GuestCheckIn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GuestServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T10:15:30Z"),
            ZoneId.of("Europe/Budapest"));

    private final GuestRepository guests = mock(GuestRepository.class);
    private final ProductDefinitionRepository products = mock(ProductDefinitionRepository.class);
    private final GuestPassRepository passes = mock(GuestPassRepository.class);
    private final GuestCheckInRepository checkIns = mock(GuestCheckInRepository.class);
    private final AuditLogService auditLog = mock(AuditLogService.class);
    private final GuestService service = new GuestService(guests, products, passes, checkIns, auditLog, CLOCK);
    private final UUID employeeId = UUID.randomUUID();
    private final GymUserPrincipal employee = new GymUserPrincipal(
            employeeId,
            "Teszt Pultos",
            EmployeeRole.EMPLOYEE,
            CLOCK.instant());

    @BeforeEach
    void setUpGuest() {
        var guestId = guestId();
        when(guests.findById(guestId)).thenReturn(Optional.of(new Guest(
                guestId,
                "Minta Vendég",
                GuestRegistrationType.NEW,
                CLOCK.instant(),
                employeeId)));
    }

    @Test
    void createsGuestWithCollapsedNameAndRegistrationSource() {
        service.createGuest("  Kiss   Anna  ", GuestRegistrationType.IMPORTED, employee);

        var guestCaptor = ArgumentCaptor.forClass(Guest.class);
        verify(guests).insert(guestCaptor.capture(), eq("kiss anna"));
        assertThat(guestCaptor.getValue().fullName()).isEqualTo("Kiss Anna");
        assertThat(guestCaptor.getValue().registrationType()).isEqualTo(GuestRegistrationType.IMPORTED);
        assertThat(guestCaptor.getValue().createdByEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    void sellsPassWithInclusiveValidityAndEmployeeSnapshot() {
        var productId = UUID.randomUUID();
        when(products.findActiveById(productId)).thenReturn(Optional.of(new ProductDefinition(
                productId,
                "GYM_8_ENTRY",
                "8 alkalmas bérlet",
                60,
                8,
                12_500,
                true)));

        var soldPass = service.sellPass(
                guestId(),
                productId,
                LocalDate.of(2026, 8, 20),
                PaymentMethod.BANK_CARD,
                employee);

        assertThat(soldPass.validUntil()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(soldPass.totalEntries()).isEqualTo(8);
        assertThat(soldPass.remainingEntries()).isEqualTo(8);
        assertThat(soldPass.pricePaid()).isEqualTo(12_500);
        assertThat(soldPass.paymentMethod()).isEqualTo(PaymentMethod.BANK_CARD);
        assertThat(soldPass.issuedByEmployeeName()).isEqualTo("Teszt Pultos");
        verify(passes).insert(soldPass);
    }

    @Test
    void rejectsPrepaidPaymentUntilBalanceLedgerExists() {
        var productId = UUID.randomUUID();
        when(products.findActiveById(productId)).thenReturn(Optional.of(new ProductDefinition(
                productId,
                "DAY_PASS",
                "Napijegy",
                1,
                1,
                3_000,
                true)));

        assertThatThrownBy(() -> service.sellPass(
                guestId(),
                productId,
                LocalDate.of(2026, 8, 20),
                PaymentMethod.PREPAID_BALANCE,
                employee))
                .isInstanceOf(ApiException.class);
        verify(passes, org.mockito.Mockito.never()).insert(any(GuestPass.class));
    }

    @Test
    void reversesSameDayCheckInAndRestoresConsumedEntry() {
        var checkInId = UUID.randomUUID();
        var passId = UUID.randomUUID();
        var checkIn = checkIn(checkInId, passId, CLOCK.instant().minusSeconds(60), true);
        when(checkIns.findByIdForGuest(checkInId, guestId())).thenReturn(Optional.of(checkIn));
        when(checkIns.reverse(checkInId, CLOCK.instant(), employeeId, "Téves vendég kiválasztása"))
                .thenReturn(true);
        when(passes.restoreEntry(passId)).thenReturn(true);

        var reversed = service.reverseCheckIn(
                guestId(),
                checkInId,
                "  Téves   vendég kiválasztása  ",
                employee);

        assertThat(reversed.reversed()).isTrue();
        assertThat(reversed.reversalReason()).isEqualTo("Téves vendég kiválasztása");
        assertThat(reversed.reversedByEmployeeName()).isEqualTo("Teszt Pultos");
        verify(passes).restoreEntry(passId);
    }

    @Test
    void rejectsReversalOfPreviousDayCheckIn() {
        var checkInId = UUID.randomUUID();
        var passId = UUID.randomUUID();
        var checkIn = checkIn(
                checkInId,
                passId,
                Instant.parse("2026-08-19T10:15:30Z"),
                true);
        when(checkIns.findByIdForGuest(checkInId, guestId())).thenReturn(Optional.of(checkIn));

        assertThatThrownBy(() -> service.reverseCheckIn(
                guestId(),
                checkInId,
                "Téves beléptetés",
                employee))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo("CHECK_IN_REVERSAL_WINDOW_CLOSED");
        verify(checkIns, org.mockito.Mockito.never()).reverse(any(), any(), any(), any());
    }

    @Test
    void dailyCheckInFlagOnlyUsesTheCurrentBudapestCalendarDay() {
        var guest = new Guest(
                guestId(),
                "Minta Vendég",
                GuestRegistrationType.NEW,
                CLOCK.instant(),
                employeeId);
        var previousDayCheckIn = checkIn(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-19T21:59:59Z"),
                false);
        var todayCheckIn = checkIn(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-19T22:00:00Z"),
                false);

        assertThat(new GuestService.GuestProfile(guest, List.of(), List.of(previousDayCheckIn))
                .checkedInToday(CLOCK)).isFalse();
        assertThat(new GuestService.GuestProfile(guest, List.of(), List.of(todayCheckIn))
                .checkedInToday(CLOCK)).isTrue();
    }

    private GuestCheckIn checkIn(UUID checkInId, UUID passId, Instant checkedInAt, boolean consumedEntry) {
        return new GuestCheckIn(
                checkInId,
                guestId(),
                passId,
                "8 alkalmas bérlet",
                checkedInAt,
                employeeId,
                "Teszt Pultos",
                consumedEntry,
                null,
                null,
                null,
                null);
    }

    private static UUID guestId() {
        return UUID.fromString("b6c980de-3fbf-4483-9ccd-445dc392ae0c");
    }
}
