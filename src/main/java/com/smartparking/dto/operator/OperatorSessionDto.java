package com.smartparking.dto.operator;

import com.smartparking.model.SessionStatus;
import com.smartparking.model.VehicleType;

import java.time.Instant;

/**
 * Operator session view limited to recent history window (e.g. 7 days).
 */
public record OperatorSessionDto(
        Long id,
        String vehicleNumber,
        VehicleType vehicleType,
        String ownerName,
        String slotNumber,
        Instant entryTime,
        Instant exitTime,
        Long durationSeconds,
        String durationFormatted,
        SessionStatus status
) {
}
