package com.ams.resident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for creating or updating a resident profile administratively")
public class ResidentRequest {
    
    @NotBlank(message = "userId is required")
    @Schema(description = "User ID assigned by Identity service", example = "usr_123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;
    
    @NotBlank(message = "firstName is required")
    @Schema(description = "First name of the resident", example = "Jane", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    
    @NotBlank(message = "lastName is required")
    @Schema(description = "Last name of the resident", example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
    
    @Schema(description = "Contact phone number", example = "+1987654321")
    private String phone;
    
    @Schema(description = "Emergency contact details", example = "John Smith (+1122334455)")
    private String emergencyContact;
}
