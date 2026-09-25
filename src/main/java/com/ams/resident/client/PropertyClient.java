package com.ams.resident.client;

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
public class PropertyClient {

    private final RestTemplate restTemplate;
    private final ServiceJwtProvider serviceJwtProvider;
    private final String gatewayBaseUrl;

    public PropertyClient(
            RestTemplate restTemplate,
            ServiceJwtProvider serviceJwtProvider,
            @Value("${gateway.base-url:http://localhost:8000}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.serviceJwtProvider = serviceJwtProvider;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public void checkUnitExists(String unitId) {
        String url = gatewayBaseUrl + "/api/v1/internal/units/" + unitId + "/exists";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceJwtProvider.generateToken());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.GET, entity, Void.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("Failed to validate unit against Property service, status: " + response.getStatusCode());
            }
        } catch (RestClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RestClientException("Failed to communicate with Property service via gateway", ex);
        }
    }
}
