package com.smartparking.controller.operator;

import com.smartparking.dto.operator.OperatorSlotDto;
import com.smartparking.mapper.OperatorMapper;
import com.smartparking.service.SlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operator/slots")
public class OperatorSlotController {

    private final SlotService slotService;
    private final OperatorMapper operatorMapper;

    public OperatorSlotController(SlotService slotService, OperatorMapper operatorMapper) {
        this.slotService = slotService;
        this.operatorMapper = operatorMapper;
    }

    @GetMapping
    public ResponseEntity<List<OperatorSlotDto>> getAllSlots() {
        List<OperatorSlotDto> list = slotService.getAllSlots().stream()
                .map(operatorMapper::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }
}
