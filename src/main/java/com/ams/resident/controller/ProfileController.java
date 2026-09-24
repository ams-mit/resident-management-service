package com.ams.resident.controller;

import com.ams.resident.dto.EmailChangeRequest;
import com.ams.resident.dto.ProfileRequest;
import com.ams.resident.dto.ProfileResponse;
import com.ams.resident.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/profiles/me")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "Endpoints for residents to manage their own profiles")
@SecurityRequirement(name = "BearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get own profile", description = "Retrieves the profile of the currently authenticated user based on the JWT subject. Requires user token.")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @ApiResponse(responseCode = "404", description = "Profile not found for the user")
    @GetMapping
    public ResponseEntity<ProfileResponse> getOwnProfile() {
        return ResponseEntity.ok(profileService.getOwnProfile());
    }

    @Operation(summary = "Edit own profile", description = "Updates the profile fields for the currently authenticated user. Requires user token.")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    @PutMapping
    public ResponseEntity<ProfileResponse> editOwnProfile(@Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(profileService.editOwnProfile(request));
    }

    @Operation(summary = "Request email change", description = "Initiates a request to change the user's primary email. Triggers Identity event (Simulated). Requires user token.")
    @ApiResponse(responseCode = "202", description = "Email change request accepted")
    @ApiResponse(responseCode = "400", description = "Invalid email format")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    @PostMapping("/email-change")
    public ResponseEntity<Void> requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        profileService.requestEmailChange(request);
        return ResponseEntity.accepted().build();
    }
}
