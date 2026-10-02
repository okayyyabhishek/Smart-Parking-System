package com.smartparking.service;

import com.smartparking.ds.SlotSnapshot;
import com.smartparking.exception.DuplicateVehicleException;
import com.smartparking.exception.SlotNotFoundException;
import com.smartparking.exception.SlotOccupiedException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SlotService {

    private final ParkingSlotRepository slotRepository;

    public SlotService(ParkingSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<ParkingSlot> getAllSlots() {
        return slotRepository.findAllByOrderBySlotNumberAsc();
    }

    public List<SlotSnapshot> getAllSlotSnapshots() {
        return getAllSlots().stream()
                .map(this::toSnapshot)
                .toList();
    }

    public Optional<ParkingSlot> findBySlotNumber(String slotNumber) {
        if (slotNumber == null) return Optional.empty();
        return slotRepository.findBySlotNumber(slotNumber.trim().toUpperCase());
    }

    @Transactional
    public ParkingSlot createSlot(String slotNumber, SlotType slotType, String zone, int floor) {
        String cleanNumber = slotNumber != null ? slotNumber.trim().toUpperCase() : "";
        if (slotRepository.existsBySlotNumber(cleanNumber)) {
            throw new DuplicateVehicleException("Slot number already exists: " + cleanNumber);
        }

        ParkingSlot slot = new ParkingSlot(
                cleanNumber,
                slotType != null ? slotType : SlotType.NORMAL,
                zone != null ? zone.trim().toUpperCase() : "A",
                floor,
                SlotStatus.AVAILABLE
        );
        return slotRepository.save(slot);
    }

    @Transactional
    public ParkingSlot setMaintenanceStatus(String slotNumber, boolean maintenance) {
        ParkingSlot slot = findBySlotNumber(slotNumber)
                .orElseThrow(() -> new SlotNotFoundException("No slot found with number: " + slotNumber));

        if (maintenance) {
            if (slot.isOccupied()) {
                throw new SlotOccupiedException("Cannot set occupied slot " + slotNumber + " to maintenance.");
            }
            slot.setStatus(SlotStatus.MAINTENANCE);
        } else {
            if (slot.getStatus() == SlotStatus.MAINTENANCE) {
                slot.setStatus(SlotStatus.AVAILABLE);
            }
        }
        return slotRepository.save(slot);
    }

    public SlotSnapshot toSnapshot(ParkingSlot slot) {
        if (slot == null) return null;
        String curVehicle = slot.getCurrentVehicle() != null ? slot.getCurrentVehicle().getVehicleNumber() : null;
        return new SlotSnapshot(
                slot.getId(),
                slot.getSlotNumber(),
                slot.getSlotType(),
                slot.getZone(),
                slot.getFloor(),
                slot.getStatus(),
                curVehicle
        );
    }
}
