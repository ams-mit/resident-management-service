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
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant/Resident profile management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class TenantController {

    private final ProfileService profileService;

    @Operation(summary = "TEN-001: List tenant profiles", description = "Retrieve paginated tenant/resident profiles. Restricted to SYSTEM_ADMINISTRATOR and APARTMENT_MANAGER.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenants retrieved successfully",
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
    public ResponseEntity<ApiResponse<PagedData<ProfileResponse>>> listTenants(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Profile status filter") @RequestParam(name = "status", required = false) ProfileStatus status,
            @Parameter(description = "Search across name and email") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Filter by userId") @RequestParam(name = "userId", required = false) String userId) {

        PagedData<ProfileResponse> result = profileService.listProfiles(
                ProfileType.TENANT, status, search, userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.of(result, "Tenants retrieved successfully"));
    }

    @Operation(summary = "TEN-002: Create tenant profile", description = "Creates a new tenant/resident profile. Validates user existence with identity-access-service.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant profile created successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found in Identity Access",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tenant profile already exists for this user",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Identity service unavailable",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> createTenant(@Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = profileService.createProfile(ProfileType.TENANT, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Tenant profile created successfully"));
    }

    @Operation(summary = "TEN-003: Get tenant profile", description = "Retrieve a specific tenant/resident profile. Allowed for SYSTEM_ADMINISTRATOR, APARTMENT_MANAGER, or the tenant user themselves.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant retrieved successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getTenant(@PathVariable("tenantId") String tenantId) {
        ProfileResponse response = profileService.getProfileById(tenantId, ProfileType.TENANT);
        return ResponseEntity.ok(ApiResponse.of(response, "Tenant retrieved successfully"));
    }
}
