package com.smartparking.dto.admin;

import com.smartparking.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
        String username,

        @NotBlank(message = "Use at least 10 characters.")
        @Size(min = 10, message = "Use at least 10 characters.")
        String password,

        @NotNull(message = "Role is required.")
        Role role,

        boolean mustChangePassword
) {
}
