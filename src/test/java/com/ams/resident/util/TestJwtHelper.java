package com.ams.resident.util;

import com.ams.resident.security.SecurityConfig;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.time.Instant;
import java.util.List;

public class TestJwtHelper {

    public static final List<String> DEFAULT_USER_ROLES = List.of("TENANT_RESIDENT");
    private static final SecurityConfig.CustomRoleConverter ROLE_CONVERTER = new SecurityConfig.CustomRoleConverter();

    public static JwtRequestPostProcessor userJwt(String userId) {
        return userJwt(userId, DEFAULT_USER_ROLES);
    }

    public static JwtRequestPostProcessor userJwt(String userId, List<String> roles) {
        Instant now = Instant.now();
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(ROLE_CONVERTER)
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
