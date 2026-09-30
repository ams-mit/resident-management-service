package com.ams.resident.controller;

import com.ams.resident.dto.ApiResponse;
import com.ams.resident.dto.PagedMeta;
import com.ams.resident.dto.RelationshipRequest;
import com.ams.resident.dto.RelationshipResponse;
import com.ams.resident.exception.BadRequestException;
import com.ams.resident.service.RelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "Relationship Management", description = "Endpoints for managing relationships between profiles and units")
@SecurityRequirement(name = "BearerAuth")
public class RelationshipController {

    private final RelationshipService relationshipService;

    @Operation(summary = "Create relationship request", description = "Submit a request to establish a relationship with an apartment unit (e.g. as Owner or Tenant).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Relationship request created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @PostMapping
    public ResponseEntity<ApiResponse<RelationshipResponse>> createRelationshipRequest(@Valid @RequestBody RelationshipRequest request) {
        RelationshipResponse response = relationshipService.createRelationshipRequest(request);
        return new ResponseEntity<>(ApiResponse.of(response), HttpStatus.CREATED);
    }

    @Operation(summary = "Get own relationships", description = "Retrieve all unit relationships associated with the currently authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationships retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RelationshipResponse>>> getOwnRelationships() {
        return ResponseEntity.ok(ApiResponse.of(relationshipService.getOwnRelationships()));
    }

    @Operation(summary = "List relationships (Admin)", description = "List all apartment relationship requests with optional status filtering and pagination. Restricted to SYSTEM_ADMINISTRATOR.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationships retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status or pagination parameters")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<RelationshipResponse>>> getRelationships(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("VALIDATION_ERROR", "Page index must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("VALIDATION_ERROR", "Page size must be between 1 and 100");
        }

        Page<RelationshipResponse> pagedResult = relationshipService.getRelationships(status, PageRequest.of(page, size));
        PagedMeta meta = PagedMeta.builder()
                .page(pagedResult.getNumber())
                .size(pagedResult.getSize())
                .totalElements(pagedResult.getTotalElements())
                .build();

        return ResponseEntity.ok(ApiResponse.of(pagedResult.getContent(), meta));
    }

    @Operation(summary = "Get relationship by ID (Admin)", description = "Retrieve details of a specific apartment relationship request. Restricted to SYSTEM_ADMINISTRATOR.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relationship not found")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<RelationshipResponse>> getRelationshipById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of(relationshipService.getRelationshipById(id)));
    }

    @Operation(summary = "Approve relationship (Admin)", description = "Approve a pending apartment relationship request. Restricted to SYSTEM_ADMINISTRATOR.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship approved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relationship not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Relationship already decided")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<RelationshipResponse>> approveRelationship(@PathVariable String id) {
        RelationshipResponse response = relationshipService.approveRelationship(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Reject relationship (Admin)", description = "Reject a pending apartment relationship request with a mandatory reason. Restricted to SYSTEM_ADMINISTRATOR.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship rejected successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or missing reason")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relationship not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Relationship already decided")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<RelationshipResponse>> rejectRelationship(
            @PathVariable String id,
            @Valid @RequestBody com.ams.resident.dto.RelationshipRejectRequest request) {
        RelationshipResponse response = relationshipService.rejectRelationship(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
