package com.smartparking.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Searches parking slots using an explicit linear search.
 *
 * DSA: Sequential Linear Search over an array/list.
 * Why: Deliberately chosen to demonstrate O(n) algorithmic search behavior in academic viva
 *      and compare with constant-time O(1) hash table lookup.
 * Big-O Complexity:
 *   - Best Case: O(1) (slot found at head of list)
 *   - Worst Case: O(n) (slot at tail or absent)
 *   - Average Case: O(n / 2) = O(n)
 *   - Space Complexity: O(1) auxiliary
 * UI Location:
 *   - Slot search input on Parking Slots screen (captioned: "Linear search · O(n)")
 */
public class SlotSearchManager {

    /**
     * Finds a parking slot by slot number using an explicit sequential loop.
     *
     * @param slots list of parking slot snapshots
     * @param targetSlotNumber slot identifier to find (e.g. "A-01")
     * @return Optional containing the slot if found
     */
    public Optional<SlotSnapshot> findBySlotNumber(List<SlotSnapshot> slots, String targetSlotNumber) {
        if (slots == null || targetSlotNumber == null) {
            return Optional.empty();
        }

        String cleanedTarget = targetSlotNumber.trim().toUpperCase();

        // Explicit linear search loop demonstrating O(n) traversal
        for (int i = 0; i < slots.size(); i++) {
            SlotSnapshot current = slots.get(i);
            if (current != null && current.slotNumber() != null) {
                if (current.slotNumber().equalsIgnoreCase(cleanedTarget)) {
                    return Optional.of(current);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds all slots matching a given zone using linear scan.
     *
     * @param slots list of parking slots
     * @param zone zone letter/name (e.g. "A")
     * @return list of matching slots
     */
    public List<SlotSnapshot> findByZone(List<SlotSnapshot> slots, String zone) {
        List<SlotSnapshot> result = new ArrayList<>();
        if (slots == null || zone == null) {
            return result;
        }

        for (SlotSnapshot slot : slots) {
            if (slot != null && zone.equalsIgnoreCase(slot.zone())) {
                result.add(slot);
            }
        }
        return result;
    }
}
