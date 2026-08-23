package com.wardanger.excalibur.audit.api;

import java.util.List;
import java.time.ZoneOffset;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.generated.api.AuditApi;
import com.wardanger.excalibur.generated.model.AuditLogEntry;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuditApiController implements AuditApi {

    private final AuditLogService auditLog;
    private final SessionAuthenticationService authentication;

    @Override
    public ResponseEntity<List<AuditLogEntry>> listAuditLogs(Integer limit) {
        var employee = authentication.currentPrincipal().orElseThrow();
        return ResponseEntity.ok(auditLog.listVisible(employee, limit).stream()
                .map(entry -> new AuditLogEntry()
                        .id(entry.id())
                        .employeeId(entry.employeeId())
                        .employeeName(entry.employeeName())
                        .action(entry.action())
                        .entityType(entry.entityType())
                        .entityId(entry.entityId())
                        .summary(entry.summary())
                        .details(entry.details())
                        .occurredAt(entry.occurredAt().atOffset(ZoneOffset.UTC)))
                .toList());
    }
}
