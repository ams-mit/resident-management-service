package com.ams.resident.service;

import com.ams.resident.client.PropertyClient;
import com.ams.resident.dto.RelationshipRequest;
import com.ams.resident.dto.RelationshipResponse;
import com.ams.resident.entity.ApartmentRelationship;
import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.repository.ApartmentRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelationshipService {

    private final ApartmentRelationshipRepository relationshipRepository;
    private final AuditService auditService;
    private final PropertyClient propertyClient;

    @Transactional
    public RelationshipResponse createRelationshipRequest(RelationshipRequest request) {
        String userId = getAuthenticatedUserId();

        ApartmentRelationship relationship = new ApartmentRelationship();
        relationship.setUserId(userId);
        relationship.setRelationshipType(request.getRelationshipType());
        relationship.setUnitReference(request.getUnitReference());
        relationship.setSupportingInfo(request.getSupportingInfo());
        relationship.setStatus(RelationshipStatus.PENDING);

        relationship = relationshipRepository.save(relationship);
        auditService.logEvent("FR-AUD-004", userId, "Submitted relationship request for unit " + request.getUnitReference());

        return mapToResponse(relationship);
    }

    public List<RelationshipResponse> getOwnRelationships() {
        String userId = getAuthenticatedUserId();
        return relationshipRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String getAuthenticatedUserId() {
        return com.ams.resident.security.SecurityUtils.getCurrentUserId();
    }
    
    private RelationshipResponse mapToResponse(ApartmentRelationship relationship) {
        RelationshipResponse response = new RelationshipResponse();
        response.setRelationshipId(relationship.getId());
        response.setRelationshipType(relationship.getRelationshipType());
        response.setUnitReference(relationship.getUnitReference());
        response.setStatus(relationship.getStatus());
        response.setDecisionReason(relationship.getDecisionReason());
        return response;
    }
}
