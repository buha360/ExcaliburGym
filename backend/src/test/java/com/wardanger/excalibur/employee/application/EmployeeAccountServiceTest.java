package com.wardanger.excalibur.employee.application;

import static com.wardanger.excalibur.employee.application.EmployeeLoginEventRepository.LoginEventType.LOGIN_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.employee.domain.EmployeeAccount;
import com.wardanger.excalibur.employee.domain.EmployeeRole;
import com.wardanger.excalibur.shared.error.ApiException;
import com.wardanger.excalibur.security.GymUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmployeeAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T18:30:00Z");

    @Mock
    private EmployeeAccountRepository accounts;

    @Mock
    private EmployeeLoginEventRepository loginEvents;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLog;

    private EmployeeAccountService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeAccountService(
                accounts,
                loginEvents,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC),
                auditLog);
    }

    @Test
    void createsTheFirstAccountAsAdminWithAnEncodedPin() {
        when(accounts.count()).thenReturn(0L);
        when(accounts.existsByDisplayNameIgnoreCase("Tulajdonos")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encoded-pin");
        var accountCaptor = ArgumentCaptor.forClass(EmployeeAccount.class);

        var account = service.createInitialAdmin("  Tulajdonos  ", "1234");

        verify(accounts).insert(accountCaptor.capture());
        assertThat(account).isEqualTo(accountCaptor.getValue());
        assertThat(account.displayName()).isEqualTo("Tulajdonos");
        assertThat(account.pinHash()).isEqualTo("encoded-pin");
        assertThat(account.role()).isEqualTo(EmployeeRole.ADMIN);
        assertThat(account.enabled()).isTrue();
        assertThat(account.createdAt()).isEqualTo(NOW);
    }

    @Test
    void refusesAnotherInitialAdminAfterSetup() {
        when(accounts.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.createInitialAdmin("Másik admin", "1234"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("SETUP_ALREADY_COMPLETED");
                    assertThat(exception.status().value()).isEqualTo(409);
                });

        verify(accounts, never()).insert(any());
    }

    @Test
    void recordsAFailedPinAttemptWithoutRevealingWhichPartWasWrong() {
        var account = account(EmployeeRole.EMPLOYEE, true);
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("9999", account.pinHash())).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(account.id(), "9999"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(exception.status().value()).isEqualTo(401);
                });

        verify(loginEvents).insert(any(UUID.class), eq(account.id()), eq(LOGIN_FAILED), eq(NOW));
    }

    @Test
    void protectsTheOwnerAccountFromDeactivation() {
        var admin = account(EmployeeRole.ADMIN, true);
        when(accounts.findById(admin.id())).thenReturn(Optional.of(admin));

        var actor = new GymUserPrincipal(admin.id(), admin.displayName(), EmployeeRole.ADMIN, NOW);
        assertThatThrownBy(() -> service.updateStatus(admin.id(), false, actor))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("ADMIN_ACCOUNT_PROTECTED"));

        verify(accounts, never()).updateEnabled(any(), eq(false), any());
    }

    private EmployeeAccount account(EmployeeRole role, boolean enabled) {
        return new EmployeeAccount(
                UUID.fromString("1b75f7f6-e5f2-42d1-a1d8-31d849ff7050"),
                "Teszt Elek",
                "encoded-pin",
                role,
                enabled,
                NOW,
                NOW);
    }
}
