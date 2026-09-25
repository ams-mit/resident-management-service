package com.ams.resident.security;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ServiceJwtProviderTest {

    private KeyPair keyPair;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a throwaway RSA key pair at test runtime - no key files in repo
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        this.keyPair = kpg.generateKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
    }

    @Test
    void shouldGenerateServiceJwtWithExactClaimsAndValidSignature() throws Exception {
        String serviceName = "resident-management-service";
        ServiceJwtProvider provider = new ServiceJwtProvider(serviceName, privateKey, Duration.ofMinutes(5));

        String token = provider.generateToken();
        assertNotNull(token);

        SignedJWT signedJWT = SignedJWT.parse(token);

        // Verify signature with matching public key
        RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier), "JWT signature must verify with matching public key");

        // Verify claims: exactly sub, type=service, iat, exp
        Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
        assertEquals(Set.of("sub", "type", "iat", "exp"), claims.keySet(),
                "Token must contain exactly sub, type, iat, exp claims");

        assertEquals(serviceName, signedJWT.getJWTClaimsSet().getSubject());
        assertEquals("service", signedJWT.getJWTClaimsSet().getStringClaim("type"));

        Date issueTime = signedJWT.getJWTClaimsSet().getIssueTime();
        Date expTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        assertNotNull(issueTime, "iat must not be null");
        assertNotNull(expTime, "exp must not be null");
        assertTrue(expTime.after(issueTime), "exp must be after iat");

        long diffSeconds = (expTime.getTime() - issueTime.getTime()) / 1000;
        assertEquals(300, diffSeconds, "exp must be 5 minutes (300s) after iat");
    }

    @Test
    void shouldInitializeWithPemKeyString() throws Exception {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----";

        ServiceJwtProvider provider = new ServiceJwtProvider("resident-management-service", pem, "5m");
        String token = provider.generateToken();

        SignedJWT signedJWT = SignedJWT.parse(token);
        RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier));
        assertEquals("resident-management-service", signedJWT.getJWTClaimsSet().getSubject());
        assertEquals("service", signedJWT.getJWTClaimsSet().getStringClaim("type"));
    }
}
