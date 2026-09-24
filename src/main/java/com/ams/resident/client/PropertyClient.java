package com.ams.resident.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PropertyClient {

    private final RestTemplate restTemplate;
    private final String propertyServiceUrl;

    public PropertyClient(
            RestTemplate restTemplate,
            @Value("${services.property.url:http://property-service}") String propertyServiceUrl) {
        this.restTemplate = restTemplate;
        this.propertyServiceUrl = propertyServiceUrl;
    }

    public void checkUnitExists(String unitId) {
        String url = propertyServiceUrl + "/api/v1/internal/units/" + unitId + "/exists";
        
        // This will naturally throw a RestClientException (e.g. 404, 503, 500) if the unit is not found 
        // or the service is unreachable.
        ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to validate unit against Property service");
        }
    }
}
