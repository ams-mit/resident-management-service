package kln.ams.residentmanagement.repository;

import kln.ams.residentmanagement.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    List<AuditEvent> findByEntityId(String entityId);

    List<AuditEvent> findByActorUserId(String actorUserId);
}
