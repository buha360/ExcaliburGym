package com.wardanger.excalibur.audit.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.wardanger.excalibur.employee.domain.EmployeeRole;
import com.wardanger.excalibur.security.GymUserPrincipal;
import org.junit.jupiter.api.Test;

class AuditLogServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC);
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditLogService service = new AuditLogService(repository, CLOCK);

    @Test
    void administratorCanReadTheCompleteLog() {
        var admin = principal(EmployeeRole.ADMIN);

        service.listVisible(admin, 200);

        verify(repository).findLatest(200);
    }

    @Test
    void employeeCanOnlyReadTheirOwnLogAndLimitIsClamped() {
        var employee = principal(EmployeeRole.EMPLOYEE);

        service.listVisible(employee, 10_000);

        verify(repository).findLatestByEmployee(employee.id(), 500);
    }

    private static GymUserPrincipal principal(EmployeeRole role) {
        return new GymUserPrincipal(UUID.randomUUID(), "Teszt Dolgozó", role, CLOCK.instant());
    }
}
