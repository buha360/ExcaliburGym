package com.wardanger.excalibur.audit.application;

import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.audit.domain.AuditEntry;

public interface AuditLogRepository {

    void insert(AuditEntry entry);

    List<AuditEntry> findLatest(int limit);

    List<AuditEntry> findLatestByEmployee(UUID employeeId, int limit);
}
