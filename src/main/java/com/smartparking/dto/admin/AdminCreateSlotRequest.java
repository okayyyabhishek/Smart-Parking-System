package com.smartparking.dto.admin;

import com.smartparking.model.SlotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminCreateSlotRequest(
        @NotBlank(message = "Slot number is required.")
        @Pattern(regexp = "^[A-Z]-\\d{2,3}$", message = "Slot number must match format like A-01 or B-102.")
        String slotNumber,

        @NotNull(message = "Slot type is required.")
        SlotType slotType,

        String zone,

        int floor
) {
}
