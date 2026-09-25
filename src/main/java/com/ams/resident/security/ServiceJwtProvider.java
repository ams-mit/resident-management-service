package com.ams.resident.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Component
public class ServiceJwtProvider {

    private final String serviceName;
    private final Duration expiresIn;
    private final RSAPrivateKey privateKey;

    @Autowired
    public ServiceJwtProvider(
            @Value("${service.name:resident-management-service}") String serviceName,
            @Value("${service.jwt.private-key:}") String privateKeyPem,
            @Value("${service.jwt.expires-in:5m}") String expiresInStr) {
        this.serviceName = serviceName;
        this.expiresIn = parseDuration(expiresInStr);
        if (StringUtils.hasText(privateKeyPem)) {
            this.privateKey = parsePrivateKey(privateKeyPem);
        } else {
            this.privateKey = null;
        }
    }

    public ServiceJwtProvider(String serviceName, RSAPrivateKey privateKey, Duration expiresIn) {
        this.serviceName = serviceName;
        this.privateKey = privateKey;
        this.expiresIn = expiresIn != null ? expiresIn : Duration.ofMinutes(5);
    }

    public String generateToken() {
        if (privateKey == null) {
            throw new IllegalStateException("SERVICE_JWT_PRIVATE_KEY is not configured");
        }
        try {
            Date now = new Date();
            Date exp = new Date(now.getTime() + expiresIn.toMillis());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(serviceName)
                    .claim("type", "service")
                    .issueTime(now)
                    .expirationTime(exp)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    claimsSet
            );

            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate service JWT", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String keyPem) {
        try {
            String cleanKey = keyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA private key", e);
        }
    }

    private static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ofMinutes(5);
        }
        String trimmed = value.trim();
        if (trimmed.matches("^\\d+$")) {
            return Duration.ofSeconds(Long.parseLong(trimmed));
        }
        if (trimmed.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        if (trimmed.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        return Duration.parse(trimmed);
    }
}
