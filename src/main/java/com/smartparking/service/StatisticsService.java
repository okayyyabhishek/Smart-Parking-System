package com.smartparking.service;

import com.smartparking.model.ParkingSession;
import com.smartparking.model.SessionStatus;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes operational dashboard and statistics strictly from the database.
 * Never hard-codes statistics values (Non-negotiable Rule #3 and TRD §12).
 */
@Service
public class StatisticsService {

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSlotRepository slotRepository;
    private final VehicleRepository vehicleRepository;
    private final ZoneId zoneId;

    public StatisticsService(
            ParkingSessionRepository sessionRepository,
            ParkingSlotRepository slotRepository,
            VehicleRepository vehicleRepository,
            @Value("${app.timezone:Asia/Kolkata}") String timezone
    ) {
        this.sessionRepository = sessionRepository;
        this.slotRepository = slotRepository;
        this.vehicleRepository = vehicleRepository;
        this.zoneId = ZoneId.of(timezone);
    }

    /**
     * Aggregates and returns current system metrics computed live from database tables.
     */
    public StatisticsData getStatistics() {
        long totalServed = sessionRepository.countByStatus(SessionStatus.COMPLETED);
        long currentParked = sessionRepository.countByStatus(SessionStatus.ACTIVE);
        long availableSlots = slotRepository.countByStatus(SlotStatus.AVAILABLE);
        long waitingVehicles = sessionRepository.countByStatus(SessionStatus.WAITING);

        Double avgDuration = sessionRepository.getAverageDurationSeconds();
        long avgDurationSeconds = avgDuration != null ? Math.round(avgDuration) : 0L;

        String mostUsedSlot = computeMostUsedSlot();
        String mostCommonType = computeMostCommonVehicleType();

        // Compute today's start and end instants in configured timezone
        LocalDate today = LocalDate.now(zoneId);
        Instant startOfDay = today.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant();

        long todaysEntries = sessionRepository.countEntriesBetween(startOfDay, endOfDay);
        long todaysExits = sessionRepository.countExitsBetween(startOfDay, endOfDay);

        Map<SlotType, SlotTypeAvailability> availabilityByType = computeAvailabilityByType();

        return new StatisticsData(
                totalServed,
                currentParked,
                availableSlots,
                waitingVehicles,
                avgDurationSeconds,
                mostUsedSlot,
                mostCommonType,
                todaysEntries,
                todaysExits,
                availabilityByType
        );
    }

    private String computeMostUsedSlot() {
        List<Object[]> usageCounts = sessionRepository.findSlotUsageCounts();
        if (usageCounts == null || usageCounts.isEmpty()) {
            return "None";
        }
        Object[] top = usageCounts.get(0);
        return top[0] != null ? top[0].toString() : "None";
    }

    private String computeMostCommonVehicleType() {
        List<ParkingSession> allSessions = sessionRepository.findAll();
        if (allSessions.isEmpty()) {
            return "None";
        }

        Map<VehicleType, Integer> frequencyMap = new HashMap<>();
        for (ParkingSession session : allSessions) {
            vehicleRepository.findByVehicleNumber(session.getVehicleNumber()).ifPresent(v -> {
                frequencyMap.merge(v.getVehicleType(), 1, Integer::sum);
            });
        }

        if (frequencyMap.isEmpty()) {
            return "None";
        }

        return frequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name())
                .orElse("None");
    }

    private Map<SlotType, SlotTypeAvailability> computeAvailabilityByType() {
        Map<SlotType, SlotTypeAvailability> map = new EnumMap<>(SlotType.class);

        for (SlotType type : SlotType.values()) {
            long total = slotRepository.countBySlotType(type);
            long available = slotRepository.countBySlotTypeAndStatus(type, SlotStatus.AVAILABLE);
            long occupied = slotRepository.countBySlotTypeAndStatus(type, SlotStatus.OCCUPIED);
            long maintenance = slotRepository.countBySlotTypeAndStatus(type, SlotStatus.MAINTENANCE);
            map.put(type, new SlotTypeAvailability(total, available, occupied, maintenance));
        }

        return map;
    }

    public record SlotTypeAvailability(
            long total,
            long available,
            long occupied,
            long maintenance
    ) {
    }

    public record StatisticsData(
            long totalVehiclesServed,
            long currentParked,
            long availableSlots,
            long waitingVehicles,
            long averageParkingDurationSeconds,
            String mostUsedSlot,
            String mostCommonVehicleType,
            long todaysEntries,
            long todaysExits,
            Map<SlotType, SlotTypeAvailability> availabilityByType
    ) {
    }
}
