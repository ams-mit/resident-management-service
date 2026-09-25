package com.ams.resident.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class TestJwtHelper {

    public static final List<String> DEFAULT_USER_ROLES = List.of("TENANT", "TENANT_RESIDENT");

    public static JwtRequestPostProcessor userJwt(String userId) {
        return userJwt(userId, DEFAULT_USER_ROLES);
    }

    public static JwtRequestPostProcessor userJwt(String userId, List<String> roles) {
        Instant now = Instant.now();
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());

        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(authorities)
                .jwt(builder -> builder
                        .subject(userId)
                        .claim("type", "user")
                        .claim("roles", roles)
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(1800))
                );
    }

    public static JwtRequestPostProcessor serviceJwt(String serviceName) {
        Instant now = Instant.now();
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder
                        .subject(serviceName)
                        .claim("type", "service")
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(300))
                );
    }
}
