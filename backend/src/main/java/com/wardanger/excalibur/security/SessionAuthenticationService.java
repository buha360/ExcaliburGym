package com.wardanger.excalibur.security;

import java.time.Clock;
import java.util.Optional;

import com.wardanger.excalibur.employee.domain.EmployeeAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SessionAuthenticationService {

    private final SecurityContextRepository securityContextRepository;
    private final Clock clock;

    public SessionAuthenticationService(SecurityContextRepository securityContextRepository, Clock clock) {
        this.securityContextRepository = securityContextRepository;
        this.clock = clock;
    }

    public GymUserPrincipal signIn(EmployeeAccount account) {
        var request = currentRequest();
        var response = currentResponse();
        var previousSession = request.getSession(false);
        if (previousSession != null) {
            previousSession.invalidate();
        }
        request.getSession(true);

        var principal = new GymUserPrincipal(
                account.id(),
                account.displayName(),
                account.role(),
                clock.instant());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.authorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return principal;
    }

    public Optional<GymUserPrincipal> currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof GymUserPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }

    public void signOut() {
        var request = currentRequest();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private static HttpServletRequest currentRequest() {
        return currentRequestAttributes().getRequest();
    }

    private static HttpServletResponse currentResponse() {
        return currentRequestAttributes().getResponse();
    }

    private static ServletRequestAttributes currentRequestAttributes() {
        var attributes = RequestContextHolder.currentRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes;
        }
        throw new IllegalStateException("No active HTTP request is available.");
    }
}
