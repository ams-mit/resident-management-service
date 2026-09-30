package com.ams.resident.dto;

import com.ams.resident.entity.ProfileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response containing profile details")
public class ProfileResponse {
    @Schema(description = "User ID derived from Identity service", example = "usr_123456")
    private String userId;
    
    @Schema(description = "Type of profile", example = "RESIDENT")
    private ProfileType profileType;
    
    @Schema(description = "First name", example = "John")
    private String firstName;
    
    @Schema(description = "Last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "Contact phone number", example = "+1234567890")
    private String phone;
    
    @Schema(description = "Additional contact information", example = "Emergency contact: Jane Doe")
    private String contactInfo;
    
    @Schema(description = "Current status information", example = "ACTIVE")
    private String statusInfo;
}
