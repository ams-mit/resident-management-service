package kln.ams.residentmanagement.dto;

import kln.ams.residentmanagement.entity.ProfileStatus;
import kln.ams.residentmanagement.entity.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationshipsResponse {

    private String userId;
    private List<RelationshipItem> relationships;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipItem {
        private String profileId;
        private ProfileType profileType;
        private ProfileStatus status;
    }
}
