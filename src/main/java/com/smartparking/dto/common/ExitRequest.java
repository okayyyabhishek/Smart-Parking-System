package com.smartparking.dto.common;

import jakarta.validation.constraints.NotBlank;

public record ExitRequest(
        @NotBlank(message = "Enter a valid vehicle number.")
        String vehicleNumber
) {
}
