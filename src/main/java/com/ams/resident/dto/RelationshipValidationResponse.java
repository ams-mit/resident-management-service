package com.ams.resident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation response for internal relationship checks")
public class RelationshipValidationResponse {

    @Schema(description = "Indicates whether the relationship is verified and approved", example = "true")
    private boolean verified;

    @Schema(description = "The relationship type queried", example = "TENANT_RESIDENT")
    private String relationshipType;

    @Schema(description = "Current status of the relationship (APPROVED, PENDING, REJECTED, or NONE)", example = "APPROVED")
    private String status;
}
