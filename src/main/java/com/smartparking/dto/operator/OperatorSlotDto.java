package com.smartparking.dto.operator;

import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.VehicleType;

/**
 * Operator slot view showing slot coordinates and vehicle plate + type (owner name omitted).
 */
public record OperatorSlotDto(
        Long id,
        String slotNumber,
        SlotType slotType,
        String zone,
        int floor,
        SlotStatus status,
        String currentVehicleNumber,
        VehicleType currentVehicleType
) {
}
