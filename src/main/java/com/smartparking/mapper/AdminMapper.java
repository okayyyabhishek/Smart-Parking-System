package com.smartparking.mapper;

import com.smartparking.dto.admin.AdminSessionDto;
import com.smartparking.dto.admin.AdminSlotDto;
import com.smartparking.dto.admin.AdminUserDto;
import com.smartparking.dto.admin.AdminVehicleDto;
import com.smartparking.dto.admin.AuditLogDto;
import com.smartparking.model.AppUser;
import com.smartparking.model.AuditLog;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.util.DurationUtil;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public AdminVehicleDto toDto(Vehicle vehicle, boolean isParked) {
        if (vehicle == null) return null;
        return new AdminVehicleDto(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                vehicle.getVehicleType(),
                vehicle.getOwnerName(),
                vehicle.getContact(),
                vehicle.getPriority(),
                isParked
        );
    }

    public AdminSlotDto toDto(ParkingSlot slot) {
        if (slot == null) return null;
        String curPlate = slot.getCurrentVehicle() != null ? slot.getCurrentVehicle().getVehicleNumber() : null;
        String curOwner = slot.getCurrentVehicle() != null ? slot.getCurrentVehicle().getOwnerName() : null;
        return new AdminSlotDto(
                slot.getId(),
                slot.getSlotNumber(),
                slot.getSlotType(),
                slot.getZone(),
                slot.getFloor(),
                slot.getStatus(),
                curPlate,
                curOwner
        );
    }

    public AdminSessionDto toDto(ParkingSession session, Vehicle vehicle) {
        if (session == null) return null;
        String owner = vehicle != null ? vehicle.getOwnerName() : "Unknown";
        VehicleType type = vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR;
        return new AdminSessionDto(
                session.getId(),
                session.getVehicleNumber(),
                type,
                owner,
                session.getSlotNumber(),
                session.getEntryTime(),
                session.getExitTime(),
                session.getDurationSeconds(),
                DurationUtil.formatDuration(session.getDurationSeconds()),
                session.getStatus(),
                session.getEnqueuedAt()
        );
    }

    public AdminUserDto toDto(AppUser user) {
        if (user == null) return null;
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.isMustChangePassword(),
                user.getCreatedAt()
        );
    }

    public AuditLogDto toDto(AuditLog log) {
        if (log == null) return null;
        return new AuditLogDto(
                log.getId(),
                log.getUsername(),
                log.getRole(),
                log.getAction(),
                log.getTarget(),
                log.getIp(),
                log.getTimestamp()
        );
    }
}
