package com.smartparking.dto.viewer;

import com.smartparking.model.SlotType;
import com.smartparking.service.StatisticsService.SlotTypeAvailability;

import java.util.List;
import java.util.Map;

/**
 * Viewer dashboard view containing macro capacity counters and masked activity.
 */
public record ViewerDashboardDto(
        long totalSlots,
        long availableSlots,
        long occupiedSlots,
        long waitingVehicles,
        long todaysEntries,
        long todaysExits,
        Map<SlotType, SlotTypeAvailability> availabilityByType,
        List<ViewerSessionDto> recentActivity
) {
}
