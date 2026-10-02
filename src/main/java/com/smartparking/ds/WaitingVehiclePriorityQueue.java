package com.smartparking.ds;

import com.smartparking.model.SlotType;
import com.smartparking.util.VehicleNumberUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Manages waiting vehicles awaiting an available compatible parking slot.
 *
 * DSA: Min/Max Binary Heap via {@link java.util.PriorityQueue} with custom {@link java.util.Comparator}.
 * Why: Guarantees prioritized scheduling:
 *      1. Priority level descending (EMERGENCY > HIGH > NORMAL)
 *      2. Arrival/Enqueued timestamp ascending (earlier arrival = longer wait gets precedence)
 *      3. Monotonic sequence ID (deterministic tie-break preserving arrival order)
 * Big-O Complexity:
 *   - Insertion (add/offer): O(log n) heap bubble-up
 *   - Extraction (poll): O(log n) heap bubble-down
 *   - Peek head: O(1)
 *   - Cancellation / Removal by vehicle number: O(n) scan + O(log n) sift
 *   - Snapshot generation (sorted list for UI): O(n log n)
 *   - Reallocation scan (pollCompatible): O(k log n) where k is number of examined incompatible entries
 * UI Location:
 *   - Waiting Queue screen on all portals (captioned: "PriorityQueue · insert/remove O(log n)")
 *   - Live queue counter on Dashboard
 */
public class WaitingVehiclePriorityQueue {

    /**
     * Total ordering comparator for waiting entries:
     * 1. Priority level descending (EMERGENCY(3) > HIGH(2) > NORMAL(1))
     * 2. Wait time descending (enqueuedAt ascending)
     * 3. Monotonic sequence ascending (tie-break)
     */
    public static final Comparator<WaitingEntry> WAITING_ORDER =
            Comparator.comparingInt((WaitingEntry e) -> e.priority().getLevel()).reversed()
                    .thenComparing(WaitingEntry::enqueuedAt)
                    .thenComparingLong(WaitingEntry::seq);

    private final PriorityQueue<WaitingEntry> queue = new PriorityQueue<>(WAITING_ORDER);

    /**
     * Enqueues a vehicle into the priority queue.
     *
     * @param entry waiting vehicle entry
     */
    public synchronized void add(WaitingEntry entry) {
        if (entry != null) {
            queue.add(entry);
        }
    }

    /**
     * Retrieves and removes the highest-priority head of the queue.
     *
     * @return Optional containing the head WaitingEntry, or empty if queue is empty
     */
    public synchronized Optional<WaitingEntry> poll() {
        return Optional.ofNullable(queue.poll());
    }

    /**
     * Inspects the highest-priority head without removing it.
     *
     * @return Optional containing the head WaitingEntry, or empty if queue is empty
     */
    public synchronized Optional<WaitingEntry> peek() {
        return Optional.ofNullable(queue.peek());
    }

    /**
     * Central Smart Reallocation:
     * Scans the waiting queue in priority order to find the highest-priority
     * entry whose vehicle type is COMPATIBLE with the released slot type.
     *
     * Incompatible entries pulled ahead of it are temporarily buffered in an ArrayList
     * and subsequently re-added to restore full PriorityQueue invariants.
     *
     * @param releasedSlotType the slot type that has become available
     * @return Optional containing the assigned WaitingEntry, or empty if none compatible
     */
    public synchronized Optional<WaitingEntry> pollCompatible(SlotType releasedSlotType) {
        if (releasedSlotType == null || queue.isEmpty()) {
            return Optional.empty();
        }

        List<WaitingEntry> skippedBuffer = new ArrayList<>();
        WaitingEntry assigned = null;

        while (!queue.isEmpty()) {
            WaitingEntry candidate = queue.poll();
            if (ParkingAllocationManager.isCompatible(candidate.type(), releasedSlotType)) {
                assigned = candidate;
                break;
            }
            skippedBuffer.add(candidate);
        }

        // Restore all skipped incompatible entries back to the PriorityQueue
        queue.addAll(skippedBuffer);

        return Optional.ofNullable(assigned);
    }

    /**
     * Removes an entry matching the specified vehicle number (e.g. on cancellation).
     *
     * @param vehicleNumber registration number to remove
     * @return true if an entry was found and removed, false otherwise
     */
    public synchronized boolean remove(String vehicleNumber) {
        if (vehicleNumber == null) {
            return false;
        }
        String normalized = VehicleNumberUtil.normalize(vehicleNumber);
        return queue.removeIf(entry -> VehicleNumberUtil.normalize(entry.vehicleNumber()).equalsIgnoreCase(normalized));
    }

    /**
     * Checks if a vehicle is currently in the waiting queue.
     *
     * @param vehicleNumber registration number to check
     * @return true if queued
     */
    public synchronized boolean contains(String vehicleNumber) {
        if (vehicleNumber == null) {
            return false;
        }
        String normalized = VehicleNumberUtil.normalize(vehicleNumber);
        for (WaitingEntry entry : queue) {
            if (VehicleNumberUtil.normalize(entry.vehicleNumber()).equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines the current 1-based queue position of a vehicle based on priority ordering.
     *
     * @param vehicleNumber registration number to query
     * @return 1-based rank position, or -1 if not in queue
     */
    public synchronized int getQueuePosition(String vehicleNumber) {
        if (vehicleNumber == null) {
            return -1;
        }
        List<WaitingEntry> sorted = snapshot();
        String normalized = VehicleNumberUtil.normalize(vehicleNumber);
        for (int i = 0; i < sorted.size(); i++) {
            if (VehicleNumberUtil.normalize(sorted.get(i).vehicleNumber()).equalsIgnoreCase(normalized)) {
                return i + 1; // 1-based index
            }
        }
        return -1;
    }

    /**
     * Returns a sorted snapshot of all waiting entries in priority order.
     *
     * @return list sorted by WAITING_ORDER
     */
    public synchronized List<WaitingEntry> snapshot() {
        List<WaitingEntry> list = new ArrayList<>(queue);
        list.sort(WAITING_ORDER);
        return list;
    }

    /**
     * Current count of waiting vehicles.
     *
     * @return queue size
     */
    public synchronized int size() {
        return queue.size();
    }

    /**
     * Checks if the queue is empty.
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clears all waiting vehicles.
     */
    public synchronized void clear() {
        queue.clear();
    }
}
