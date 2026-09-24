package com.ams.resident.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {
    
    public void logEvent(String eventCode, String userId, String details) {
        try {
            // SLF4J structured audit log
            log.info("AUDIT [{}] - Actor: {} - Action: {}", eventCode, userId, details);
        } catch (Exception e) {
            // Audit failure should not interrupt the core business transaction.
            log.error("Failed to log audit event: {}", eventCode, e);
        }
    }
}
