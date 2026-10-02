package com.smartparking.service;

import com.smartparking.model.AuditLog;
import com.smartparking.model.Role;
import com.smartparking.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog record(String username, Role role, String action, String target, String ip) {
        AuditLog log = new AuditLog(username, role, action, target, ip);
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsBetween(Instant from, Instant to) {
        if (from != null && to != null) {
            return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to);
        }
        return getRecentLogs();
    }
}
