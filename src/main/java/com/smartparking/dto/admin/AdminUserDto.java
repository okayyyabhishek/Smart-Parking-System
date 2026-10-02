package com.smartparking.dto.admin;

import com.smartparking.model.Role;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String username,
        Role role,
        boolean enabled,
        boolean mustChangePassword,
        Instant createdAt
) {
}
