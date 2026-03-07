package com.authservice.service;

import com.authservice.entity.AuditLog;
import com.authservice.entity.User;
import com.authservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(User user, String event, String ipAddress, String userAgent, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .user(user)
                    .event(event)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details(details)
                    .build();
            auditLogRepository.save(entry);
            log.debug("Audit: event={} user={}", event, user != null ? user.getId() : "anonymous");
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void log(String event, String ipAddress, String details) {
        log(null, event, ipAddress, null, details);
    }
}
