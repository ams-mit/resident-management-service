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
@Schema(description = "Unit validation details placeholder")
public class UnitValidationResponse {

    @Schema(description = "Validation status", example = "NOT_AVAILABLE")
    private String status;

    @Schema(description = "Validation status reason", example = "Group 2 unit/occupancy contract not yet agreed")
    private String reason;
}
