package com.ams.resident.controller;

import com.ams.resident.dto.ResidentRequest;
import com.ams.resident.dto.ResidentResponse;
import com.ams.resident.security.SecurityUtils;
import com.ams.resident.service.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
@Tag(name = "Resident Management", description = "Administrative endpoints for resident staff operations")
@SecurityRequirement(name = "BearerAuth")
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "Create resident", description = "Create a new resident profile. Required Role: APARTMENT_MANAGER or SYSTEM_ADMIN.")
    @ApiResponse(responseCode = "201", description = "Resident created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request format")
    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @PostMapping
    @PreAuthorize("hasAnyRole('APARTMENT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ResidentResponse> createResident(@Valid @RequestBody ResidentRequest request) {
        ResidentResponse response = residentService.createResident(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all residents", description = "Retrieve a list of all residents. Required Role: APARTMENT_MANAGER or SYSTEM_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Residents retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @GetMapping
    @PreAuthorize("hasAnyRole('APARTMENT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<ResidentResponse>> getResidents() {
        return ResponseEntity.ok(residentService.getAllResidents());
    }

    @Operation(summary = "Get resident by ID", description = "Retrieve a specific resident profile. Accessible by the resident themselves or Administrators.")
    @ApiResponse(responseCode = "200", description = "Resident retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    @ApiResponse(responseCode = "404", description = "Resident not found")
    @GetMapping("/{residentId}")
    public ResponseEntity<ResidentResponse> getResidentById(@PathVariable String residentId) {
        ResidentResponse resident = residentService.getResidentById(residentId);
        
        // Authorization: Admin or Self
        if (!isAdmin() && !resident.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to access this resident profile.");
        }
        
        return ResponseEntity.ok(resident);
    }

    @Operation(summary = "Update resident", description = "Update a specific resident profile. Accessible by the resident themselves or Administrators.")
    @ApiResponse(responseCode = "200", description = "Resident updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    @ApiResponse(responseCode = "404", description = "Resident not found")
    @PutMapping("/{residentId}")
    public ResponseEntity<ResidentResponse> updateResident(@PathVariable String residentId, @Valid @RequestBody ResidentRequest request) {
        ResidentResponse resident = residentService.getResidentById(residentId);
        
        // Authorization: Admin or Self
        if (!isAdmin() && !resident.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to modify this resident profile.");
        }
        
        return ResponseEntity.ok(residentService.updateResident(residentId, request));
    }

    @Operation(summary = "Update resident status", description = "Update the status of a resident. Currently blocked by canonical schema requirements.")
    @ApiResponse(responseCode = "501", description = "Not Implemented")
    @PatchMapping("/{residentId}/status")
    @PreAuthorize("hasAnyRole('APARTMENT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Void> updateResidentStatus(@PathVariable String residentId) {
        // BLOCKED: The approved schema (V1__init_schema.sql) strictly omits a status column for profiles.
        // Inventing a status field or transition rule is expressly prohibited by the requirements.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_APARTMENT_MANAGER") || 
                               a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
    }
}
