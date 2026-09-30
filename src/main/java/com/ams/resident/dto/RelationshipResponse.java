package com.ams.resident.dto;

import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.entity.RelationshipType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing relationship details")
public class RelationshipResponse {

    @Schema(description = "Unique identifier of the relationship", example = "rel_abcdef")
    private String relationshipId;

    @Schema(description = "User ID of the requester", example = "usr_123")
    private String requesterUserId;

    @Schema(description = "Type of relationship", example = "TENANT_RESIDENT")
    private RelationshipType relationshipType;

    @Schema(description = "The reference ID of the apartment unit", example = "UNIT-101")
    private String unitReference;

    @Schema(description = "Supporting information or documentation notes", example = "Signed lease document attached")
    private String supportingInfo;

    @Schema(description = "Current status of the relationship request", example = "PENDING")
    private RelationshipStatus status;

    @Schema(description = "Reason for decision (if rejected)", example = "Invalid lease document")
    private String decisionReason;

    @Schema(description = "User ID of administrator who made the decision", example = "admin_456")
    private String decidedBy;

    @Schema(description = "Timestamp when the decision was made")
    private LocalDateTime decidedAt;

    @Schema(description = "Timestamp when the relationship request was created")
    private LocalDateTime createdAt;

    @Schema(description = "Unit validation details placeholder")
    private UnitValidationResponse unitValidation;
}
