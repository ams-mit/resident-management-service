package kln.ams.residentmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kln.ams.residentmanagement.dto.ApiResponse;
import kln.ams.residentmanagement.dto.CreateProfileRequest;
import kln.ams.residentmanagement.dto.PagedData;
import kln.ams.residentmanagement.dto.ProfileResponse;
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
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Staff profile management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class StaffController {

    private final ProfileService profileService;

    @Operation(summary = "STF-001: List staff profiles", description = "Retrieve paginated staff profiles. Restricted to SYSTEM_ADMINISTRATOR and APARTMENT_MANAGER.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Staff retrieved successfully",
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
    public ResponseEntity<ApiResponse<PagedData<ProfileResponse>>> listStaff(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Profile status filter") @RequestParam(name = "status", required = false) ProfileStatus status,
            @Parameter(description = "Search across name and email") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Filter by userId") @RequestParam(name = "userId", required = false) String userId) {

        PagedData<ProfileResponse> result = profileService.listProfiles(
                ProfileType.STAFF, status, search, userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.of(result, "Staff retrieved successfully"));
    }

    @Operation(summary = "STF-002: Create staff profile", description = "Creates a new staff profile. Validates user existence with identity-access-service.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Staff profile created successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found in Identity Access",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Staff profile already exists for this user",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Identity service unavailable",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> createStaff(@Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = profileService.createProfile(ProfileType.STAFF, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Staff profile created successfully"));
    }
}
