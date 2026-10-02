package com.smartparking.dto.viewer;

import com.smartparking.model.SessionStatus;
import com.smartparking.model.VehicleType;

import java.time.Instant;

/**
 * Viewer parking session view.
 * Security design: Strictly omits owner names, contact numbers, and masks license plates (PB10••••34).
 */
public record ViewerSessionDto(
        Long id,
        String vehicleNumberMasked,
        VehicleType vehicleType,
        String slotNumber,
        Instant entryTime,
        Instant exitTime,
        Long durationSeconds,
        String durationFormatted,
        SessionStatus status
) {
}
