package com.ams.resident.client;

import com.ams.resident.dto.EmailChangeRequest;
import com.ams.resident.security.ServiceJwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class IdentityClient {

    private final RestTemplate restTemplate;
    private final ServiceJwtProvider serviceJwtProvider;
    private final String gatewayBaseUrl;

    public IdentityClient(
            RestTemplate restTemplate,
            ServiceJwtProvider serviceJwtProvider,
            @Value("${gateway.base-url:http://localhost:8000}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.serviceJwtProvider = serviceJwtProvider;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public void requestEmailChange(String userId, String newEmail) {
        String url = gatewayBaseUrl + "/api/v1/internal/users/" + userId + "/email";
        
        EmailChangeRequest request = new EmailChangeRequest();
        request.setNewEmail(newEmail);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceJwtProvider.generateToken());
        
        HttpEntity<EmailChangeRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("Failed to update email in Identity service, status: " + response.getStatusCode());
            }
        } catch (RestClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RestClientException("Failed to communicate with Identity service via gateway", ex);
        }
    }
}
