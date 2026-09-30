package kln.ams.residentmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentValidationResponse {

    private String residentId;
    private String userId;
    private boolean exists;
    private boolean active;
}
