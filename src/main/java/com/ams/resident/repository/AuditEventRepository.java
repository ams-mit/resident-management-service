package com.ams.resident.repository;

import com.ams.resident.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findByActorUserId(String actorUserId);
    List<AuditEvent> findByAction(String action);
}
