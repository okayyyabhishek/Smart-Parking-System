package com.smartparking.dto.admin;

import com.smartparking.model.SessionStatus;
import com.smartparking.model.VehicleType;

import java.time.Instant;

/**
 * Super Admin parking session record containing full plate numbers, timestamps, and durations.
 */
public record AdminSessionDto(
        Long id,
        String vehicleNumber,
        VehicleType vehicleType,
        String ownerName,
        String slotNumber,
        Instant entryTime,
        Instant exitTime,
        Long durationSeconds,
        String durationFormatted,
        SessionStatus status,
        Instant enqueuedAt
) {
}
