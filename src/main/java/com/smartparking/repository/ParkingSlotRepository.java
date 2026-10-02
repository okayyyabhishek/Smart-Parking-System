package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    Optional<ParkingSlot> findBySlotNumber(String slotNumber);

    boolean existsBySlotNumber(String slotNumber);

    List<ParkingSlot> findByStatus(SlotStatus status);

    List<ParkingSlot> findBySlotType(SlotType slotType);

    long countByStatus(SlotStatus status);

    long countBySlotTypeAndStatus(SlotType slotType, SlotStatus status);

    long countBySlotType(SlotType slotType);

    List<ParkingSlot> findAllByOrderBySlotNumberAsc();
}
