package com.smartparking.dto.operator;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;

import java.time.Instant;

/**
 * Operator waiting queue view with masked contact details.
 */
public record OperatorWaitingDto(
        int position,
        String vehicleNumber,
        VehicleType vehicleType,
        Priority priority,
        String ownerName,
        String contactMasked,
        Instant enqueuedAt
) {
}
