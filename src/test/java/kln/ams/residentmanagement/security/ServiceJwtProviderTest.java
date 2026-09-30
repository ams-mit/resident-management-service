package kln.ams.residentmanagement.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ServiceJwtProviderTest {

    @Test
    @DisplayName("Generate service JWT with RS256, typ=JWT, and required claims")
    void testGenerateToken_Success() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        ServiceJwtProvider provider = new ServiceJwtProvider("resident-management-service", privateKey, Duration.ofMinutes(5));

        String token = provider.generateToken();
        assertNotNull(token);

        SignedJWT signedJWT = SignedJWT.parse(token);

        // Header verification
        assertEquals(JWSAlgorithm.RS256, signedJWT.getHeader().getAlgorithm());
        assertNotNull(signedJWT.getHeader().getType());
        assertEquals("JWT", signedJWT.getHeader().getType().toString());

        // Claims verification
        assertEquals("resident-management-service", signedJWT.getJWTClaimsSet().getSubject());
        assertEquals("service", signedJWT.getJWTClaimsSet().getStringClaim("type"));
        assertNotNull(signedJWT.getJWTClaimsSet().getIssueTime());
        assertNotNull(signedJWT.getJWTClaimsSet().getExpirationTime());

        // Standard explicitly says NO iss claim
        assertNull(signedJWT.getJWTClaimsSet().getIssuer());
    }

    @Test
    @DisplayName("Throws IllegalStateException when private key is not configured")
    void testGenerateToken_MissingKey() {
        ServiceJwtProvider provider = new ServiceJwtProvider("resident-management-service", "", "5m");
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::generateToken);
        assertTrue(ex.getMessage().contains("SERVICE_JWT_PRIVATE_KEY is not configured"));
    }
}
