package com.smartparking.service;

import com.smartparking.ds.VehicleLookupManager;
import com.smartparking.ds.VehicleSnapshot;
import com.smartparking.model.Priority;
import com.smartparking.model.SessionStatus;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.util.VehicleNumberUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final ParkingSessionRepository sessionRepository;
    private final VehicleLookupManager lookupManager = new VehicleLookupManager();

    public VehicleService(VehicleRepository vehicleRepository, ParkingSessionRepository sessionRepository) {
        this.vehicleRepository = vehicleRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Warms the in-memory HashMap from the database on startup.
     */
    @PostConstruct
    public void warmLookupManager() {
        lookupManager.clear();
        List<Vehicle> vehicles = vehicleRepository.findAll();
        for (Vehicle v : vehicles) {
            boolean parked = sessionRepository.findFirstByVehicleNumberAndStatus(v.getVehicleNumber(), SessionStatus.ACTIVE).isPresent();
            VehicleSnapshot snapshot = toSnapshot(v, parked);
            lookupManager.register(snapshot);
        }
        log.info("Warmed VehicleLookupManager HashMap with {} vehicles from database.", vehicles.size());
    }

    @Transactional
    public Vehicle findOrCreateVehicle(String vehicleNumber, String ownerName, String contact, VehicleType type, Priority priority) {
        String normalized = VehicleNumberUtil.normalize(vehicleNumber);
        Optional<Vehicle> existingOpt = vehicleRepository.findByVehicleNumber(normalized);

        if (existingOpt.isPresent()) {
            Vehicle existing = existingOpt.get();
            // Update mutable fields if provided
            if (ownerName != null && !ownerName.isBlank()) existing.setOwnerName(ownerName.trim());
            if (contact != null) existing.setContact(contact.trim());
            if (type != null) existing.setVehicleType(type);
            if (priority != null) existing.setPriority(priority);
            Vehicle saved = vehicleRepository.save(existing);
            syncLookup(saved, isParked(normalized));
            return saved;
        }

        Vehicle newVehicle = new Vehicle(
                normalized,
                ownerName != null ? ownerName.trim() : "Unknown",
                contact != null ? contact.trim() : null,
                type != null ? type : VehicleType.CAR,
                priority != null ? priority : Priority.NORMAL
        );
        Vehicle saved = vehicleRepository.save(newVehicle);
        syncLookup(saved, false);
        return saved;
    }

    public Optional<Vehicle> findByVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null) return Optional.empty();
        return vehicleRepository.findByVehicleNumber(VehicleNumberUtil.normalize(vehicleNumber));
    }

    public boolean isParked(String vehicleNumber) {
        return lookupManager.isParked(vehicleNumber);
    }

    public void setParkedStatus(String vehicleNumber, boolean parked) {
        lookupManager.setParkedStatus(vehicleNumber, parked);
    }

    public List<Vehicle> searchVehicles(String query) {
        if (query == null || query.isBlank()) {
            return vehicleRepository.findAll();
        }
        String clean = query.trim();
        return vehicleRepository.findByVehicleNumberContainingIgnoreCaseOrOwnerNameContainingIgnoreCase(clean, clean);
    }

    public VehicleLookupManager getLookupManager() {
        return lookupManager;
    }

    private void syncLookup(Vehicle v, boolean isParked) {
        lookupManager.register(toSnapshot(v, isParked));
    }

    public VehicleSnapshot toSnapshot(Vehicle v, boolean isParked) {
        if (v == null) return null;
        return new VehicleSnapshot(
                v.getId(),
                v.getVehicleNumber(),
                v.getVehicleType(),
                v.getOwnerName(),
                v.getContact(),
                v.getPriority(),
                isParked
        );
    }
}
