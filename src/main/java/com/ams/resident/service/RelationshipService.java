package com.ams.resident.service;

import com.ams.resident.client.PropertyClient;
import com.ams.resident.dto.RelationshipRequest;
import com.ams.resident.dto.RelationshipResponse;
import com.ams.resident.dto.UnitValidationResponse;
import com.ams.resident.entity.ApartmentRelationship;
import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.exception.BadRequestException;
import com.ams.resident.exception.ResourceNotFoundException;
import com.ams.resident.repository.ApartmentRelationshipRepository;
import com.ams.resident.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        auditService.logEvent("FR-AUD-004", "RELATIONSHIP", relationship.getId(), userId, "Submitted relationship request for unit " + request.getUnitReference());

        return mapToResponse(relationship, false);
    }

    public List<RelationshipResponse> getOwnRelationships() {
        String userId = getAuthenticatedUserId();
        return relationshipRepository.findByUserId(userId).stream()
                .map(rel -> mapToResponse(rel, false))
                .collect(Collectors.toList());
    }

    public Page<RelationshipResponse> getRelationships(String status, Pageable pageable) {
        Page<ApartmentRelationship> page;
        if (StringUtils.hasText(status)) {
            RelationshipStatus relStatus;
            try {
                relStatus = RelationshipStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("VALIDATION_ERROR", "Invalid status: " + status + ". Allowed values: PENDING, APPROVED, REJECTED");
            }
            page = relationshipRepository.findByStatus(relStatus, pageable);
        } else {
            page = relationshipRepository.findAll(pageable);
        }

        return page.map(rel -> mapToResponse(rel, false));
    }

    public RelationshipResponse getRelationshipById(String id) {
        ApartmentRelationship relationship = relationshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RELATIONSHIP_NOT_FOUND", "Relationship not found with id: " + id));

        RelationshipResponse response = mapToResponse(relationship, true);
        // TODO: Group 2 unit/occupancy contract not yet agreed. Do NOT call PropertyClient here.
        response.setUnitValidation(new UnitValidationResponse("NOT_AVAILABLE", "Group 2 unit/occupancy contract not yet agreed"));
        return response;
    }

    @Transactional
    public RelationshipResponse approveRelationship(String id) {
        ApartmentRelationship relationship = relationshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RELATIONSHIP_NOT_FOUND", "Relationship not found with id: " + id));

        if (relationship.getStatus() != RelationshipStatus.PENDING) {
            throw new com.ams.resident.exception.ConflictException("RELATIONSHIP_ALREADY_DECIDED", "Relationship request has already been decided");
        }

        String adminUserId = getAuthenticatedUserId();
        relationship.setStatus(RelationshipStatus.APPROVED);
        relationship.setDecidedBy(adminUserId);
        relationship.setDecidedAt(java.time.LocalDateTime.now());
        relationship = relationshipRepository.save(relationship);

        auditService.logEvent("RELATIONSHIP_APPROVED", "RELATIONSHIP", relationship.getId(), adminUserId, "FR-AUD-005: Approved relationship request for unit " + relationship.getUnitReference());

        // TODO: Send notification to user regarding relationship decision once notification ownership is finalized (D8).

        return mapToResponse(relationship, true);
    }

    @Transactional
    public RelationshipResponse rejectRelationship(String id, String reason) {
        ApartmentRelationship relationship = relationshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RELATIONSHIP_NOT_FOUND", "Relationship not found with id: " + id));

        if (relationship.getStatus() != RelationshipStatus.PENDING) {
            throw new com.ams.resident.exception.ConflictException("RELATIONSHIP_ALREADY_DECIDED", "Relationship request has already been decided");
        }

        String adminUserId = getAuthenticatedUserId();
        relationship.setStatus(RelationshipStatus.REJECTED);
        relationship.setDecisionReason(reason);
        relationship.setDecidedBy(adminUserId);
        relationship.setDecidedAt(java.time.LocalDateTime.now());
        relationship = relationshipRepository.save(relationship);

        auditService.logEvent("RELATIONSHIP_REJECTED", "RELATIONSHIP", relationship.getId(), adminUserId, "FR-AUD-005: Rejected relationship request for unit " + relationship.getUnitReference() + ": " + reason);

        // TODO: Send notification to user regarding relationship decision once notification ownership is finalized (D8).

        return mapToResponse(relationship, true);
    }

    public com.ams.resident.dto.RelationshipValidationResponse validateRelationship(String userId, String unitReference, String relationshipTypeStr) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(unitReference) || !StringUtils.hasText(relationshipTypeStr)) {
            throw new BadRequestException("VALIDATION_ERROR", "userId, unitReference, and relationshipType are required");
        }

        RelationshipType relationshipType;
        try {
            relationshipType = RelationshipType.valueOf(relationshipTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("VALIDATION_ERROR", "Invalid relationshipType: " + relationshipTypeStr);
        }

        List<ApartmentRelationship> relationships = relationshipRepository
                .findByUserIdAndUnitReferenceAndRelationshipTypeOrderByCreatedAtDesc(userId.trim(), unitReference.trim(), relationshipType);

        if (relationships.isEmpty()) {
            return new com.ams.resident.dto.RelationshipValidationResponse(false, relationshipType.name(), "NONE");
        }

        // Check if any matching relationship is APPROVED
        boolean hasApproved = relationships.stream().anyMatch(r -> r.getStatus() == RelationshipStatus.APPROVED);
        if (hasApproved) {
            return new com.ams.resident.dto.RelationshipValidationResponse(true, relationshipType.name(), "APPROVED");
        }

        // Otherwise return the status of the most recent relationship
        RelationshipStatus latestStatus = relationships.get(0).getStatus();
        return new com.ams.resident.dto.RelationshipValidationResponse(false, relationshipType.name(), latestStatus.name());
    }

    private String getAuthenticatedUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private RelationshipResponse mapToResponse(ApartmentRelationship relationship, boolean includeAllDecisions) {
        RelationshipResponse response = new RelationshipResponse();
        response.setRelationshipId(relationship.getId());
        response.setRequesterUserId(relationship.getUserId());
        response.setRelationshipType(relationship.getRelationshipType());
        response.setUnitReference(relationship.getUnitReference());
        response.setSupportingInfo(relationship.getSupportingInfo());
        response.setStatus(relationship.getStatus());
        response.setCreatedAt(relationship.getCreatedAt());

        if (includeAllDecisions || relationship.getStatus() == RelationshipStatus.REJECTED) {
            response.setDecisionReason(relationship.getDecisionReason());
        }
        if (includeAllDecisions) {
            response.setDecidedBy(relationship.getDecidedBy());
            response.setDecidedAt(relationship.getDecidedAt());
        }

        return response;
    }
}
