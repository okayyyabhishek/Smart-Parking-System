package com.smartparking.dto.admin;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;

/**
 * Super Admin vehicle representation with unmasked owner name, contact, and plate.
 */
public record AdminVehicleDto(
        Long id,
        String vehicleNumber,
        VehicleType vehicleType,
        String ownerName,
        String contact,
        Priority priority,
        boolean isParked
) {
}
