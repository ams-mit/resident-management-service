package kln.ams.residentmanagement.service;

import kln.ams.residentmanagement.exception.DependencyUnavailableException;
import kln.ams.residentmanagement.exception.ResourceNotFoundException;
import kln.ams.residentmanagement.security.ServiceJwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ServiceJwtProvider serviceJwtProvider;

    private IdentityClient identityClient;

    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        identityClient = new IdentityClient(restTemplate, serviceJwtProvider, "http://localhost:8000");
    }

    @Test
    @DisplayName("validateUser success when service returns 200")
    void testValidateUser_Success() {
        when(serviceJwtProvider.generateToken()).thenReturn("mock-service-jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"success\":true}", HttpStatus.OK));

        assertDoesNotThrow(() -> identityClient.validateUser(USER_ID));
    }

    @Test
    @DisplayName("validateUser throws ResourceNotFoundException on 404")
    void testValidateUser_NotFound() {
        when(serviceJwtProvider.generateToken()).thenReturn("mock-service-jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "User Not Found"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> identityClient.validateUser(USER_ID));
        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("validateUser throws DependencyUnavailableException on 503")
    void testValidateUser_ServiceUnavailable() {
        when(serviceJwtProvider.generateToken()).thenReturn("mock-service-jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        DependencyUnavailableException ex = assertThrows(DependencyUnavailableException.class,
                () -> identityClient.validateUser(USER_ID));
        assertEquals("DEPENDENCY_UNAVAILABLE", ex.getCode());
        assertEquals("identity-access-service", ex.getServiceName());
    }

    @Test
    @DisplayName("validateUser throws DependencyUnavailableException on connection timeout/refused")
    void testValidateUser_NetworkError() {
        when(serviceJwtProvider.generateToken()).thenReturn("mock-service-jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        DependencyUnavailableException ex = assertThrows(DependencyUnavailableException.class,
                () -> identityClient.validateUser(USER_ID));
        assertEquals("DEPENDENCY_UNAVAILABLE", ex.getCode());
        assertEquals("identity-access-service", ex.getServiceName());
    }
}
