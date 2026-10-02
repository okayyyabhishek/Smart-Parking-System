package com.smartparking.dto.common;

import com.smartparking.model.SlotType;
import com.smartparking.service.StatisticsService.SlotTypeAvailability;

import java.util.Map;

/**
 * Statistics data transfer object containing live aggregated operational metrics.
 */
public record StatisticsDto(
        long totalVehiclesServed,
        long currentParked,
        long availableSlots,
        long waitingVehicles,
        long averageParkingDurationSeconds,
        String averageParkingDurationFormatted,
        String mostUsedSlot,
        String mostCommonVehicleType,
        long todaysEntries,
        long todaysExits,
        Map<SlotType, SlotTypeAvailability> availabilityByType
) {
}
