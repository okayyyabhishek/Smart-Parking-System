package com.smartparking.mapper;

import com.smartparking.dto.viewer.ViewerSessionDto;
import com.smartparking.dto.viewer.ViewerSlotDto;
import com.smartparking.dto.viewer.ViewerWaitingDto;
import com.smartparking.ds.WaitingEntry;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.util.DurationUtil;
import org.springframework.stereotype.Component;

@Component
public class ViewerMapper {

    /**
     * Masks vehicle license plate per TRD §8:
     * Keep first 4 and last 2 chars, rest '•' -> PB10••••34 (exact length preserved).
     */
    public static String maskPlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return "";
        }
        String clean = plate.trim().toUpperCase();
        if (clean.length() <= 4) {
            return "••••";
        }
        if (clean.length() <= 6) {
            return clean.substring(0, 2) + "••" + clean.substring(clean.length() - 2);
        }
        int maskedCount = clean.length() - 6;
        return clean.substring(0, 4) + "•".repeat(maskedCount) + clean.substring(clean.length() - 2);
    }

    public ViewerSlotDto toDto(ParkingSlot slot) {
        if (slot == null) return null;
        return new ViewerSlotDto(
                slot.getId(),
                slot.getSlotNumber(),
                slot.getSlotType(),
                slot.getZone(),
                slot.getFloor(),
                slot.getStatus()
        );
    }

    public ViewerSessionDto toDto(ParkingSession session, Vehicle vehicle) {
        if (session == null) return null;
        VehicleType type = vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR;
        return new ViewerSessionDto(
                session.getId(),
                maskPlate(session.getVehicleNumber()),
                type,
                session.getSlotNumber(),
                session.getEntryTime(),
                session.getExitTime(),
                session.getDurationSeconds(),
                DurationUtil.formatDuration(session.getDurationSeconds()),
                session.getStatus()
        );
    }

    public ViewerWaitingDto toWaitingDto(int position, WaitingEntry entry) {
        if (entry == null) return null;
        return new ViewerWaitingDto(
                position,
                entry.type(),
                entry.priority(),
                entry.enqueuedAt()
        );
    }
}
