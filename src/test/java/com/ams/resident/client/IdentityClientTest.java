package com.ams.resident.client;

import com.ams.resident.exception.ConflictException;
import com.ams.resident.exception.DependencyUnavailableException;
import com.ams.resident.security.ServiceJwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ServiceJwtProvider serviceJwtProvider;

    private IdentityClient identityClient;
    private final String gatewayBaseUrl = "http://api-gateway:8000";

    @BeforeEach
    void setUp() {
        identityClient = new IdentityClient(restTemplate, serviceJwtProvider, gatewayBaseUrl);
    }

    @Test
    void shouldCallGatewayWithBearerTokenAndExactPath() {
        when(serviceJwtProvider.generateToken()).thenReturn("mocked.service.jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        identityClient.updateUserEmail("user-123", "new@example.com");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.PUT), entityCaptor.capture(), eq(Void.class));

        assertEquals(gatewayBaseUrl + "/internal/v1/users/user-123/email", urlCaptor.getValue());
        assertEquals("Bearer mocked.service.jwt", entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldMapConflictToConflictException() {
        when(serviceJwtProvider.generateToken()).thenReturn("mocked.service.jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT, "Conflict"));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                identityClient.updateUserEmail("user-123", "new@example.com"));
        assertEquals("EMAIL_ALREADY_IN_USE", ex.getCode());
    }

    @Test
    void shouldMapUnauthorizedToDependencyUnavailable() {
        when(serviceJwtProvider.generateToken()).thenReturn("mocked.service.jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        DependencyUnavailableException ex = assertThrows(DependencyUnavailableException.class, () ->
                identityClient.updateUserEmail("user-123", "new@example.com"));
        assertEquals("DEPENDENCY_UNAVAILABLE", ex.getCode());
    }

    @Test
    void shouldMapNetworkFailureToDependencyUnavailable() {
        when(serviceJwtProvider.generateToken()).thenReturn("mocked.service.jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        DependencyUnavailableException ex = assertThrows(DependencyUnavailableException.class, () ->
                identityClient.updateUserEmail("user-123", "new@example.com"));
        assertEquals("DEPENDENCY_UNAVAILABLE", ex.getCode());
    }
}
