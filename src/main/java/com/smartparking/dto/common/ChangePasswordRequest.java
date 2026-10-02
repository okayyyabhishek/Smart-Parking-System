package com.smartparking.dto.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "Use at least 10 characters.")
        @Size(min = 10, message = "Use at least 10 characters.")
        String newPassword
) {
}
