package com.smartparking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entity representing an active, waiting, or completed parking session.
 */
@Entity
@Table(
        name = "parking_sessions",
        indexes = {
                @Index(name = "idx_session_vehicle", columnList = "vehicle_number"),
                @Index(name = "idx_session_status", columnList = "status"),
                @Index(name = "idx_session_entry_time", columnList = "entry_time")
        }
)
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_number", nullable = false, length = 32)
    private String vehicleNumber;

    @Column(name = "slot_number", length = 20)
    private String slotNumber;

    @Column(name = "entry_time")
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "enqueued_at")
    private Instant enqueuedAt;

    public ParkingSession() {
    }

    public static ParkingSession createActive(String vehicleNumber, String slotNumber, Instant entryTime) {
        ParkingSession session = new ParkingSession();
        session.setVehicleNumber(vehicleNumber);
        session.setSlotNumber(slotNumber);
        session.setEntryTime(entryTime != null ? entryTime : Instant.now());
        session.setStatus(SessionStatus.ACTIVE);
        return session;
    }

    public static ParkingSession createWaiting(String vehicleNumber, Instant enqueuedAt) {
        ParkingSession session = new ParkingSession();
        session.setVehicleNumber(vehicleNumber);
        session.setEnqueuedAt(enqueuedAt != null ? enqueuedAt : Instant.now());
        session.setStatus(SessionStatus.WAITING);
        return session;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Instant entryTime) {
        this.entryTime = entryTime;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public void setExitTime(Instant exitTime) {
        this.exitTime = exitTime;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getEnqueuedAt() {
        return enqueuedAt;
    }

    public void setEnqueuedAt(Instant enqueuedAt) {
        this.enqueuedAt = enqueuedAt;
    }
}
