package com.ams.resident.client;

import com.ams.resident.dto.EmailChangeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class IdentityClient {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public IdentityClient(
            RestTemplate restTemplate,
            @Value("${services.identity.url:http://identity-access-service}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl;
    }

    public void requestEmailChange(String userId, String newEmail) {
        String url = identityServiceUrl + "/api/v1/internal/users/" + userId + "/email";
        
        EmailChangeRequest request = new EmailChangeRequest();
        request.setNewEmail(newEmail);
        
        HttpEntity<EmailChangeRequest> entity = new HttpEntity<>(request);
        
        // This will naturally throw a RestClientException (e.g. 503 or 500) if the service is unreachable.
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to update email in Identity service");
        }
    }
}
