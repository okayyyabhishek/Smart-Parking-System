package com.smartparking.controller.viewer;

import com.smartparking.dto.viewer.ViewerDashboardDto;
import com.smartparking.dto.viewer.ViewerSessionDto;
import com.smartparking.mapper.ViewerMapper;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.Vehicle;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.service.StatisticsService;
import com.smartparking.service.VehicleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/viewer/dashboard")
public class ViewerDashboardController {

    private final StatisticsService statisticsService;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final VehicleService vehicleService;
    private final ViewerMapper viewerMapper;

    public ViewerDashboardController(
            StatisticsService statisticsService,
            ParkingSlotRepository slotRepository,
            ParkingSessionRepository sessionRepository,
            VehicleService vehicleService,
            ViewerMapper viewerMapper
    ) {
        this.statisticsService = statisticsService;
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.vehicleService = vehicleService;
        this.viewerMapper = viewerMapper;
    }

    @GetMapping
    public ResponseEntity<ViewerDashboardDto> getDashboard() {
        var stats = statisticsService.getStatistics();
        long totalSlots = slotRepository.count();
        long availableSlots = slotRepository.countByStatus(SlotStatus.AVAILABLE);
        long occupiedSlots = slotRepository.countByStatus(SlotStatus.OCCUPIED);

        List<ParkingSession> recentSessions = sessionRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();

        List<ViewerSessionDto> recentActivity = recentSessions.stream()
                .map(s -> {
                    Vehicle v = vehicleService.findByVehicleNumber(s.getVehicleNumber()).orElse(null);
                    return viewerMapper.toDto(s, v);
                })
                .toList();

        return ResponseEntity.ok(new ViewerDashboardDto(
                totalSlots,
                availableSlots,
                occupiedSlots,
                stats.waitingVehicles(),
                stats.todaysEntries(),
                stats.todaysExits(),
                stats.availabilityByType(),
                recentActivity
        ));
    }
}
