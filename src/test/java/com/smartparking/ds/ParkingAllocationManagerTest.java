package com.smartparking.ds;

import com.smartparking.model.Priority;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParkingAllocationManager Compatibility and Ranking Tests")
class ParkingAllocationManagerTest {

    private ParkingAllocationManager manager;

    @BeforeEach
    void setUp() {
        manager = new ParkingAllocationManager();
    }

    private List<SlotSnapshot> createDemoSlots() {
        return List.of(
                new SlotSnapshot(1L, "A-01", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(2L, "A-02", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(3L, "A-03", SlotType.EV, "A", 0, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(4L, "A-04", SlotType.LARGE, "A", 0, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(5L, "A-05", SlotType.ACCESSIBLE, "A", 0, SlotStatus.AVAILABLE, null)
        );
    }

    @Test
    @DisplayName("CAR prefers NORMAL (A-01) over LARGE (A-04)")
    void testCarAllocation() {
        VehicleSnapshot car = new VehicleSnapshot(1L, "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, false);
        var decision = manager.allocate(car, createDemoSlots());

        assertTrue(decision.allocated());
        assertEquals("A-01", decision.slot().slotNumber());
        assertEquals(SlotType.NORMAL, decision.slot().slotType());
        assertTrue(decision.reason().contains("NORMAL slot preferred for CAR"));
    }

    @Test
    @DisplayName("EV prefers EV slot (A-03) over NORMAL")
    void testEvAllocation() {
        VehicleSnapshot ev = new VehicleSnapshot(2L, "PB10XY4567", VehicleType.EV, "S. Singh", "9876543211", Priority.NORMAL, false);
        var decision = manager.allocate(ev, createDemoSlots());

        assertTrue(decision.allocated());
        assertEquals("A-03", decision.slot().slotNumber());
        assertEquals(SlotType.EV, decision.slot().slotType());
        assertTrue(decision.reason().contains("EV slot preferred for EV"));
    }

    @Test
    @DisplayName("SUV requires LARGE slot only (A-04)")
    void testSuvAllocation() {
        VehicleSnapshot suv = new VehicleSnapshot(3L, "PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL, false);
        var decision = manager.allocate(suv, createDemoSlots());

        assertTrue(decision.allocated());
        assertEquals("A-04", decision.slot().slotNumber());
        assertEquals(SlotType.LARGE, decision.slot().slotType());
    }

    @Test
    @DisplayName("ACCESSIBLE prefers ACCESSIBLE slot (A-05)")
    void testAccessibleAllocation() {
        VehicleSnapshot acc = new VehicleSnapshot(4L, "PB10AC9999", VehicleType.ACCESSIBLE, "G. Kaur", "9876543213", Priority.HIGH, false);
        var decision = manager.allocate(acc, createDemoSlots());

        assertTrue(decision.allocated());
        assertEquals("A-05", decision.slot().slotNumber());
        assertEquals(SlotType.ACCESSIBLE, decision.slot().slotType());
    }

    @Test
    @DisplayName("Skips OCCUPIED and MAINTENANCE slots")
    void testSkipsOccupiedAndMaintenanceSlots() {
        List<SlotSnapshot> slots = List.of(
                new SlotSnapshot(1L, "A-01", SlotType.NORMAL, "A", 0, SlotStatus.OCCUPIED, "PB10ZZ0001"),
                new SlotSnapshot(2L, "A-02", SlotType.NORMAL, "A", 0, SlotStatus.MAINTENANCE, null),
                new SlotSnapshot(3L, "A-03", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null)
        );

        VehicleSnapshot car = new VehicleSnapshot(1L, "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, false);
        var decision = manager.allocate(car, slots);

        assertTrue(decision.allocated());
        assertEquals("A-03", decision.slot().slotNumber());
    }

    @Test
    @DisplayName("Returns WAITING decision when no compatible slot is available")
    void testNoCompatibleSlotReturnsWaiting() {
        // Only NORMAL slots available; SUV needs LARGE
        List<SlotSnapshot> slots = List.of(
                new SlotSnapshot(1L, "A-01", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(2L, "A-02", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null)
        );

        VehicleSnapshot suv = new VehicleSnapshot(3L, "PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL, false);
        var decision = manager.allocate(suv, slots);

        assertFalse(decision.allocated());
        assertNull(decision.slot());
        assertTrue(decision.reason().contains("No compatible AVAILABLE slot for SUV"));
    }

    @Test
    @DisplayName("Fallback compatibility: CAR can use LARGE if NORMAL is unavailable")
    void testFallbackCompatibility() {
        List<SlotSnapshot> slots = List.of(
                new SlotSnapshot(1L, "A-01", SlotType.NORMAL, "A", 0, SlotStatus.OCCUPIED, "PB10ZZ0001"),
                new SlotSnapshot(4L, "A-04", SlotType.LARGE, "A", 0, SlotStatus.AVAILABLE, null)
        );

        VehicleSnapshot car = new VehicleSnapshot(1L, "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, false);
        var decision = manager.allocate(car, slots);

        assertTrue(decision.allocated());
        assertEquals("A-04", decision.slot().slotNumber());
        assertEquals(SlotType.LARGE, decision.slot().slotType());
        assertTrue(decision.reason().contains("compatible fallback"));
    }

    @Test
    @DisplayName("Deterministic tie-breaking: lower floor first, then zone, then slot number")
    void testTieBreakingOrder() {
        List<SlotSnapshot> slots = List.of(
                new SlotSnapshot(1L, "B-02", SlotType.NORMAL, "B", 1, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(2L, "A-02", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null),
                new SlotSnapshot(3L, "A-01", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE, null)
        );

        VehicleSnapshot car = new VehicleSnapshot(1L, "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, false);
        var decision = manager.allocate(car, slots);

        assertTrue(decision.allocated());
        assertEquals("A-01", decision.slot().slotNumber(), "Floor 0, Zone A, Slot A-01 should win tie-break over A-02 and B-02");
    }
}
