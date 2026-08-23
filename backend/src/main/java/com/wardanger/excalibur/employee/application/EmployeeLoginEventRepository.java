package com.wardanger.excalibur.employee.application;

import java.time.Instant;
import java.util.UUID;

public interface EmployeeLoginEventRepository {

    void insert(UUID eventId, UUID employeeId, LoginEventType eventType, Instant occurredAt);

    enum LoginEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT
    }
}
