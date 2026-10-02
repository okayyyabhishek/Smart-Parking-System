package com.smartparking.dto.common;

import com.smartparking.model.Role;

public record LoginResponse(
        String username,
        Role role,
        boolean mustChangePassword,
        String message
) {
}
