package com.smartparking.controller.admin;

import com.smartparking.dto.admin.AdminCreateSlotRequest;
import com.smartparking.dto.admin.AdminSlotDto;
import com.smartparking.mapper.AdminMapper;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Role;
import com.smartparking.service.AuditService;
import com.smartparking.service.SlotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/slots")
public class AdminSlotController {

    private final SlotService slotService;
    private final AdminMapper adminMapper;
    private final AuditService auditService;

    public AdminSlotController(SlotService slotService, AdminMapper adminMapper, AuditService auditService) {
        this.slotService = slotService;
        this.adminMapper = adminMapper;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AdminSlotDto>> getAllSlots() {
        List<AdminSlotDto> slots = slotService.getAllSlots().stream()
                .map(adminMapper::toDto)
                .toList();
        return ResponseEntity.ok(slots);
    }

    @PostMapping
    public ResponseEntity<AdminSlotDto> createSlot(
            @Valid @RequestBody AdminCreateSlotRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ParkingSlot created = slotService.createSlot(request.slotNumber(), request.slotType(), request.zone(), request.floor());
        auditService.record(authentication.getName(), Role.SUPER_ADMIN, "SLOT_CREATE",
                "Created slot " + created.getSlotNumber() + " (" + created.getSlotType() + ")", httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(adminMapper.toDto(created));
    }

    @PostMapping("/{slotNumber}/maintenance")
    public ResponseEntity<AdminSlotDto> setMaintenance(
            @PathVariable String slotNumber,
            @RequestParam boolean enabled,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ParkingSlot updated = slotService.setMaintenanceStatus(slotNumber, enabled);
        auditService.record(authentication.getName(), Role.SUPER_ADMIN, "SLOT_MAINTENANCE",
                "Slot " + slotNumber + " maintenance set to " + enabled, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(adminMapper.toDto(updated));
    }
}
