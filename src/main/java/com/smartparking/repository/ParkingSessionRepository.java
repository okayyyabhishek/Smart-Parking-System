package com.smartparking.repository;

import com.smartparking.model.ParkingSession;
import com.smartparking.model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    Optional<ParkingSession> findFirstByVehicleNumberAndStatus(String vehicleNumber, SessionStatus status);

    List<ParkingSession> findByStatusOrderByEnqueuedAtAsc(SessionStatus status);

    List<ParkingSession> findByVehicleNumber(String vehicleNumber);

    List<ParkingSession> findByVehicleNumberOrderByEntryTimeDesc(String vehicleNumber);

    long countByStatus(SessionStatus status);

    List<ParkingSession> findByStatus(SessionStatus status);

    List<ParkingSession> findByEntryTimeAfter(Instant threshold);

    @Query("SELECT COUNT(s) FROM ParkingSession s WHERE s.entryTime >= :start AND s.entryTime < :end")
    long countEntriesBetween(Instant start, Instant end);

    @Query("SELECT COUNT(s) FROM ParkingSession s WHERE s.exitTime >= :start AND s.exitTime < :end")
    long countExitsBetween(Instant start, Instant end);

    @Query("SELECT AVG(s.durationSeconds) FROM ParkingSession s WHERE s.status = 'COMPLETED' AND s.durationSeconds IS NOT NULL")
    Double getAverageDurationSeconds();

    @Query("SELECT s.slotNumber, COUNT(s) FROM ParkingSession s WHERE s.slotNumber IS NOT NULL GROUP BY s.slotNumber ORDER BY COUNT(s) DESC, s.slotNumber ASC")
    List<Object[]> findSlotUsageCounts();

    Page<ParkingSession> findByVehicleNumberContainingIgnoreCase(String search, Pageable pageable);

    Page<ParkingSession> findByEntryTimeAfterAndVehicleNumberContainingIgnoreCase(Instant threshold, String search, Pageable pageable);
}
