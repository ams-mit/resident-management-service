package com.ams.resident.controller;

import com.ams.resident.dto.RelationshipRequest;
import com.ams.resident.dto.RelationshipResponse;
import com.ams.resident.service.RelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "Relationship Management", description = "Endpoints for managing relationships between profiles and units")
@SecurityRequirement(name = "BearerAuth")
public class RelationshipController {

    private final RelationshipService relationshipService;

    @Operation(summary = "Create relationship request", description = "Submit a request to establish a relationship with an apartment unit (e.g. as Owner or Tenant).")
    @ApiResponse(responseCode = "201", description = "Relationship request created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @PostMapping
    public ResponseEntity<RelationshipResponse> createRelationshipRequest(@Valid @RequestBody RelationshipRequest request) {
        RelationshipResponse response = relationshipService.createRelationshipRequest(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get own relationships", description = "Retrieve all unit relationships associated with the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "Relationships retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @GetMapping("/me")
    public ResponseEntity<List<RelationshipResponse>> getOwnRelationships() {
        return ResponseEntity.ok(relationshipService.getOwnRelationships());
    }
}
