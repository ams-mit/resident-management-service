package com.ams.resident.controller;

import com.ams.resident.dto.EmailChangeRequest;
import com.ams.resident.dto.ProfileRequest;
import com.ams.resident.dto.ProfileResponse;
import com.ams.resident.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ams.resident.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/profiles/me")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "Endpoints for residents to manage their own profiles")
@SecurityRequirement(name = "BearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get own profile", description = "Retrieves the profile of the currently authenticated user based on the JWT subject. Requires user token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile not found for the user")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getOwnProfile() {
        return ResponseEntity.ok(ApiResponse.of(profileService.getOwnProfile()));
    }

    @Operation(summary = "Edit own profile", description = "Updates the profile fields for the currently authenticated user. Requires user token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile not found")
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> editOwnProfile(@Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.of(profileService.editOwnProfile(request)));
    }

    @Operation(summary = "Request email change", description = "Initiates a request to change the user's primary email. Triggers Identity event (Simulated). Requires user token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Email change request accepted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email format")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @PostMapping("/email-change")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        profileService.requestEmailChange(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED)
                .body(ApiResponse.of(java.util.Map.of("message", "Verification required")));
    }
}
