package kln.ams.residentmanagement.service;

import kln.ams.residentmanagement.entity.AuditEvent;
import kln.ams.residentmanagement.repository.AuditEventRepository;
import kln.ams.residentmanagement.security.RequestIdContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public void logEvent(String action, String entityType, String entityId, String actorUserId, String details) {
        String safeActor = actorUserId != null ? actorUserId : "SYSTEM";
        String requestId = RequestIdContext.getRequestId();

        log.info("AUDIT: operation={} entityType={} entityId={} userId={} requestId={} result=SUCCESS",
                action, entityType, entityId, safeActor, requestId);

        try {
            AuditEvent event = AuditEvent.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .actorUserId(safeActor)
                    .details(details)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to persist audit event for operation {}: {}", action, e.getMessage());
        }
    }
}
