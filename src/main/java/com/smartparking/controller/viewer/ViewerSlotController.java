package com.smartparking.controller.viewer;

import com.smartparking.dto.viewer.ViewerSlotDto;
import com.smartparking.mapper.ViewerMapper;
import com.smartparking.service.SlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/viewer/slots")
public class ViewerSlotController {

    private final SlotService slotService;
    private final ViewerMapper viewerMapper;

    public ViewerSlotController(SlotService slotService, ViewerMapper viewerMapper) {
        this.slotService = slotService;
        this.viewerMapper = viewerMapper;
    }

    @GetMapping
    public ResponseEntity<List<ViewerSlotDto>> getAllSlots() {
        List<ViewerSlotDto> list = slotService.getAllSlots().stream()
                .map(viewerMapper::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }
}
