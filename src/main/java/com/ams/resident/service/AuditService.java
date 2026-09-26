package com.ams.resident.service;

import com.ams.resident.entity.AuditEvent;
import com.ams.resident.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void logEvent(String action, String entityType, String entityId, String actorUserId, String details) {
        log.info("AUDIT [{}] - Actor: {} - Action: {}", action, actorUserId, details);
        AuditEvent event = AuditEvent.builder()
                .action(action)
                .entityType(entityType != null ? entityType : "GENERAL")
                .entityId(entityId)
                .actorUserId(actorUserId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        auditEventRepository.save(event);
    }

    @Transactional
    public void logEvent(String action, String actorUserId, String details) {
        logEvent(action, "GENERAL", null, actorUserId, details);
    }
}
