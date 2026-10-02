package com.smartparking.dto.viewer;

import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;

/**
 * Viewer parking slot view.
 * Security design: Does NOT contain vehicle fields or owner identities (occupied/free state only).
 */
public record ViewerSlotDto(
        Long id,
        String slotNumber,
        SlotType slotType,
        String zone,
        int floor,
        SlotStatus status
) {
}
