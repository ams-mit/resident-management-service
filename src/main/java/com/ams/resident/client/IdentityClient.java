package com.ams.resident.client;

import com.ams.resident.dto.EmailChangeRequest;
import com.ams.resident.exception.ConflictException;
import com.ams.resident.exception.DependencyUnavailableException;
import com.ams.resident.security.ServiceJwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
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

    public void updateUserEmail(String userId, String newEmail) {
        String url = gatewayBaseUrl + "/internal/v1/users/" + userId + "/email";

        EmailChangeRequest request = new EmailChangeRequest();
        request.setNewEmail(newEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serviceJwtProvider.generateToken());

        HttpEntity<EmailChangeRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "Failed to update email in Identity service, status: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ConflictException("EMAIL_ALREADY_IN_USE", "Email already in use");
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Identity service rejected service JWT with status: {}", ex.getStatusCode());
                throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "Identity service authentication failed");
            } else {
                log.warn("Identity service returned error status: {}", ex.getStatusCode());
                throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "Identity service returned error: " + ex.getStatusCode());
            }
        } catch (ResourceAccessException ex) {
            log.warn("Identity service connection error: {}", ex.getMessage());
            throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "Identity service unreachable");
        } catch (ConflictException | DependencyUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Unexpected error communicating with Identity service: {}", ex.getMessage());
            throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "Failed to communicate with Identity service");
        }
    }
}
