package com.smartparking.dto.common;

import com.smartparking.model.Role;

public record AuthMeResponse(
        String username,
        Role role,
        boolean mustChangePassword
) {
}
