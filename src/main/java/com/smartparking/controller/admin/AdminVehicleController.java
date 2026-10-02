package com.smartparking.controller.admin;

import com.smartparking.dto.admin.AdminVehicleDto;
import com.smartparking.mapper.AdminMapper;
import com.smartparking.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicles")
public class AdminVehicleController {

    private final VehicleService vehicleService;
    private final AdminMapper adminMapper;

    public AdminVehicleController(VehicleService vehicleService, AdminMapper adminMapper) {
        this.vehicleService = vehicleService;
        this.adminMapper = adminMapper;
    }

    @GetMapping
    public ResponseEntity<List<AdminVehicleDto>> getVehicles(@RequestParam(required = false) String search) {
        List<AdminVehicleDto> list = vehicleService.searchVehicles(search).stream()
                .map(v -> adminMapper.toDto(v, vehicleService.isParked(v.getVehicleNumber())))
                .toList();
        return ResponseEntity.ok(list);
    }
}
