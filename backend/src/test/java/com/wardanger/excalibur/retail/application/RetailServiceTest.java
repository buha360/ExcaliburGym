package com.wardanger.excalibur.retail.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.employee.domain.EmployeeRole;
import com.wardanger.excalibur.events.EventTopics;
import com.wardanger.excalibur.guest.application.GuestRepository;
import com.wardanger.excalibur.guest.domain.Guest;
import com.wardanger.excalibur.guest.domain.GuestRegistrationType;
import com.wardanger.excalibur.integration.outbox.IntegrationEventPublisher;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.retail.domain.RetailProduct;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RetailServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T08:00:00Z"), ZoneOffset.UTC);
    private final RetailRepository retail = mock(RetailRepository.class);
    private final GuestRepository guests = mock(GuestRepository.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final IntegrationEventPublisher events = mock(IntegrationEventPublisher.class);
    private final RetailService service = new RetailService(retail, guests, audit, events, CLOCK);
    private final UUID guestId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final GymUserPrincipal employee =
            new GymUserPrincipal(employeeId, "Teszt Pultos", EmployeeRole.EMPLOYEE, CLOCK.instant());

    @BeforeEach
    void guestExists() {
        when(guests.findById(guestId)).thenReturn(Optional.of(
                new Guest(guestId, "Minta Vendég", GuestRegistrationType.NEW, CLOCK.instant(), employeeId)));
    }

    @Test
    void depositsMoneyAndAppendsImmutableLedgerEntry() {
        when(retail.balance(guestId)).thenReturn(8_000L);

        var result = service.deposit(guestId, 8_000, PaymentMethod.BANK_CARD, employee);

        assertThat(result.balance()).isEqualTo(8_000);
        verify(retail).ensureBalanceAccount(guestId, CLOCK.instant());
        verify(retail).addBalance(guestId, 8_000, CLOCK.instant());
        var transaction = ArgumentCaptor.forClass(com.wardanger.excalibur.retail.domain.BalanceTransaction.class);
        verify(retail).insertBalanceTransaction(transaction.capture());
        assertThat(transaction.getValue().amountDelta()).isEqualTo(8_000);
        assertThat(transaction.getValue().balanceAfter()).isEqualTo(8_000);
        verify(events).publish(eq(EventTopics.RETAIL_EVENTS), eq("GUEST_BALANCE_DEPOSITED"),
                eq("GUEST_BALANCE_TRANSACTION"), any(), eq(employeeId), eq(CLOCK.instant()), any());
    }

    @Test
    void refusesPrepaidSaleWithoutEnoughBalanceAndDoesNotInsertSale() {
        var productId = UUID.randomUUID();
        when(retail.findActiveProduct(productId)).thenReturn(Optional.of(new RetailProduct(
                productId, "WATER", "Ásványvíz", 600, true, CLOCK.instant(), CLOCK.instant())));
        when(retail.subtractBalance(guestId, 1_200, CLOCK.instant())).thenReturn(false);

        assertThatThrownBy(() -> service.sell(
                guestId,
                PaymentMethod.PREPAID_BALANCE,
                List.of(new RetailService.SaleLineRequest(productId, 2)),
                employee))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("INSUFFICIENT_GUEST_BALANCE"));
    }
}