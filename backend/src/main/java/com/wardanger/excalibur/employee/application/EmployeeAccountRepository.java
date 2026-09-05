package com.wardanger.excalibur.employee.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.employee.domain.EmployeeAccount;

public interface EmployeeAccountRepository {

    long count();

    boolean existsByDisplayNameIgnoreCase(String displayName);

    Optional<EmployeeAccount> findById(UUID id);

    List<EmployeeAccount> findAll();

    List<EmployeeAccount> findAllEnabled();

    void insert(EmployeeAccount account);

    void updateEnabled(UUID id, boolean enabled, Instant updatedAt);

    void updatePinHash(UUID id, String pinHash, Instant updatedAt);
}
