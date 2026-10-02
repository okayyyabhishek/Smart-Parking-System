package com.smartparking.ds;

import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;

/**
 * Immutable view of a parking slot used by the DSA layer.
 *
 * DSA: Decoupled domain object for algorithmic ranking and search.
 * Big-O: O(1) attribute access.
 * UI Location: Represented visually on Parking Slots grid and table views.
 */
public record SlotSnapshot(
        Long id,
        String slotNumber,
        SlotType slotType,
        String zone,
        int floor,
        SlotStatus status,
        String currentVehicleNumber
) {
    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE;
    }

    public boolean isOccupied() {
        return status == SlotStatus.OCCUPIED;
    }

    public boolean isMaintenance() {
        return status == SlotStatus.MAINTENANCE;
    }
}
