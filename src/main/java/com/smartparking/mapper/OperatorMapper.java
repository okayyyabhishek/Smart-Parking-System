package com.smartparking.mapper;

import com.smartparking.dto.operator.OperatorSessionDto;
import com.smartparking.dto.operator.OperatorSlotDto;
import com.smartparking.dto.operator.OperatorVehicleDto;
import com.smartparking.dto.operator.OperatorWaitingDto;
import com.smartparking.ds.WaitingEntry;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.util.DurationUtil;
import org.springframework.stereotype.Component;

@Component
public class OperatorMapper {

    /**
     * Masks contact number, preserving only the last 4 digits (e.g. ••••••3210).
     */
    public static String maskContact(String contact) {
        if (contact == null || contact.isBlank()) {
            return "";
        }
        String clean = contact.trim();
        if (clean.length() <= 4) {
            return "••••";
        }
        int bulletCount = clean.length() - 4;
        return "•".repeat(bulletCount) + clean.substring(clean.length() - 4);
    }

    public OperatorVehicleDto toDto(Vehicle vehicle, boolean isParked) {
        if (vehicle == null) return null;
        return new OperatorVehicleDto(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                vehicle.getVehicleType(),
                vehicle.getOwnerName(),
                maskContact(vehicle.getContact()),
                vehicle.getPriority(),
                isParked
        );
    }

    public OperatorSlotDto toDto(ParkingSlot slot) {
        if (slot == null) return null;
        String curPlate = slot.getCurrentVehicle() != null ? slot.getCurrentVehicle().getVehicleNumber() : null;
        VehicleType curType = slot.getCurrentVehicle() != null ? slot.getCurrentVehicle().getVehicleType() : null;
        return new OperatorSlotDto(
                slot.getId(),
                slot.getSlotNumber(),
                slot.getSlotType(),
                slot.getZone(),
                slot.getFloor(),
                slot.getStatus(),
                curPlate,
                curType
        );
    }

    public OperatorSessionDto toDto(ParkingSession session, Vehicle vehicle) {
        if (session == null) return null;
        String owner = vehicle != null ? vehicle.getOwnerName() : "Unknown";
        VehicleType type = vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR;
        return new OperatorSessionDto(
                session.getId(),
                session.getVehicleNumber(),
                type,
                owner,
                session.getSlotNumber(),
                session.getEntryTime(),
                session.getExitTime(),
                session.getDurationSeconds(),
                DurationUtil.formatDuration(session.getDurationSeconds()),
                session.getStatus()
        );
    }

    public OperatorWaitingDto toWaitingDto(int position, WaitingEntry entry, Vehicle vehicle) {
        if (entry == null) return null;
        String owner = vehicle != null ? vehicle.getOwnerName() : "Unknown";
        String contact = vehicle != null ? vehicle.getContact() : null;
        return new OperatorWaitingDto(
                position,
                entry.vehicleNumber(),
                entry.type(),
                entry.priority(),
                owner,
                maskContact(contact),
                entry.enqueuedAt()
        );
    }
}
