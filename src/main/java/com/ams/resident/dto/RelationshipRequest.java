package com.ams.resident.dto;

import com.ams.resident.entity.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for creating a new relationship with a unit")
public class RelationshipRequest {
    @NotNull
    @Schema(description = "Type of relationship to establish (e.g. OWNER, TENANT)", example = "TENANT", requiredMode = Schema.RequiredMode.REQUIRED)
    private RelationshipType relationshipType;
    
    @NotBlank
    @Schema(description = "The reference ID of the apartment unit", example = "UNIT-101", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unitReference;
    
    @Schema(description = "Optional supporting information or lease details", example = "Lease agreement signed on 2026-09-01")
    private String supportingInfo;
}
