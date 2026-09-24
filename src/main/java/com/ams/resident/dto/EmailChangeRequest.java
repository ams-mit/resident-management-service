package com.ams.resident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for changing user email")
public class EmailChangeRequest {
    
    @NotBlank(message = "New email is required")
    @Email(message = "New email must be a valid email format")
    @Schema(description = "The new email address to be associated with the account", example = "new-email@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newEmail;
}
