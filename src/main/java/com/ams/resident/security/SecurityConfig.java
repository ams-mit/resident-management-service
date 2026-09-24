package com.ams.resident.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

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
                .requestMatchers("/api/v1/profiles/me/**", "/api/v1/relationships/me/**").hasAnyRole("TENANT", "OWNER", "RESIDENT", "APARTMENT_MANAGER", "SYSTEM_ADMIN")
                .requestMatchers("/api/v1/relationships").hasAnyRole("TENANT", "OWNER", "RESIDENT", "APARTMENT_MANAGER", "SYSTEM_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
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

    private static class JwtTypeValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            String type = jwt.getClaimAsString("type");
            if (type == null || (!type.equals("user") && !type.equals("service"))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid or missing 'type' claim", null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }

    private static class CustomRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
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
