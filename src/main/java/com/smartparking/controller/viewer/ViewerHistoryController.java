package com.smartparking.controller.viewer;

import com.smartparking.dto.viewer.ViewerSessionDto;
import com.smartparking.mapper.ViewerMapper;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.Vehicle;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/viewer/parking/history")
public class ViewerHistoryController {

    private final ParkingSessionRepository sessionRepository;
    private final VehicleService vehicleService;
    private final ViewerMapper viewerMapper;

    public ViewerHistoryController(
            ParkingSessionRepository sessionRepository,
            VehicleService vehicleService,
            ViewerMapper viewerMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.vehicleService = vehicleService;
        this.viewerMapper = viewerMapper;
    }

    @GetMapping
    public ResponseEntity<List<ViewerSessionDto>> getHistory(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "entryTime") String sortBy,
            @RequestParam(defaultValue = "desc") String order
    ) {
        List<ParkingSession> sessions = sessionRepository.findAll();

        if (search != null && !search.isBlank()) {
            String query = search.trim().toUpperCase();
            sessions = sessions.stream()
                    .filter(s -> s.getVehicleNumber().contains(query) ||
                            (s.getSlotNumber() != null && s.getSlotNumber().toUpperCase().contains(query)))
                    .toList();
        }

        List<ViewerSessionDto> dtos = new ArrayList<>(sessions.stream()
                .map(s -> {
                    Vehicle v = vehicleService.findByVehicleNumber(s.getVehicleNumber()).orElse(null);
                    return viewerMapper.toDto(s, v);
                })
                .toList());

        Comparator<ViewerSessionDto> comparator;
        if ("duration".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(ViewerSessionDto::durationSeconds, Comparator.nullsLast(Long::compareTo));
        } else {
            comparator = Comparator.comparing(ViewerSessionDto::entryTime, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        dtos.sort(comparator);

        return ResponseEntity.ok(dtos);
    }
}
