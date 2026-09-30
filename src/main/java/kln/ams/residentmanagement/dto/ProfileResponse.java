package kln.ams.residentmanagement.dto;

import kln.ams.residentmanagement.entity.Profile;
import kln.ams.residentmanagement.entity.ProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private ProfileStatus status;

    public static ProfileResponse fromEntity(Profile profile) {
        if (profile == null) {
            return null;
        }
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .status(profile.getStatus())
                .build();
    }
}
