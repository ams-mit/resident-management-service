package com.ams.resident.dto;

import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.entity.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response containing relationship details")
public class RelationshipResponse {
    @Schema(description = "Unique identifier of the relationship", example = "rel_abcdef")
    private String relationshipId;
    
    @Schema(description = "Type of relationship", example = "TENANT")
    private RelationshipType relationshipType;
    
    @Schema(description = "The reference ID of the apartment unit", example = "UNIT-101")
    private String unitReference;
    
    @Schema(description = "Current status of the relationship request", example = "PENDING")
    private RelationshipStatus status;
    
    @Schema(description = "Reason for decision (if approved or rejected)", example = "Verified lease agreement")
    private String decisionReason;
}
