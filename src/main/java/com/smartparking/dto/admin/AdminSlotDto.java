package com.smartparking.dto.admin;

import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;

/**
 * Super Admin parking slot view showing slot coordinates and full vehicle details.
 */
public record AdminSlotDto(
        Long id,
        String slotNumber,
        SlotType slotType,
        String zone,
        int floor,
        SlotStatus status,
        String currentVehicleNumber,
        String currentVehicleOwner
) {
}
