package com.smartparking.ds;

import com.smartparking.model.SlotType;
import com.smartparking.model.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Core algorithmic engine for intelligent and explainable parking slot allocation.
 *
 * DSA:
 *   - Preference Mapping: {@link java.util.EnumMap} holding ordered compatible {@link SlotType} lists
 *   - Linear Filtering: O(n) filtering over candidate slots (status == AVAILABLE && compatible)
 *   - Lexicographical Sorting: O(k log k) ranking using {@link SlotRankComparator}
 * Why: Guarantees explainability, compatibility safety (SUVs never in small bays, EVs prioritized
 *      for charging bays), and deterministic best-fit allocation.
 * Big-O Complexity:
 *   - Overall Allocation: O(n) filtering + O(k log k) sorting where k <= n is compatible count.
 *   - Compatibility Check: O(1) average lookup in EnumMap
 * UI Location:
 *   - Vehicle Entry allocation result card (shows chosen slot and exact explainable reason)
 */
public class ParkingAllocationManager {

    /**
     * Preference Matrix per TRD §5:
     * - CAR:        NORMAL (preferred), LARGE (fallback)
     * - EV:         EV (preferred), NORMAL (fallback)
     * - SUV:        LARGE only
     * - ACCESSIBLE: ACCESSIBLE (preferred), NORMAL (fallback)
     */
    private static final Map<VehicleType, List<SlotType>> PREFERENCE_MATRIX = new EnumMap<>(VehicleType.class);

    static {
        PREFERENCE_MATRIX.put(VehicleType.CAR, List.of(SlotType.NORMAL, SlotType.LARGE));
        PREFERENCE_MATRIX.put(VehicleType.EV, List.of(SlotType.EV, SlotType.NORMAL));
        PREFERENCE_MATRIX.put(VehicleType.SUV, List.of(SlotType.LARGE));
        PREFERENCE_MATRIX.put(VehicleType.ACCESSIBLE, List.of(SlotType.ACCESSIBLE, SlotType.NORMAL));
    }

    /**
     * Checks if a vehicle type is compatible with a slot type.
     *
     * @param vehicleType type of vehicle
     * @param slotType type of parking slot
     * @return true if compatible, false otherwise
     */
    public static boolean isCompatible(VehicleType vehicleType, SlotType slotType) {
        if (vehicleType == null || slotType == null) {
            return false;
        }
        List<SlotType> allowed = PREFERENCE_MATRIX.get(vehicleType);
        return allowed != null && allowed.contains(slotType);
    }

    /**
     * Returns an unmodifiable list of compatible slot types in preference order for the vehicle type.
     *
     * @param vehicleType type of vehicle
     * @return ordered list of compatible slot types
     */
    public static List<SlotType> getPreferredSlotTypes(VehicleType vehicleType) {
        if (vehicleType == null) {
            return Collections.emptyList();
        }
        return PREFERENCE_MATRIX.getOrDefault(vehicleType, Collections.emptyList());
    }

    /**
     * Evaluates all parking slots and selects the best compatible slot for the incoming vehicle.
     *
     * Algorithm steps:
     * 1. Filter AVAILABLE slots (exclude OCCUPIED and MAINTENANCE) - O(n)
     * 2. Filter compatible slots per vehicle preference matrix - O(n)
     * 3. If none compatible -> return AllocationDecision with waiting reason
     * 4. Sort compatible candidates using SlotRankComparator - O(k log k)
     * 5. Select top slot and construct human-explainable reason
     *
     * @param vehicle incoming vehicle snapshot
     * @param allSlots snapshot list of all parking slots
     * @return AllocationDecision containing the assigned slot or waiting status with reason
     */
    public AllocationDecision allocate(VehicleSnapshot vehicle, List<SlotSnapshot> allSlots) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        if (allSlots == null || allSlots.isEmpty()) {
            return AllocationDecision.waiting("No parking slots registered in system.");
        }

        // Step 1 & 2: Filter AVAILABLE and COMPATIBLE slots
        List<SlotSnapshot> compatible = new ArrayList<>();
        for (SlotSnapshot slot : allSlots) {
            if (slot != null && slot.isAvailable() && isCompatible(vehicle.type(), slot.slotType())) {
                compatible.add(slot);
            }
        }

        // Step 3: Handle empty compatible list
        if (compatible.isEmpty()) {
            return AllocationDecision.waiting("No compatible AVAILABLE slot for " + vehicle.type() + ".");
        }

        // Step 4: Rank compatible candidates
        compatible.sort(new SlotRankComparator(vehicle.type(), PREFERENCE_MATRIX));

        // Step 5: Pick the optimal slot (head of sorted list)
        SlotSnapshot bestSlot = compatible.get(0);
        String reason = buildExplanationReason(vehicle.type(), bestSlot);

        return AllocationDecision.success(bestSlot, reason);
    }

    /**
     * Formulates an explainable, human-readable reason for the allocation decision.
     */
    private String buildExplanationReason(VehicleType vehicleType, SlotSnapshot slot) {
        List<SlotType> preferred = PREFERENCE_MATRIX.get(vehicleType);
        boolean isTopPreference = preferred != null && !preferred.isEmpty() && preferred.get(0) == slot.slotType();

        if (isTopPreference) {
            return slot.slotType() + " slot preferred for " + vehicleType + "; " +
                    slot.slotNumber() + " is the lowest-numbered available slot.";
        } else {
            return slot.slotType() + " slot assigned as compatible fallback for " + vehicleType + "; " +
                    slot.slotNumber() + " is the lowest-numbered available slot.";
        }
    }
}
