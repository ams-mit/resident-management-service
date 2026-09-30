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
import com.ams.resident.dto.ApiResponse;
import com.ams.resident.dto.PagedMeta;
import com.ams.resident.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
@Tag(name = "Resident Management", description = "Administrative endpoints for resident staff operations")
@SecurityRequirement(name = "BearerAuth")
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "Create resident", description = "Create a new resident profile. Required Role: APARTMENT_MANAGER or SYSTEM_ADMIN.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Resident created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request format")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @PostMapping
    @PreAuthorize("hasAnyRole('APARTMENT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ResidentResponse>> createResident(@Valid @RequestBody ResidentRequest request) {
        ResidentResponse response = residentService.createResident(request);
        return new ResponseEntity<>(ApiResponse.of(response), HttpStatus.CREATED);
    }

    @Operation(summary = "Get all residents", description = "Retrieve a list of all residents with pagination. Required Role: APARTMENT_MANAGER or SYSTEM_ADMIN.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Residents retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    @GetMapping
    @PreAuthorize("hasAnyRole('APARTMENT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getResidents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("VALIDATION_ERROR", "Page index must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("VALIDATION_ERROR", "Page size must be between 1 and 100");
        }

        Page<ResidentResponse> pagedResidents = residentService.getAllResidents(PageRequest.of(page, size));
        if (pagedResidents == null) {
            pagedResidents = Page.empty(PageRequest.of(page, size));
        }

        PagedMeta meta = PagedMeta.builder()
                .page(pagedResidents.getNumber())
                .size(pagedResidents.getSize())
                .totalElements(pagedResidents.getTotalElements())
                .build();

        return ResponseEntity.ok(ApiResponse.of(pagedResidents.getContent(), meta));
    }

    @Operation(summary = "Get resident by ID", description = "Retrieve a specific resident profile. Accessible by the resident themselves or Administrators.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resident retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Resident not found")
    @GetMapping("/{residentId}")
    public ResponseEntity<ApiResponse<ResidentResponse>> getResidentById(@PathVariable String residentId) {
        ResidentResponse resident = residentService.getResidentById(residentId);
        
        // Authorization: Admin or Self
        if (!isAdmin() && !resident.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to access this resident profile.");
        }
        
        return ResponseEntity.ok(ApiResponse.of(resident));
    }

    @Operation(summary = "Update resident", description = "Update a specific resident profile. Accessible by the resident themselves or Administrators.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resident updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Resident not found")
    @PutMapping("/{residentId}")
    public ResponseEntity<ApiResponse<ResidentResponse>> updateResident(@PathVariable String residentId, @Valid @RequestBody ResidentRequest request) {
        ResidentResponse resident = residentService.getResidentById(residentId);
        
        // Authorization: Admin or Self
        if (!isAdmin() && !resident.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to modify this resident profile.");
        }
        
        return ResponseEntity.ok(ApiResponse.of(residentService.updateResident(residentId, request)));
    }

    @Operation(summary = "Update resident status", description = "Update the status of a resident. Currently blocked by canonical schema requirements.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "Not Implemented")
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
