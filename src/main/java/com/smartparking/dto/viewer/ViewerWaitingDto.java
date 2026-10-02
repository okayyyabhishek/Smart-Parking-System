package com.smartparking.dto.viewer;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;

import java.time.Instant;

/**
 * Viewer waiting queue entry.
 * Security design: Anonymized position without vehicle number, owner, or contact.
 */
public record ViewerWaitingDto(
        int position,
        VehicleType vehicleType,
        Priority priority,
        Instant waitingSince
) {
}
