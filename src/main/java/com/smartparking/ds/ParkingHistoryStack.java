package com.smartparking.ds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Bounded stack holding recent vehicle departures.
 *
 * DSA: Bounded LIFO Stack implemented using {@link java.util.ArrayDeque} (via {@link java.util.Deque}).
 * Why: Models Last-In, First-Out (LIFO) semantics for immediate display of most recent exit events.
 *      Bounded capacity avoids unbounded memory usage while preserving instant recent history.
 * Big-O Complexity:
 *   - push(): O(1) amortized
 *   - pop(): O(1)
 *   - peek(): O(1)
 *   - Eviction of oldest when full: O(1) using deque tail removal (removeLast)
 *   - Snapshot traversal: O(k) where k is stack size (bounded <= capacity)
 * UI Location:
 *   - "Recent Exits" strip above the Parking History table
 */
public class ParkingHistoryStack {

    private final int capacity;
    private final Deque<ExitRecord> stack;

    public ParkingHistoryStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.stack = new ArrayDeque<>(capacity);
    }

    public ParkingHistoryStack() {
        this(50);
    }

    /**
     * Pushes a new exit record onto the top of the stack.
     * If the stack is at maximum capacity, the oldest record at the bottom is evicted.
     *
     * @param record exit event to push
     */
    public synchronized void push(ExitRecord record) {
        if (record == null) {
            return;
        }
        if (stack.size() >= capacity) {
            stack.removeLast(); // Evict oldest exit from the bottom of stack in O(1)
        }
        stack.push(record); // Insert at top of stack in O(1)
    }

    /**
     * Pops and removes the most recent exit record from the top of the stack.
     *
     * @return Optional containing the most recent ExitRecord if present
     */
    public synchronized Optional<ExitRecord> pop() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.pop());
    }

    /**
     * Peeks at the most recent exit record without removing it.
     *
     * @return Optional containing the most recent ExitRecord if present
     */
    public synchronized Optional<ExitRecord> peek() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.peek());
    }

    /**
     * Returns an ordered snapshot list of recent exits from newest (top) to oldest (bottom).
     *
     * @return list of recent exit records in LIFO order
     */
    public synchronized List<ExitRecord> getRecentExits() {
        return new ArrayList<>(stack);
    }

    /**
     * Returns current number of records in the stack.
     *
     * @return current size
     */
    public synchronized int size() {
        return stack.size();
    }

    /**
     * Clears all items in the stack.
     */
    public synchronized void clear() {
        stack.clear();
    }

    /**
     * Returns the configured capacity.
     */
    public int getCapacity() {
        return capacity;
    }
}
