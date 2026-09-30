package com.ams.resident.repository;

import com.ams.resident.entity.ApartmentRelationship;
import com.ams.resident.entity.RelationshipStatus;
import com.ams.resident.entity.RelationshipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentRelationshipRepository extends JpaRepository<ApartmentRelationship, String> {
    List<ApartmentRelationship> findByUserId(String userId);

    Page<ApartmentRelationship> findByStatus(RelationshipStatus status, Pageable pageable);

    Optional<ApartmentRelationship> findByUserIdAndUnitReferenceAndRelationshipType(
            String userId,
            String unitReference,
            RelationshipType relationshipType
    );
}
