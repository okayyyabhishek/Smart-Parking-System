package com.smartparking.dto.admin;

import com.smartparking.model.SlotType;
import com.smartparking.service.StatisticsService.SlotTypeAvailability;

import java.util.List;
import java.util.Map;

public record AdminDashboardDto(
        long totalSlots,
        long availableSlots,
        long occupiedSlots,
        long waitingVehicles,
        long todaysEntries,
        long todaysExits,
        Map<SlotType, SlotTypeAvailability> availabilityByType,
        List<AdminSessionDto> recentActivity
) {
}
