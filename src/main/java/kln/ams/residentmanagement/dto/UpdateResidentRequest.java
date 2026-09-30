package kln.ams.residentmanagement.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResidentRequest {

    private String firstName;
    private String lastName;

    @Email(message = "email must be a valid email address")
    private String email;

    private String phone;
}
