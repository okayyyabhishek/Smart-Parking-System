package com.smartparking.ds;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EqualPriorityFifoQueueDemo Tests")
class EqualPriorityFifoQueueDemoTest {

    @Test
    @DisplayName("Maintains strict First-In First-Out (FIFO) arrival order for equal priority entries")
    void testStrictFifoOrder() {
        EqualPriorityFifoQueueDemo queue = new EqualPriorityFifoQueueDemo();
        Instant now = Instant.now();

        WaitingEntry e1 = new WaitingEntry("V1", VehicleType.CAR, Priority.NORMAL, now, 1L);
        WaitingEntry e2 = new WaitingEntry("V2", VehicleType.CAR, Priority.NORMAL, now.plusSeconds(1), 2L);
        WaitingEntry e3 = new WaitingEntry("V3", VehicleType.CAR, Priority.NORMAL, now.plusSeconds(2), 3L);

        queue.enqueue(e1);
        queue.enqueue(e2);
        queue.enqueue(e3);

        assertEquals(3, queue.size());
        assertEquals("V1", queue.dequeue().orElseThrow().vehicleNumber());
        assertEquals("V2", queue.dequeue().orElseThrow().vehicleNumber());
        assertEquals("V3", queue.dequeue().orElseThrow().vehicleNumber());
        assertTrue(queue.isEmpty());
    }
}
