package com.ams.resident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for confirming email change")
public class EmailChangeConfirmRequest {

    @NotBlank(message = "Verification token is required")
    @Schema(description = "The verification token received for confirming email change", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verificationToken;
}
