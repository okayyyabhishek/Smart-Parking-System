package com.smartparking.ds;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;

/**
 * Immutable view of a vehicle used by the DSA layer.
 *
 * DSA: Value object stored and looked up via HashMap.
 * Big-O: O(1) attribute access.
 * UI Location: Vehicle Entry form and lookup search fields.
 */
public record VehicleSnapshot(
        Long id,
        String vehicleNumber,
        VehicleType type,
        String ownerName,
        String contact,
        Priority priority,
        boolean isParked
) {
}
