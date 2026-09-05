package com.wardanger.excalibur.employee.application;

import static com.wardanger.excalibur.employee.application.EmployeeLoginEventRepository.LoginEventType.LOGIN_FAILED;
import static com.wardanger.excalibur.employee.application.EmployeeLoginEventRepository.LoginEventType.LOGIN_SUCCESS;
import static com.wardanger.excalibur.employee.application.EmployeeLoginEventRepository.LoginEventType.LOGOUT;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.employee.domain.EmployeeAccount;
import com.wardanger.excalibur.employee.domain.EmployeeRole;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeAccountService {

    private final EmployeeAccountRepository accounts;
    private final EmployeeLoginEventRepository loginEvents;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AuditLogService auditLog;

    public boolean isSetupRequired() {
        return accounts.count() == 0;
    }

    @Transactional
    public EmployeeAccount createInitialAdmin(String displayName, String pin) {
        if (!isSetupRequired()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SETUP_ALREADY_COMPLETED",
                    "A kezdeti adminisztrátor már létre lett hozva.");
        }
        var account = createAccount(displayName, pin, EmployeeRole.ADMIN);
        auditLog.record(account.id(), account.displayName(), "ADMIN_CREATED", "EMPLOYEE", account.id(),
                "Tulajdonosi fiók létrehozva: " + account.displayName(), null);
        return account;
    }

    @Transactional
    public EmployeeAccount createEmployee(String displayName, String pin, GymUserPrincipal actor) {
        var account = createAccount(displayName, pin, EmployeeRole.EMPLOYEE);
        auditLog.record(actor, "EMPLOYEE_CREATED", "EMPLOYEE", account.id(), "Dolgozó létrehozva: " + account.displayName());
        return account;
    }

    public List<EmployeeAccount> listEmployees() {
        return accounts.findAll();
    }

    public List<EmployeeAccount> listLoginOptions() {
        return accounts.findAllEnabled();
    }

    @Transactional
    public EmployeeAccount authenticate(UUID employeeId, String pin) {
        var account = accounts.findById(employeeId).orElse(null);
        if (account == null || !account.enabled() || !passwordEncoder.matches(pin, account.pinHash())) {
            if (account != null) {
                loginEvents.insert(UUID.randomUUID(), account.id(), LOGIN_FAILED, clock.instant());
            }
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Hibás dolgozó vagy PIN-kód.");
        }

        loginEvents.insert(UUID.randomUUID(), account.id(), LOGIN_SUCCESS, clock.instant());
        auditLog.record(account.id(), account.displayName(), "LOGIN", "EMPLOYEE", account.id(), "Bejelentkezés", null);
        return account;
    }

    @Transactional
    public void recordLogout(UUID employeeId) {
        if (accounts.findById(employeeId).isPresent()) {
            var account = accounts.findById(employeeId).orElseThrow();
            loginEvents.insert(UUID.randomUUID(), employeeId, LOGOUT, clock.instant());
            auditLog.record(account.id(), account.displayName(), "LOGOUT", "EMPLOYEE", account.id(), "Kijelentkezés", null);
        }
    }

    @Transactional
    public EmployeeAccount updateStatus(
            UUID employeeId,
            boolean enabled,
            GymUserPrincipal actor) {
        var account = getRequired(employeeId);
        if (account.role() == EmployeeRole.ADMIN && !enabled) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ADMIN_ACCOUNT_PROTECTED",
                    "A tulajdonosi adminfiók nem deaktiválható.");
        }

        accounts.updateEnabled(employeeId, enabled, clock.instant());
        var updated = getRequired(employeeId);
        auditLog.record(actor, enabled ? "EMPLOYEE_ACTIVATED" : "EMPLOYEE_DEACTIVATED", "EMPLOYEE", employeeId,
                (enabled ? "Dolgozó aktiválva: " : "Dolgozó deaktiválva: ") + updated.displayName());
        return updated;
    }

    @Transactional
    public void resetPin(UUID employeeId, String pin, GymUserPrincipal actor) {
        var account = getRequired(employeeId);
        accounts.updatePinHash(employeeId, passwordEncoder.encode(pin), clock.instant());
        auditLog.record(actor, "EMPLOYEE_PIN_CHANGED", "EMPLOYEE", employeeId, "PIN-kód módosítva: " + account.displayName());
    }

    private EmployeeAccount createAccount(String rawDisplayName, String pin, EmployeeRole role) {
        var displayName = rawDisplayName.strip();
        if (accounts.existsByDisplayNameIgnoreCase(displayName)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMPLOYEE_NAME_ALREADY_EXISTS",
                    "Már létezik ilyen nevű dolgozó.");
        }

        var now = clock.instant();
        var account = new EmployeeAccount(
                UUID.randomUUID(),
                displayName,
                passwordEncoder.encode(pin),
                role,
                true,
                now,
                now);
        accounts.insert(account);
        return account;
    }

    private EmployeeAccount getRequired(UUID employeeId) {
        return accounts.findById(employeeId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "EMPLOYEE_NOT_FOUND",
                "A dolgozó nem található."));
    }
}
