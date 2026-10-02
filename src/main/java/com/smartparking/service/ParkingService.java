package com.smartparking.service;

import com.smartparking.ds.ExitRecord;
import com.smartparking.ds.ParkingAllocationManager;
import com.smartparking.ds.ParkingHistoryStack;
import com.smartparking.ds.SlotSnapshot;
import com.smartparking.ds.VehicleSnapshot;
import com.smartparking.ds.WaitingEntry;
import com.smartparking.exception.AlreadyParkedException;
import com.smartparking.exception.AlreadyWaitingException;
import com.smartparking.exception.VehicleNotFoundException;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Priority;
import com.smartparking.model.Role;
import com.smartparking.model.SessionStatus;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.util.VehicleNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full lifecycle of parking: entry allocation, waiting queue admission,
 * vehicle departure, history tracking, and automatic smart reallocation.
 */
@Service
public class ParkingService {

    private static final Logger log = LoggerFactory.getLogger(ParkingService.class);

    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final VehicleService vehicleService;
    private final SlotService slotService;
    private final WaitingQueueService waitingQueueService;
    private final AuditService auditService;

    private final ParkingAllocationManager allocationManager = new ParkingAllocationManager();
    private final ParkingHistoryStack historyStack;

    public ParkingService(
            ParkingSlotRepository slotRepository,
            ParkingSessionRepository sessionRepository,
            VehicleService vehicleService,
            SlotService slotService,
            WaitingQueueService waitingQueueService,
            AuditService auditService,
            @Value("${app.recent-exits.capacity:50}") int recentExitsCapacity
    ) {
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.vehicleService = vehicleService;
        this.slotService = slotService;
        this.waitingQueueService = waitingQueueService;
        this.auditService = auditService;
        this.historyStack = new ParkingHistoryStack(recentExitsCapacity);
    }

    /**
     * Vehicle Entry Workflow:
     * Validates vehicle, finds available slots, filters compatible, ranks, assigns the best.
     * If no compatible slot exists, admits vehicle to waiting PriorityQueue.
     */
    @Transactional
    public ParkingEntryResult enterParking(
            String rawVehicleNumber,
            VehicleType vehicleType,
            String ownerName,
            String contact,
            Priority priority,
            String actorUsername,
            Role actorRole,
            String clientIp
    ) {
        String normalizedNumber = VehicleNumberUtil.normalize(rawVehicleNumber);

        // 1. Guard against duplicate active sessions
        Optional<ParkingSession> activeSession = sessionRepository.findFirstByVehicleNumberAndStatus(normalizedNumber, SessionStatus.ACTIVE);
        if (activeSession.isPresent()) {
            throw new AlreadyParkedException(activeSession.get().getSlotNumber());
        }

        // 2. Guard against duplicate waiting entries
        if (waitingQueueService.contains(normalizedNumber)) {
            throw new AlreadyWaitingException();
        }

        // 3. Register or update vehicle entity
        Vehicle vehicle = vehicleService.findOrCreateVehicle(normalizedNumber, ownerName, contact, vehicleType, priority);
        VehicleSnapshot vehicleSnapshot = vehicleService.toSnapshot(vehicle, false);

        // 4. Algorithmic allocation over all slot snapshots
        List<SlotSnapshot> allSlots = slotService.getAllSlotSnapshots();
        var decision = allocationManager.allocate(vehicleSnapshot, allSlots);

        Instant now = Instant.now();

        if (decision.allocated()) {
            // Best compatible slot selected
            SlotSnapshot chosenSnapshot = decision.slot();
            ParkingSlot slot = slotService.findBySlotNumber(chosenSnapshot.slotNumber())
                    .orElseThrow(() -> new IllegalStateException("Allocated slot does not exist: " + chosenSnapshot.slotNumber()));

            slot.setStatus(SlotStatus.OCCUPIED);
            slot.setCurrentVehicle(vehicle);
            slotRepository.save(slot);

            ParkingSession session = ParkingSession.createActive(normalizedNumber, slot.getSlotNumber(), now);
            ParkingSession savedSession = sessionRepository.save(session);

            vehicleService.setParkedStatus(normalizedNumber, true);

            auditService.record(actorUsername, actorRole, "PARK_ENTRY",
                    "Slot: " + slot.getSlotNumber() + ", Vehicle: " + normalizedNumber, clientIp);

            log.info("Vehicle [{}] allocated to slot [{}] (Type: {}). Reason: {}",
                    maskPlate(normalizedNumber), slot.getSlotNumber(), slot.getSlotType(), decision.reason());

            return ParkingEntryResult.allocated(slot.getSlotNumber(), slot.getSlotType(), savedSession.getId(), now, decision.reason());
        } else {
            // No compatible available slot: add to Waiting PriorityQueue
            ParkingSession waitingSession = ParkingSession.createWaiting(normalizedNumber, now);
            sessionRepository.save(waitingSession);

            waitingQueueService.enqueue(normalizedNumber, vehicle.getVehicleType(), vehicle.getPriority(), now);
            int position = waitingQueueService.getQueuePosition(normalizedNumber);

            auditService.record(actorUsername, actorRole, "PARK_WAITING",
                    "Queue Pos: " + position + ", Vehicle: " + normalizedNumber, clientIp);

            log.info("No compatible slot for [{}]. Enqueued at position #{}", maskPlate(normalizedNumber), position);

            return ParkingEntryResult.waiting(position, decision.reason());
        }
    }

