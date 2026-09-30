package kln.ams.residentmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kln.ams.residentmanagement.dto.ApiResponse;
import kln.ams.residentmanagement.dto.ResidentValidationResponse;
import kln.ams.residentmanagement.dto.UserRelationshipsResponse;
import kln.ams.residentmanagement.exception.ApiErrorResponse;
import kln.ams.residentmanagement.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Tag(name = "Internal APIs", description = "Internal cross-service provider APIs for resident validation and user relationships")
@SecurityRequirement(name = "BearerAuth")
public class ResidentInternalController {

    private final ProfileService profileService;

    @Operation(summary = "RES-INT-001: Validate resident profile", description = "Validates that a resident profile exists and is active. Accessible by registered backend services only.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resident validation successful",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid service token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller service not allowed",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Resident not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/residents/{residentId}/validate")
    public ResponseEntity<ApiResponse<ResidentValidationResponse>> validateResident(
            @PathVariable("residentId") String residentId) {
        ResidentValidationResponse response = profileService.validateResident(residentId);
        return ResponseEntity.ok(ApiResponse.of(response, "Resident validation successful"));
    }

    @Operation(summary = "RES-INT-002: Get user relationships", description = "Retrieves all resident-domain relationships owned by Resident Management for a user. Accessible by registered backend services only.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User relationships retrieved successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid service token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller service not allowed",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/users/{userId}/relationships")
    public ResponseEntity<ApiResponse<UserRelationshipsResponse>> getUserRelationships(
            @PathVariable("userId") String userId) {
        UserRelationshipsResponse response = profileService.getUserRelationships(userId);
        return ResponseEntity.ok(ApiResponse.of(response, "User relationships retrieved successfully"));
    }
}
