package com.ams.resident.repository;

import com.ams.resident.entity.EmailChangeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequestEntity, String> {

    List<EmailChangeRequestEntity> findByUserIdAndUsedAtIsNull(String userId);

    Optional<EmailChangeRequestEntity> findByTokenHashAndUserId(String tokenHash, String userId);

    @Modifying
    @Query("UPDATE EmailChangeRequestEntity e SET e.expiresAt = :now WHERE e.userId = :userId AND e.usedAt IS NULL AND e.expiresAt > :now")
    void invalidateUnusedRequestsForUser(@Param("userId") String userId, @Param("now") LocalDateTime now);
}