    /**
     * Vehicle Exit Workflow:
     * Releases the slot, closes the active session, computes duration, pushes to recent-exits stack,
     * and performs AUTOMATIC SMART REALLOCATION to the next compatible waiting vehicle.
     */
    @Transactional
    public ParkingExitResult exitParking(String rawVehicleNumber, String actorUsername, Role actorRole, String clientIp) {
        String normalizedNumber = VehicleNumberUtil.normalize(rawVehicleNumber);

        ParkingSession session = sessionRepository.findFirstByVehicleNumberAndStatus(normalizedNumber, SessionStatus.ACTIVE)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle " + normalizedNumber + " is not currently parked in an active session."));

        String slotNumber = session.getSlotNumber();
        ParkingSlot slot = slotService.findBySlotNumber(slotNumber)
                .orElseThrow(() -> new IllegalStateException("Slot " + slotNumber + " referenced by active session not found."));

        Instant exitTime = Instant.now();
        Instant entryTime = session.getEntryTime() != null ? session.getEntryTime() : exitTime;
        long durationSeconds = Math.max(0, Duration.between(entryTime, exitTime).toSeconds());

        // 1. Close session
        session.setExitTime(exitTime);
        session.setDurationSeconds(durationSeconds);
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        // 2. Update lookup manager
        vehicleService.setParkedStatus(normalizedNumber, false);

        // 3. Push to LIFO Recent Exits Stack
        ExitRecord exitRecord = new ExitRecord(normalizedNumber, slot.getSlotNumber(), slot.getSlotType(), exitTime, durationSeconds);
        historyStack.push(exitRecord);

        auditService.record(actorUsername, actorRole, "PARK_EXIT",
                "Slot: " + slot.getSlotNumber() + ", Vehicle: " + normalizedNumber + ", Duration: " + durationSeconds + "s", clientIp);

        log.info("Vehicle [{}] exited slot [{}] after {}s", maskPlate(normalizedNumber), slot.getSlotNumber(), durationSeconds);

        // 4. Central Smart Feature: Automatic Reallocation
        Optional<WaitingEntry> compatibleWaiting = waitingQueueService.pollCompatible(slot.getSlotType());

        ReallocationDetails reallocation;

        if (compatibleWaiting.isPresent()) {
            WaitingEntry reallocatedVehicle = compatibleWaiting.get();
            String nextPlate = reallocatedVehicle.vehicleNumber();

            // Transition waiting session to ACTIVE
            Optional<ParkingSession> waitingSessionOpt = sessionRepository.findFirstByVehicleNumberAndStatus(nextPlate, SessionStatus.WAITING);
            ParkingSession reallocatedSession;
            if (waitingSessionOpt.isPresent()) {
                reallocatedSession = waitingSessionOpt.get();
                reallocatedSession.setStatus(SessionStatus.ACTIVE);
                reallocatedSession.setSlotNumber(slot.getSlotNumber());
                reallocatedSession.setEntryTime(exitTime);
            } else {
                reallocatedSession = ParkingSession.createActive(nextPlate, slot.getSlotNumber(), exitTime);
            }
            sessionRepository.save(reallocatedSession);

            // Re-assign the released slot
            Vehicle waitingVehicleEntity = vehicleService.findByVehicleNumber(nextPlate)
                    .orElseGet(() -> vehicleService.findOrCreateVehicle(nextPlate, "Queued Owner", null, reallocatedVehicle.type(), reallocatedVehicle.priority()));

            slot.setStatus(SlotStatus.OCCUPIED);
            slot.setCurrentVehicle(waitingVehicleEntity);
            slotRepository.save(slot);

            vehicleService.setParkedStatus(nextPlate, true);

            auditService.record(actorUsername, actorRole, "PARK_REALLOCATED",
                    "Slot: " + slot.getSlotNumber() + ", Reallocated to: " + nextPlate, clientIp);

            log.info("Slot [{}] automatically reallocated to waiting vehicle [{}] (Priority: {})",
                    slot.getSlotNumber(), maskPlate(nextPlate), reallocatedVehicle.priority());

            reallocation = new ReallocationDetails(true, nextPlate, slot.getSlotNumber());
        } else {
            // No compatible waiting vehicle; slot becomes free
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setCurrentVehicle(null);
            slotRepository.save(slot);

            reallocation = new ReallocationDetails(false, null, null);
        }

        return new ParkingExitResult(slot.getSlotNumber(), durationSeconds, reallocation);
    }

    public ParkingHistoryStack getHistoryStack() {
        return historyStack;
    }

    private String maskPlate(String plate) {
        if (plate == null || plate.length() <= 6) return plate;
        return plate.substring(0, 4) + "••••" + plate.substring(plate.length() - 2);
    }

    public record ParkingEntryResult(
            boolean allocated,
            String slotNumber,
            SlotType slotType,
            Long sessionId,
            Instant entryTime,
            int queuePosition,
            String reason
    ) {
        public static ParkingEntryResult allocated(String slotNumber, SlotType slotType, Long sessionId, Instant entryTime, String reason) {
            return new ParkingEntryResult(true, slotNumber, slotType, sessionId, entryTime, 0, reason);
        }

        public static ParkingEntryResult waiting(int queuePosition, String reason) {
            return new ParkingEntryResult(false, null, null, null, null, queuePosition, reason);
        }
    }

    public record ReallocationDetails(
            boolean performed,
            String vehicleNumber,
            String slotNumber
    ) {
    }

    public record ParkingExitResult(
            String releasedSlot,
            long durationSeconds,
            ReallocationDetails reallocation
    ) {
    }
}
