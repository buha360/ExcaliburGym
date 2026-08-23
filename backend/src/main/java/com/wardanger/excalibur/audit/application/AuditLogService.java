package com.wardanger.excalibur.audit.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.audit.domain.AuditEntry;
import com.wardanger.excalibur.employee.domain.EmployeeRole;
import com.wardanger.excalibur.security.GymUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    private final AuditLogRepository repository;
    private final Clock clock;

    public void record(
            GymUserPrincipal employee,
            String action,
            String entityType,
            UUID entityId,
            String summary) {
        record(employee.id(), employee.displayName(), action, entityType, entityId, summary, null);
    }

    public void record(
            UUID employeeId,
            String employeeName,
            String action,
            String entityType,
            UUID entityId,
            String summary,
            String details) {
        repository.insert(new AuditEntry(
                UUID.randomUUID(),
                employeeId,
                employeeName,
                action,
                entityType,
                entityId,
                summary,
                details,
                clock.instant()));
    }

    public List<AuditEntry> listVisible(GymUserPrincipal employee, Integer requestedLimit) {
        var limit = requestedLimit == null ? DEFAULT_LIMIT : Math.clamp(requestedLimit, 1, MAX_LIMIT);
        return employee.role() == EmployeeRole.ADMIN
                ? repository.findLatest(limit)
                : repository.findLatestByEmployee(employee.id(), limit);
    }
}
