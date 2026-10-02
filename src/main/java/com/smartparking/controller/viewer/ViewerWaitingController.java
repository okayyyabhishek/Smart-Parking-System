package com.smartparking.controller.viewer;

import com.smartparking.dto.viewer.ViewerWaitingDto;
import com.smartparking.ds.WaitingEntry;
import com.smartparking.mapper.ViewerMapper;
import com.smartparking.service.WaitingQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/viewer/parking/waiting")
public class ViewerWaitingController {

    private final WaitingQueueService waitingQueueService;
    private final ViewerMapper viewerMapper;

    public ViewerWaitingController(WaitingQueueService waitingQueueService, ViewerMapper viewerMapper) {
        this.waitingQueueService = waitingQueueService;
        this.viewerMapper = viewerMapper;
    }

    @GetMapping
    public ResponseEntity<List<ViewerWaitingDto>> getWaitingQueue() {
        List<WaitingEntry> snapshot = waitingQueueService.getSnapshot();
        List<ViewerWaitingDto> list = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            list.add(viewerMapper.toWaitingDto(i + 1, snapshot.get(i)));
        }
        return ResponseEntity.ok(list);
    }
}
