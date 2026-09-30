package kln.ams.residentmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kln.ams.residentmanagement.dto.*;
import kln.ams.residentmanagement.entity.ProfileStatus;
import kln.ams.residentmanagement.entity.ProfileType;
import kln.ams.residentmanagement.exception.ApiErrorResponse;
import kln.ams.residentmanagement.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
@Tag(name = "Residents", description = "Resident profile management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class ResidentController {

    private final ProfileService profileService;

    @Operation(summary = "RES-001: List resident profiles", description = "Retrieve paginated resident profiles. Restricted to SYSTEM_ADMINISTRATOR and APARTMENT_MANAGER.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Residents retrieved successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<PagedData<ProfileResponse>>> listResidents(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Profile status filter") @RequestParam(name = "status", required = false) ProfileStatus status,
            @Parameter(description = "Search across name and email") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Filter by userId") @RequestParam(name = "userId", required = false) String userId) {

        PagedData<ProfileResponse> result = profileService.listProfiles(
                ProfileType.RESIDENT, status, search, userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.of(result, "Residents retrieved successfully"));
    }

    @Operation(summary = "RES-002: Create resident profile", description = "Creates a new resident profile. Validates user existence with identity-access-service.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Resident profile created successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found in Identity Access",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Resident profile already exists for this user",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Identity service unavailable",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> createResident(@Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = profileService.createProfile(ProfileType.RESIDENT, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Resident profile created successfully"));
    }

    @Operation(summary = "RES-003: Get resident profile", description = "Retrieve a specific resident profile. Allowed for SYSTEM_ADMINISTRATOR, APARTMENT_MANAGER, or the resident user themselves.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resident retrieved successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Resident not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{residentId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getResident(@PathVariable("residentId") String residentId) {
        ProfileResponse response = profileService.getProfileById(residentId, ProfileType.RESIDENT);
        return ResponseEntity.ok(ApiResponse.of(response, "Resident retrieved successfully"));
    }

    @Operation(summary = "RES-004: Update resident profile", description = "Update resident profile fields. Allowed for SYSTEM_ADMINISTRATOR, APARTMENT_MANAGER, or self-service update.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resident profile updated successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Resident not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{residentId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateResident(
            @PathVariable("residentId") String residentId,
            @Valid @RequestBody UpdateResidentRequest request) {
        ProfileResponse response = profileService.updateResident(residentId, request);
        return ResponseEntity.ok(ApiResponse.of(response, "Resident profile updated successfully"));
    }
}
