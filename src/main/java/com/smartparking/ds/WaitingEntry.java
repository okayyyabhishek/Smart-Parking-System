package com.smartparking.ds;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;

import java.time.Instant;

/**
 * Immutable record representing an entry in the waiting queue.
 *
 * DSA: Record component for PriorityQueue storage.
 * Big-O: O(1) creation, memory-efficient value object.
 *
 * @param vehicleNumber normalized vehicle identifier
 * @param type vehicle type for compatibility matching
 * @param priority emergency/high/normal priority
 * @param enqueuedAt timestamp when vehicle entered waiting queue
 * @param seq monotonic sequence number to guarantee deterministic total order
 */
public record WaitingEntry(
        String vehicleNumber,
        VehicleType type,
        Priority priority,
        Instant enqueuedAt,
        long seq
) {
    public WaitingEntry {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("vehicleNumber cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (priority == null) {
            priority = Priority.NORMAL;
        }
        if (enqueuedAt == null) {
            enqueuedAt = Instant.now();
        }
    }
}
