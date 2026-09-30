package com.ams.resident.controller;

import com.ams.resident.dto.ApiResponse;
import com.ams.resident.dto.RelationshipValidationResponse;
import com.ams.resident.service.RelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "Internal Relationship Operations", description = "Endpoints for inter-service communication and verification")
@SecurityRequirement(name = "BearerAuth")
public class RelationshipInternalController {

    private final RelationshipService relationshipService;

    @Operation(summary = "Validate relationship", description = "Validates if a user has an approved relationship with a unit. Accessible by allowed services only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship validation completed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid query parameters")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid service token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Caller service not in allow-list")
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<RelationshipValidationResponse>> validateRelationship(
            @RequestParam(name = "userId") String userId,
            @RequestParam(name = "unitReference") String unitReference,
            @RequestParam(name = "relationshipType") String relationshipType) {

        RelationshipValidationResponse response = relationshipService.validateRelationship(userId, unitReference, relationshipType);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
