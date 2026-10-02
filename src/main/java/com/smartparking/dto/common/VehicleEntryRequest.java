package com.smartparking.dto.common;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for vehicle parking entry.
 * Note: Crucial rule - NO slot field exists in this request (slot allocation is 100% automated).
 */
public record VehicleEntryRequest(
        @NotBlank(message = "Enter a valid vehicle number.")
        @Pattern(regexp = "^[A-Za-z0-9\\s\\-\\.]+$", message = "Enter a valid vehicle number.")
        String vehicleNumber,

        @NotNull(message = "Choose CAR, EV, SUV or ACCESSIBLE.")
        VehicleType vehicleType,

        @NotBlank(message = "Owner name is required.")
        @Size(min = 2, max = 60, message = "Owner name must be between 2 and 60 characters.")
        String ownerName,

        @Pattern(regexp = "^$|^[0-9]{7,15}$", message = "Contact must be 7–15 digits.")
        String contact,

        Priority priority
) {
    public Priority priority() {
        return priority != null ? priority : Priority.NORMAL;
    }
}
