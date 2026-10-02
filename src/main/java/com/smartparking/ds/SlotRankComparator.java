package com.smartparking.ds;

import com.smartparking.model.SlotType;
import com.smartparking.model.VehicleType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Custom comparator to rank compatible parking slots for a given vehicle.
 *
 * DSA: Custom Multi-field {@link java.util.Comparator} implementing lexicographical ranking:
 *      1. Preference index in compatibility matrix (lower index = higher preference)
 *      2. Floor ascending (lower floors are closer and more accessible)
 *      3. Zone ascending (alphabetical deterministic order)
 *      4. Slot number ascending (deterministic tie-break)
 * Big-O Complexity: O(1) comparison time per slot pair.
 * UI Location: Decides the slot displayed in the Allocation Result Card.
 */
public class SlotRankComparator implements Comparator<SlotSnapshot> {

    private final VehicleType vehicleType;
    private final Map<VehicleType, List<SlotType>> preferenceMatrix;

    public SlotRankComparator(VehicleType vehicleType, Map<VehicleType, List<SlotType>> preferenceMatrix) {
        this.vehicleType = vehicleType;
        this.preferenceMatrix = preferenceMatrix;
    }

    @Override
    public int compare(SlotSnapshot s1, SlotSnapshot s2) {
        if (s1 == s2) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;

        // 1. Preference Rank (Index in compatibility list)
        int rank1 = getPreferenceRank(s1.slotType());
        int rank2 = getPreferenceRank(s2.slotType());
        int prefCompare = Integer.compare(rank1, rank2);
        if (prefCompare != 0) {
            return prefCompare;
        }

        // 2. Floor ascending
        int floorCompare = Integer.compare(s1.floor(), s2.floor());
        if (floorCompare != 0) {
            return floorCompare;
        }

        // 3. Zone ascending
        String z1 = s1.zone() == null ? "" : s1.zone();
        String z2 = s2.zone() == null ? "" : s2.zone();
        int zoneCompare = z1.compareToIgnoreCase(z2);
        if (zoneCompare != 0) {
            return zoneCompare;
        }

        // 4. Slot number ascending
        String n1 = s1.slotNumber() == null ? "" : s1.slotNumber();
        String n2 = s2.slotNumber() == null ? "" : s2.slotNumber();
        return n1.compareToIgnoreCase(n2);
    }

    private int getPreferenceRank(SlotType slotType) {
        List<SlotType> preferredList = preferenceMatrix.get(vehicleType);
        if (preferredList == null) {
            return Integer.MAX_VALUE;
        }
        int index = preferredList.indexOf(slotType);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}
