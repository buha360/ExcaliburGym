package com.wardanger.excalibur.employee.api;

import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.employee.application.EmployeeAccountService;
import com.wardanger.excalibur.generated.api.EmployeesApi;
import com.wardanger.excalibur.generated.model.ChangePinRequest;
import com.wardanger.excalibur.generated.model.CreateEmployeeRequest;
import com.wardanger.excalibur.generated.model.Employee;
import com.wardanger.excalibur.generated.model.UpdateEmployeeStatusRequest;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmployeesApiController implements EmployeesApi {

    private final EmployeeAccountService employeeAccounts;
    private final EmployeeApiMapper mapper;
    private final SessionAuthenticationService authentication;

    @Override
    public ResponseEntity<Employee> createEmployee(CreateEmployeeRequest request) {
        var employee = employeeAccounts.createEmployee(request.getDisplayName(), request.getPin(), actor());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toEmployee(employee));
    }

    @Override
    public ResponseEntity<List<Employee>> listEmployees() {
        return ResponseEntity.ok(employeeAccounts.listEmployees().stream()
                .map(mapper::toEmployee)
                .toList());
    }

    @Override
    public ResponseEntity<Employee> updateEmployeeStatus(
            UUID employeeId,
            UpdateEmployeeStatusRequest request) {
        return ResponseEntity.ok(mapper.toEmployee(
                employeeAccounts.updateStatus(employeeId, request.getEnabled(), actor())));
    }

    @Override
    public ResponseEntity<Void> resetEmployeePin(UUID employeeId, ChangePinRequest request) {
        employeeAccounts.resetPin(employeeId, request.getPin(), actor());
        return ResponseEntity.noContent().build();
    }

    private GymUserPrincipal actor() {
        return authentication.currentPrincipal().orElseThrow();
    }
}
