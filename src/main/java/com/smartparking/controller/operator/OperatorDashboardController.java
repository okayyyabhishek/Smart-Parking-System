package com.smartparking.controller.operator;

import com.smartparking.dto.operator.OperatorDashboardDto;
import com.smartparking.dto.operator.OperatorSessionDto;
import com.smartparking.mapper.OperatorMapper;
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
@RequestMapping("/api/operator/dashboard")
public class OperatorDashboardController {

    private final StatisticsService statisticsService;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final VehicleService vehicleService;
    private final OperatorMapper operatorMapper;

    public OperatorDashboardController(
            StatisticsService statisticsService,
            ParkingSlotRepository slotRepository,
            ParkingSessionRepository sessionRepository,
            VehicleService vehicleService,
            OperatorMapper operatorMapper
    ) {
        this.statisticsService = statisticsService;
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.vehicleService = vehicleService;
        this.operatorMapper = operatorMapper;
    }

    @GetMapping
    public ResponseEntity<OperatorDashboardDto> getDashboard() {
        var stats = statisticsService.getStatistics();
        long totalSlots = slotRepository.count();
        long availableSlots = slotRepository.countByStatus(SlotStatus.AVAILABLE);
        long occupiedSlots = slotRepository.countByStatus(SlotStatus.OCCUPIED);

        List<ParkingSession> recentSessions = sessionRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();

        List<OperatorSessionDto> recentActivity = recentSessions.stream()
                .map(s -> {
                    Vehicle v = vehicleService.findByVehicleNumber(s.getVehicleNumber()).orElse(null);
                    return operatorMapper.toDto(s, v);
                })
                .toList();

        return ResponseEntity.ok(new OperatorDashboardDto(
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
