package com.smartparking.ds;

import com.smartparking.model.Priority;
import com.smartparking.model.SlotType;
import com.smartparking.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WaitingVehiclePriorityQueue Tests")
class WaitingVehiclePriorityQueueTest {

    @Test
    @DisplayName("Priority ordering: higher priority beats wait time, wait time beats arrival seq")
    void testPriorityOrderingRules() {
        WaitingVehiclePriorityQueue pq = new WaitingVehiclePriorityQueue();
        Instant t0 = Instant.now();

        // NORMAL priority arrived at t0
        WaitingEntry normalEarly = new WaitingEntry("CAR-NORMAL-EARLY", VehicleType.CAR, Priority.NORMAL, t0, 1L);
        // HIGH priority arrived 10 minutes later at t0 + 600s
        WaitingEntry highLate = new WaitingEntry("CAR-HIGH-LATE", VehicleType.CAR, Priority.HIGH, t0.plusSeconds(600), 2L);
        // EMERGENCY priority arrived 20 minutes later at t0 + 1200s
        WaitingEntry emergencyLatest = new WaitingEntry("CAR-EMERGENCY", VehicleType.CAR, Priority.EMERGENCY, t0.plusSeconds(1200), 3L);
        // Another NORMAL arrived 1 minute later at t0 + 60s
        WaitingEntry normalLate = new WaitingEntry("CAR-NORMAL-LATE", VehicleType.CAR, Priority.NORMAL, t0.plusSeconds(60), 4L);

        pq.add(normalEarly);
        pq.add(highLate);
        pq.add(emergencyLatest);
        pq.add(normalLate);

        assertEquals(4, pq.size());

        // Polling order should be: EMERGENCY -> HIGH -> NORMAL-EARLY -> NORMAL-LATE
        assertEquals("CAR-EMERGENCY", pq.poll().orElseThrow().vehicleNumber());
        assertEquals("CAR-HIGH-LATE", pq.poll().orElseThrow().vehicleNumber());
        assertEquals("CAR-NORMAL-EARLY", pq.poll().orElseThrow().vehicleNumber());
        assertEquals("CAR-NORMAL-LATE", pq.poll().orElseThrow().vehicleNumber());
        assertTrue(pq.isEmpty());
    }

    @Test
    @DisplayName("Smart Reallocation: Incompatible head is skipped and restored, first compatible vehicle is assigned")
    void testReallocationSkippingIncompatibleHeadAndRestoring() {
        WaitingVehiclePriorityQueue pq = new WaitingVehiclePriorityQueue();
        Instant t0 = Instant.now();

        // SUV at head of queue (Priority HIGH)
        WaitingEntry suvHigh = new WaitingEntry("SUV-HIGH", VehicleType.SUV, Priority.HIGH, t0, 1L);
        // CAR behind SUV (Priority NORMAL)
        WaitingEntry carNormal = new WaitingEntry("CAR-NORMAL", VehicleType.CAR, Priority.NORMAL, t0.plusSeconds(10), 2L);

        pq.add(suvHigh);
        pq.add(carNormal);

        // Released slot is a NORMAL slot (A-01).
        // SUV is incompatible with NORMAL (SUVs require LARGE).
        // CAR is compatible with NORMAL.
        var reallocated = pq.pollCompatible(SlotType.NORMAL);

        assertTrue(reallocated.isPresent(), "Compatible vehicle should be found");
        assertEquals("CAR-NORMAL", reallocated.get().vehicleNumber(), "CAR should be allocated despite lower priority");

        // Crucial test: SUV must be preserved and restored in queue!
        assertEquals(1, pq.size(), "Incompatible SUV must remain in queue");
        assertEquals("SUV-HIGH", pq.peek().orElseThrow().vehicleNumber(), "SUV must be at the head of queue");
    }

    @Test
    @DisplayName("Queue position, snapshot ordering, and cancellation removal")
    void testPositionAndRemoval() {
        WaitingVehiclePriorityQueue pq = new WaitingVehiclePriorityQueue();
        Instant now = Instant.now();

        WaitingEntry e1 = new WaitingEntry("V1", VehicleType.CAR, Priority.NORMAL, now, 1L);
        WaitingEntry e2 = new WaitingEntry("V2", VehicleType.SUV, Priority.HIGH, now.plusSeconds(5), 2L);
        WaitingEntry e3 = new WaitingEntry("V3", VehicleType.EV, Priority.NORMAL, now.plusSeconds(10), 3L);

        pq.add(e1);
        pq.add(e2);
        pq.add(e3);

        // In sorted order: V2 (HIGH, rank 1), V1 (NORMAL, rank 2), V3 (NORMAL, rank 3)
        assertEquals(1, pq.getQueuePosition("v2"));
        assertEquals(2, pq.getQueuePosition("V1"));
        assertEquals(3, pq.getQueuePosition("v3"));
        assertEquals(-1, pq.getQueuePosition("UNKNOWN"));

        List<WaitingEntry> snapshot = pq.snapshot();
        assertEquals("V2", snapshot.get(0).vehicleNumber());
        assertEquals("V1", snapshot.get(1).vehicleNumber());
        assertEquals("V3", snapshot.get(2).vehicleNumber());

        // Cancel / remove V1
        assertTrue(pq.remove("v1"));
        assertEquals(2, pq.size());
        assertFalse(pq.contains("V1"));
        assertEquals(1, pq.getQueuePosition("V2"));
        assertEquals(2, pq.getQueuePosition("V3"));
    }
}
