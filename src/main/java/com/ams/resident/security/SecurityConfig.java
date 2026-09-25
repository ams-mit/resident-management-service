package com.ams.resident.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${gateway.jwt.public-key}")
    private String publicKeyStr;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/profiles/me/**", "/api/v1/relationships/me/**").hasAnyRole("TENANT_RESIDENT", "OWNER", "APARTMENT_MANAGER", "SYSTEM_ADMINISTRATOR", "SYSTEM_ADMIN")
                .requestMatchers("/api/v1/relationships").hasAnyRole("TENANT_RESIDENT", "OWNER", "APARTMENT_MANAGER", "SYSTEM_ADMINISTRATOR", "SYSTEM_ADMIN")
                .requestMatchers("/internal/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .addFilterAfter(new TokenTypeFilter(), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            String cleanKey = publicKeyStr
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
            
            OAuth2TokenValidator<Jwt> defaultValidator = new JwtTimestampValidator();
            OAuth2TokenValidator<Jwt> typeValidator = new JwtTypeValidator();
            
            OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(defaultValidator, typeValidator);
            jwtDecoder.setJwtValidator(delegatingValidator);

            return jwtDecoder;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key", e);
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomRoleConverter());
        return converter;
    }

    public static class TokenTypeFilter extends OncePerRequestFilter {

        private final BearerTokenAuthenticationEntryPoint authenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String path = request.getRequestURI();
                String type = jwtAuth.getToken().getClaimAsString("type");

                if (path.startsWith("/api/v1/")) {
                    if (!"user".equals(type)) {
                        OAuth2Error error = new OAuth2Error(
                                BearerTokenErrorCodes.INVALID_TOKEN,
                                "The access token 'type' claim must be 'user' for /api/v1 endpoints",
                                null
                        );
                        authenticationEntryPoint.commence(request, response, new OAuth2AuthenticationException(error));
                        return;
                    }
                } else if (path.startsWith("/internal/v1/")) {
                    if (!"service".equals(type)) {
                        OAuth2Error error = new OAuth2Error(
                                BearerTokenErrorCodes.INVALID_TOKEN,
                                "The access token 'type' claim must be 'service' for /internal/v1 endpoints",
                                null
                        );
                        authenticationEntryPoint.commence(request, response, new OAuth2AuthenticationException(error));
                        return;
                    }
                }
            }

            filterChain.doFilter(request, response);
        }
    }

    private static class JwtTypeValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            String type = jwt.getClaimAsString("type");
            if (type == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(BearerTokenErrorCodes.INVALID_TOKEN, "Missing 'type' claim", null));
            }
            if (!type.equals("user") && !type.equals("service")) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(BearerTokenErrorCodes.INVALID_TOKEN, "Unknown 'type' claim: " + type, null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }

    public static class CustomRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null || roles.isEmpty()) {
                return List.of();
            }
            return roles.stream()
                    .filter(StringUtils::hasText)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        }
    }
}
