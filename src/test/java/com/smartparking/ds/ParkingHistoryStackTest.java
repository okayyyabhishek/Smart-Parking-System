package com.smartparking.ds;

import com.smartparking.model.SlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParkingHistoryStack Deque-as-Stack Tests")
class ParkingHistoryStackTest {

    @Test
    @DisplayName("LIFO ordering: newest exit is on top and popped first")
    void testLifoOrdering() {
        ParkingHistoryStack stack = new ParkingHistoryStack(10);

        ExitRecord r1 = new ExitRecord("PB10AA0001", "A-01", SlotType.NORMAL, Instant.now().minusSeconds(300), 1800);
        ExitRecord r2 = new ExitRecord("PB10BB0002", "A-02", SlotType.NORMAL, Instant.now().minusSeconds(100), 3600);
        ExitRecord r3 = new ExitRecord("PB10CC0003", "A-03", SlotType.EV, Instant.now(), 7200);

        stack.push(r1);
        stack.push(r2);
        stack.push(r3);

        assertEquals(3, stack.size());

        // Snapshot list has newest first
        List<ExitRecord> snapshot = stack.getRecentExits();
        assertEquals(3, snapshot.size());
        assertEquals("PB10CC0003", snapshot.get(0).vehicleNumber());
        assertEquals("PB10BB0002", snapshot.get(1).vehicleNumber());
        assertEquals("PB10AA0001", snapshot.get(2).vehicleNumber());

        // Peek should show top element without removal
        var peeked = stack.peek();
        assertTrue(peeked.isPresent());
        assertEquals("PB10CC0003", peeked.get().vehicleNumber());
        assertEquals(3, stack.size());

        // Pop should remove top element
        var popped = stack.pop();
        assertTrue(popped.isPresent());
        assertEquals("PB10CC0003", popped.get().vehicleNumber());
        assertEquals(2, stack.size());
    }

    @Test
    @DisplayName("Bounded capacity evicts oldest exit when stack reaches limit")
    void testBoundedCapacityEviction() {
        ParkingHistoryStack stack = new ParkingHistoryStack(2);

        ExitRecord r1 = new ExitRecord("V1", "A-01", SlotType.NORMAL, Instant.now(), 100);
        ExitRecord r2 = new ExitRecord("V2", "A-02", SlotType.NORMAL, Instant.now(), 200);
        ExitRecord r3 = new ExitRecord("V3", "A-03", SlotType.EV, Instant.now(), 300);

        stack.push(r1);
        stack.push(r2);
        stack.push(r3); // Should cause r1 to be evicted

        assertEquals(2, stack.size());
        List<ExitRecord> snapshot = stack.getRecentExits();
        assertEquals("V3", snapshot.get(0).vehicleNumber());
        assertEquals("V2", snapshot.get(1).vehicleNumber());
    }
}
