package com.ams.resident.dto;

import com.ams.resident.entity.ProfileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response containing administrative resident profile details")
public class ResidentResponse {
    @Schema(description = "Internal database ID of the resident profile", example = "a1b2c3d4-e5f6-7890")
    private String id;
    
    @Schema(description = "User ID assigned by Identity service", example = "usr_123456")
    private String userId;
    
    @Schema(description = "Type of profile", example = "RESIDENT")
    private ProfileType profileType;
    
    @Schema(description = "First name", example = "Jane")
    private String firstName;
    
    @Schema(description = "Last name", example = "Smith")
    private String lastName;
    
    @Schema(description = "Contact phone number", example = "+1987654321")
    private String phone;
    
    @Schema(description = "Emergency contact details", example = "John Smith (+1122334455)")
    private String emergencyContact;
}
