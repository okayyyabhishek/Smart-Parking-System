package com.smartparking.dto.operator;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;

/**
 * Operator vehicle view with masked contact numbers (••••••3210).
 */
public record OperatorVehicleDto(
        Long id,
        String vehicleNumber,
        VehicleType vehicleType,
        String ownerName,
        String contactMasked,
        Priority priority,
        boolean isParked
) {
}
