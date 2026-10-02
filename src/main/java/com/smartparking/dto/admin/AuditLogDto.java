package com.smartparking.dto.admin;

import com.smartparking.model.Role;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        String username,
        Role role,
        String action,
        String target,
        String ip,
        Instant timestamp
) {
}
