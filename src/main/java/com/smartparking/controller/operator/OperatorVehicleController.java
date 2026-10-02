package com.smartparking.controller.operator;

import com.smartparking.dto.operator.OperatorVehicleDto;
import com.smartparking.mapper.OperatorMapper;
import com.smartparking.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operator/vehicles")
public class OperatorVehicleController {

    private final VehicleService vehicleService;
    private final OperatorMapper operatorMapper;

    public OperatorVehicleController(VehicleService vehicleService, OperatorMapper operatorMapper) {
        this.vehicleService = vehicleService;
        this.operatorMapper = operatorMapper;
    }

    @GetMapping
    public ResponseEntity<List<OperatorVehicleDto>> getVehicles(@RequestParam(required = false) String search) {
        List<OperatorVehicleDto> list = vehicleService.searchVehicles(search).stream()
                .map(v -> operatorMapper.toDto(v, vehicleService.isParked(v.getVehicleNumber())))
                .toList();
        return ResponseEntity.ok(list);
    }
}
