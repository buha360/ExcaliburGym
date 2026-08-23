package com.wardanger.excalibur.employee.api;

import java.time.Instant;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.employee.domain.EmployeeAccount;
import com.wardanger.excalibur.generated.model.AuthenticatedEmployee;
import com.wardanger.excalibur.generated.model.Employee;
import com.wardanger.excalibur.generated.model.LoginOption;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.time.TimeConfiguration;
import org.springframework.stereotype.Component;

@Component
public class EmployeeApiMapper {

    public Employee toEmployee(EmployeeAccount account) {
        return new Employee(
                account.id(),
                account.displayName(),
                toApiRole(account.role()),
                account.enabled(),
                toLocalOffset(account.createdAt()));
    }

    public LoginOption toLoginOption(EmployeeAccount account) {
        return new LoginOption(account.id(), account.displayName(), toApiRole(account.role()));
    }

    public AuthenticatedEmployee toAuthenticatedEmployee(GymUserPrincipal principal) {
        return new AuthenticatedEmployee(
                principal.id(),
                principal.displayName(),
                toApiRole(principal.role()),
                toLocalOffset(principal.loggedInAt()));
    }

    private static com.wardanger.excalibur.generated.model.EmployeeRole toApiRole(
            com.wardanger.excalibur.employee.domain.EmployeeRole role) {
        return com.wardanger.excalibur.generated.model.EmployeeRole.fromValue(role.name());
    }

    private static OffsetDateTime toLocalOffset(Instant instant) {
        return OffsetDateTime.ofInstant(instant, TimeConfiguration.APPLICATION_ZONE);
    }
}
