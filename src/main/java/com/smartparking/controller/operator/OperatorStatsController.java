package com.smartparking.controller.operator;

import com.smartparking.dto.common.StatisticsDto;
import com.smartparking.service.StatisticsService;
import com.smartparking.util.DurationUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operator/statistics")
public class OperatorStatsController {

    private final StatisticsService statisticsService;

    public OperatorStatsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ResponseEntity<StatisticsDto> getStatistics() {
        var data = statisticsService.getStatistics();
        StatisticsDto dto = new StatisticsDto(
                data.totalVehiclesServed(),
                data.currentParked(),
                data.availableSlots(),
                data.waitingVehicles(),
                data.averageParkingDurationSeconds(),
                DurationUtil.formatDuration(data.averageParkingDurationSeconds()),
                data.mostUsedSlot(),
                data.mostCommonVehicleType(),
                data.todaysEntries(),
                data.todaysExits(),
                data.availabilityByType()
        );
        return ResponseEntity.ok(dto);
    }
}
