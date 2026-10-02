package com.smartparking.ds;

/**
 * Result of an automatic parking slot allocation attempt.
 *
 * DSA: Encapsulates the algorithmic assignment decision and its human-readable explanation.
 * Big-O: O(1) creation.
 * UI Location: "Allocation Result Card" shown upon successful vehicle entry.
 */
public record AllocationDecision(
        boolean allocated,
        SlotSnapshot slot,
        String reason
) {
    public static AllocationDecision success(SlotSnapshot slot, String reason) {
        return new AllocationDecision(true, slot, reason);
    }

    public static AllocationDecision waiting(String reason) {
        return new AllocationDecision(false, null, reason);
    }
}
