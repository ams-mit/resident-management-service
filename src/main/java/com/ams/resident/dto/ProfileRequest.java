package com.ams.resident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for updating an existing profile")
public class ProfileRequest {
    @NotBlank
    @Schema(description = "Resident's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    
    @NotBlank
    @Schema(description = "Resident's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
    
    @Schema(description = "Resident's full name", example = "John Doe")
    private String fullName;
    
    @Schema(description = "Resident's contact phone number", example = "+1234567890")
    private String phone;
}
