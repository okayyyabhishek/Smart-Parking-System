package com.smartparking.ds;

import com.smartparking.model.SlotType;

import java.time.Instant;

/**
 * Immutable record representing a vehicle departure event.
 *
 * DSA: Stored in LIFO order inside ParkingHistoryStack (Deque-as-Stack).
 * Big-O: O(1) creation.
 * UI Location: "Recent Exits" strip displayed above the Parking History table.
 */
public record ExitRecord(
        String vehicleNumber,
        String slotNumber,
        SlotType slotType,
        Instant exitTime,
        long durationSeconds
) {
}
