package com.ams.resident.client;

import com.ams.resident.security.ServiceJwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
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
    void shouldCallGatewayWithBearerToken() {
        when(serviceJwtProvider.generateToken()).thenReturn("mocked.service.jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        identityClient.requestEmailChange("user-123", "new@example.com");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.PUT), entityCaptor.capture(), eq(Void.class));

        assertTrue(urlCaptor.getValue().startsWith(gatewayBaseUrl + "/api/v1/internal/users/user-123/email"));
        assertEquals("Bearer mocked.service.jwt", entityCaptor.getValue().getHeaders().getFirst("Authorization"));
    }

    @Test
    void shouldMapFailureToRestClientException() {
        when(serviceJwtProvider.generateToken()).thenReturn("mocked.service.jwt");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RestClientException.class, () ->
                identityClient.requestEmailChange("user-123", "new@example.com"));
    }
}
