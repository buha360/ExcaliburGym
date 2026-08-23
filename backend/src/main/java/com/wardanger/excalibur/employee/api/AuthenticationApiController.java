package com.wardanger.excalibur.employee.api;

import java.util.List;

import com.wardanger.excalibur.employee.application.EmployeeAccountService;
import com.wardanger.excalibur.generated.api.AuthenticationApi;
import com.wardanger.excalibur.generated.model.AuthenticatedEmployee;
import com.wardanger.excalibur.generated.model.CsrfTokenResponse;
import com.wardanger.excalibur.generated.model.LoginOption;
import com.wardanger.excalibur.generated.model.LoginRequest;
import com.wardanger.excalibur.generated.model.SessionStatus;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import com.wardanger.excalibur.shared.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationApiController implements AuthenticationApi {

    private final EmployeeAccountService employeeAccounts;
    private final SessionAuthenticationService sessions;
    private final EmployeeApiMapper mapper;
    private final HttpServletRequest request;

    public AuthenticationApiController(
            EmployeeAccountService employeeAccounts,
            SessionAuthenticationService sessions,
            EmployeeApiMapper mapper,
            HttpServletRequest request) {
        this.employeeAccounts = employeeAccounts;
        this.sessions = sessions;
        this.mapper = mapper;
        this.request = request;
    }

    @Override
    public ResponseEntity<CsrfTokenResponse> getCsrfToken() {
        var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) request.getAttribute("_csrf");
        }
        if (token == null) {
            throw new IllegalStateException("CSRF token was not created by the security filter chain.");
        }
        return ResponseEntity.ok(new CsrfTokenResponse(token.getHeaderName(), token.getToken()));
    }

    @Override
    public ResponseEntity<List<LoginOption>> listLoginOptions() {
        return ResponseEntity.ok(employeeAccounts.listLoginOptions().stream()
                .map(mapper::toLoginOption)
                .toList());
    }

    @Override
    public ResponseEntity<AuthenticatedEmployee> login(LoginRequest loginRequest) {
        var account = employeeAccounts.authenticate(loginRequest.getEmployeeId(), loginRequest.getPin());
        var principal = sessions.signIn(account);
        return ResponseEntity.ok(mapper.toAuthenticatedEmployee(principal));
    }

    @Override
    public ResponseEntity<SessionStatus> getSession() {
        var principal = sessions.currentPrincipal();
        var status = new SessionStatus(principal.isPresent());
        principal.map(mapper::toAuthenticatedEmployee).ifPresent(status::setEmployee);
        return ResponseEntity.ok(status);
    }

    @Override
    public ResponseEntity<Void> logout() {
        var principal = sessions.currentPrincipal().orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "A kijelentkezéshez bejelentkezés szükséges."));
        employeeAccounts.recordLogout(principal.id());
        sessions.signOut();
        return ResponseEntity.noContent().build();
    }
}
