package com.smartparking.ds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

/**
 * Demonstrates standard First-In, First-Out (FIFO) queue semantics using {@link java.util.ArrayDeque}.
 *
 * DSA: FIFO Linear Queue implemented via {@link java.util.Queue} / {@link java.util.ArrayDeque}.
 * Why: Created as an academic viva comparison point:
 *      - {@link java.util.PriorityQueue}: O(log n) enqueue/dequeue ordered by vehicle priority and arrival.
 *      - Standard {@link java.util.Queue}: O(1) enqueue/dequeue strictly serving in order of arrival (FIFO),
 *        applicable when all vehicle priorities are equal.
 * Big-O Complexity:
 *   - Enqueue (offer): O(1) amortized
 *   - Dequeue (poll): O(1)
 *   - Peek: O(1)
 * UI Location:
 *   - Viva demonstration reference and equal-priority comparison unit tests.
 */
public class EqualPriorityFifoQueueDemo {

    private final Queue<WaitingEntry> fifoQueue = new ArrayDeque<>();

    /**
     * Enqueues an entry at the tail of the FIFO queue.
     *
     * @param entry waiting entry
     */
    public synchronized void enqueue(WaitingEntry entry) {
        if (entry != null) {
            fifoQueue.offer(entry);
        }
    }

    /**
     * Dequeues the oldest entry from the head of the FIFO queue.
     *
     * @return Optional containing the dequeued entry, or empty if queue is empty
     */
    public synchronized Optional<WaitingEntry> dequeue() {
        return Optional.ofNullable(fifoQueue.poll());
    }

    /**
     * Peeks at the head of the queue without removing it.
     */
    public synchronized Optional<WaitingEntry> peek() {
        return Optional.ofNullable(fifoQueue.peek());
    }

    /**
     * Returns a snapshot of items in FIFO arrival order.
     */
    public synchronized List<WaitingEntry> toList() {
        return new ArrayList<>(fifoQueue);
    }

    /**
     * Returns the number of items in the queue.
     */
    public synchronized int size() {
        return fifoQueue.size();
    }

    /**
     * Checks if the queue is empty.
     */
    public synchronized boolean isEmpty() {
        return fifoQueue.isEmpty();
    }

    /**
     * Clears the queue.
     */
    public synchronized void clear() {
        fifoQueue.clear();
    }
}
