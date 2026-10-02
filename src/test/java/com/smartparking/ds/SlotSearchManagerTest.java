package com.smartparking.ds;

import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SlotSearchManager Linear Search Tests")
class SlotSearchManagerTest {

    private final SlotSearchManager searchManager = new SlotSearchManager();

    private final List<SlotSnapshot> demoSlots = List.of(
            new SlotSnapshot(1L, "A-01", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null),
            new SlotSnapshot(2L, "A-02", SlotType.NORMAL, "A", 0, SlotStatus.OCCUPIED, "PB10AB1234"),
            new SlotSnapshot(3L, "A-03", SlotType.EV, "A", 0, SlotStatus.AVAILABLE, null),
            new SlotSnapshot(4L, "A-04", SlotType.LARGE, "A", 0, SlotStatus.MAINTENANCE, null),
            new SlotSnapshot(5L, "B-01", SlotType.ACCESSIBLE, "B", 1, SlotStatus.AVAILABLE, null)
    );

    @Test
    @DisplayName("Finds slot by exact and case-insensitive slot number via linear search")
    void testFindBySlotNumber() {
        var foundA03 = searchManager.findBySlotNumber(demoSlots, "a-03");
        assertTrue(foundA03.isPresent());
        assertEquals("A-03", foundA03.get().slotNumber());
        assertEquals(SlotType.EV, foundA03.get().slotType());

        var notFound = searchManager.findBySlotNumber(demoSlots, "Z-99");
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("Finds all slots within a specified zone")
    void testFindByZone() {
        var zoneASlots = searchManager.findByZone(demoSlots, "A");
        assertEquals(4, zoneASlots.size());

        var zoneBSlots = searchManager.findByZone(demoSlots, "B");
        assertEquals(1, zoneBSlots.size());
        assertEquals("B-01", zoneBSlots.get(0).slotNumber());

        var zoneCSlots = searchManager.findByZone(demoSlots, "C");
        assertTrue(zoneCSlots.isEmpty());
    }
}
