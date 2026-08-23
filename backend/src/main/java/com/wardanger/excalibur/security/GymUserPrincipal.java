package com.wardanger.excalibur.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.employee.domain.EmployeeRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record GymUserPrincipal(
        UUID id,
        String displayName,
        EmployeeRole role,
        Instant loggedInAt) implements Serializable {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
