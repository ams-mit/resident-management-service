package kln.ams.residentmanagement.service;

import kln.ams.residentmanagement.exception.DependencyUnavailableException;
import kln.ams.residentmanagement.exception.ResourceNotFoundException;
import kln.ams.residentmanagement.security.RequestIdContext;
import kln.ams.residentmanagement.security.ServiceJwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
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

    public void validateUser(String userId) {
        String url = gatewayBaseUrl + "/api/v1/internal/users/" + userId + "/validate";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set(RequestIdContext.getRequestIdHeaderName(), RequestIdContext.getRequestId());
        try {
            headers.setBearerAuth(serviceJwtProvider.generateToken());
        } catch (Exception e) {
            log.error("Failed to generate service token for IdentityClient: {}", e.getMessage());
            throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "identity-access-service",
                    "Failed to generate service token for identity-access-service");
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "identity-access-service",
                        "Identity service returned non-success status: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("User {} not found in Identity Access", userId);
                throw new ResourceNotFoundException("USER_NOT_FOUND", "Referenced user does not exist in Identity Access");
            } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Identity service authorization error: {}", ex.getStatusCode());
                throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "identity-access-service",
                        "Identity service rejected service credentials");
            } else {
                log.warn("Identity service error: status={}", ex.getStatusCode());
                throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "identity-access-service",
                        "Identity service is temporarily unavailable");
            }
        } catch (RestClientException ex) {
            log.warn("Failed to communicate with Identity service: {}", ex.getMessage());
            throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE", "identity-access-service",
                    "Identity service is temporarily unavailable");
        }
    }
}
