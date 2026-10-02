package com.smartparking.service;

import com.smartparking.ds.WaitingEntry;
import com.smartparking.ds.WaitingVehiclePriorityQueue;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.Priority;
import com.smartparking.model.SessionStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.util.VehicleNumberUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service managing the in-memory priority queue for waiting vehicles.
 * Serializes queue mutations using a ReentrantLock and provides startup recovery from DB.
 */
@Service
public class WaitingQueueService {

    private static final Logger log = LoggerFactory.getLogger(WaitingQueueService.class);

    private final WaitingVehiclePriorityQueue priorityQueue = new WaitingVehiclePriorityQueue();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    private final ParkingSessionRepository sessionRepository;
    private final VehicleRepository vehicleRepository;

    public WaitingQueueService(ParkingSessionRepository sessionRepository, VehicleRepository vehicleRepository) {
        this.sessionRepository = sessionRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Startup rebuild: Re-populates the in-memory PriorityQueue from persisted WAITING sessions.
     * Enforces Assumption A4.
     */
    @PostConstruct
    public void rebuildFromDatabase() {
        lock.lock();
        try {
            priorityQueue.clear();
            List<ParkingSession> waitingSessions = sessionRepository.findByStatusOrderByEnqueuedAtAsc(SessionStatus.WAITING);
            int restoredCount = 0;

            for (ParkingSession session : waitingSessions) {
                String norm = VehicleNumberUtil.normalize(session.getVehicleNumber());
                Optional<Vehicle> vOpt = vehicleRepository.findByVehicleNumber(norm);
                VehicleType vType = vOpt.map(Vehicle::getVehicleType).orElse(VehicleType.CAR);
                Priority vPriority = vOpt.map(Vehicle::getPriority).orElse(Priority.NORMAL);

                Instant enqueuedTime = session.getEnqueuedAt() != null ? session.getEnqueuedAt() : Instant.now();
                WaitingEntry entry = new WaitingEntry(norm, vType, vPriority, enqueuedTime, sequenceGenerator.incrementAndGet());
                priorityQueue.add(entry);
                restoredCount++;
            }

            log.info("Rebuilt waiting PriorityQueue from database: {} active waiting vehicles restored.", restoredCount);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueues a vehicle into the priority queue.
     */
    public WaitingEntry enqueue(String vehicleNumber, VehicleType type, Priority priority, Instant enqueuedAt) {
        lock.lock();
        try {
            String normalized = VehicleNumberUtil.normalize(vehicleNumber);
            long seq = sequenceGenerator.incrementAndGet();
            WaitingEntry entry = new WaitingEntry(normalized, type, priority, enqueuedAt != null ? enqueuedAt : Instant.now(), seq);
            priorityQueue.add(entry);
            log.info("Enqueued vehicle [{}] (type: {}, priority: {}) at position #{}",
                    maskPlate(normalized), type, priority, priorityQueue.getQueuePosition(normalized));
            return entry;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Finds and polls the highest-priority compatible vehicle for a released slot.
     */
    public Optional<WaitingEntry> pollCompatible(SlotType releasedSlotType) {
        lock.lock();
        try {
            return priorityQueue.pollCompatible(releasedSlotType);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a vehicle from the waiting queue (cancellation).
     */
    public boolean remove(String vehicleNumber) {
        lock.lock();
        try {
            return priorityQueue.remove(vehicleNumber);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if a vehicle is in the waiting queue.
     */
    public boolean contains(String vehicleNumber) {
        lock.lock();
        try {
            return priorityQueue.contains(vehicleNumber);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns 1-based queue position.
     */
    public int getQueuePosition(String vehicleNumber) {
        lock.lock();
        try {
            return priorityQueue.getQueuePosition(vehicleNumber);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a snapshot of waiting entries in priority order.
     */
    public List<WaitingEntry> getSnapshot() {
        lock.lock();
        try {
            return priorityQueue.snapshot();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns total count of waiting vehicles.
     */
    public int size() {
        lock.lock();
        try {
            return priorityQueue.size();
        } finally {
            lock.unlock();
        }
    }

    private String maskPlate(String plate) {
        if (plate == null || plate.length() <= 6) return plate;
        return plate.substring(0, 4) + "••••" + plate.substring(plate.length() - 2);
    }
}
