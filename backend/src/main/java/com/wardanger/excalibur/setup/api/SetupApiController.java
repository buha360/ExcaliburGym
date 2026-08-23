package com.wardanger.excalibur.setup.api;

import com.wardanger.excalibur.employee.api.EmployeeApiMapper;
import com.wardanger.excalibur.employee.application.EmployeeAccountService;
import com.wardanger.excalibur.generated.api.SetupApi;
import com.wardanger.excalibur.generated.model.AuthenticatedEmployee;
import com.wardanger.excalibur.generated.model.CreateInitialAdminRequest;
import com.wardanger.excalibur.generated.model.SetupStatus;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SetupApiController implements SetupApi {

    private final EmployeeAccountService employeeAccounts;
    private final SessionAuthenticationService sessions;
    private final EmployeeApiMapper mapper;

    public SetupApiController(
            EmployeeAccountService employeeAccounts,
            SessionAuthenticationService sessions,
            EmployeeApiMapper mapper) {
        this.employeeAccounts = employeeAccounts;
        this.sessions = sessions;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<AuthenticatedEmployee> createInitialAdmin(CreateInitialAdminRequest request) {
        var created = employeeAccounts.createInitialAdmin(request.getDisplayName(), request.getPin());
        employeeAccounts.authenticate(created.id(), request.getPin());
        var principal = sessions.signIn(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAuthenticatedEmployee(principal));
    }

    @Override
    public ResponseEntity<SetupStatus> getSetupStatus() {
        return ResponseEntity.ok(new SetupStatus(employeeAccounts.isSetupRequired()));
    }
}
