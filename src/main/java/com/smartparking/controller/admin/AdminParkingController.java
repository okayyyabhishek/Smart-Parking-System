package com.smartparking.controller.admin;

import com.smartparking.dto.admin.AdminSessionDto;
import com.smartparking.dto.common.ExitRequest;
import com.smartparking.dto.common.VehicleEntryRequest;
import com.smartparking.dto.operator.OperatorWaitingDto;
import com.smartparking.ds.ExitRecord;
import com.smartparking.ds.WaitingEntry;
import com.smartparking.mapper.AdminMapper;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.Role;
import com.smartparking.model.Vehicle;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.service.ParkingService;
import com.smartparking.service.VehicleService;
import com.smartparking.service.WaitingQueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/parking")
public class AdminParkingController {

    private final ParkingService parkingService;
    private final WaitingQueueService waitingQueueService;
    private final ParkingSessionRepository sessionRepository;
    private final VehicleService vehicleService;
    private final AdminMapper adminMapper;

    public AdminParkingController(
            ParkingService parkingService,
            WaitingQueueService waitingQueueService,
            ParkingSessionRepository sessionRepository,
            VehicleService vehicleService,
            AdminMapper adminMapper
    ) {
        this.parkingService = parkingService;
        this.waitingQueueService = waitingQueueService;
        this.sessionRepository = sessionRepository;
        this.vehicleService = vehicleService;
        this.adminMapper = adminMapper;
    }

    @PostMapping("/entry")
    public ResponseEntity<ParkingService.ParkingEntryResult> enter(
            @Valid @RequestBody VehicleEntryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = parkingService.enterParking(
                request.vehicleNumber(),
                request.vehicleType(),
                request.ownerName(),
                request.contact(),
                request.priority(),
                authentication.getName(),
                Role.SUPER_ADMIN,
                httpRequest.getRemoteAddr()
        );

        HttpStatus status = result.allocated() ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/exit")
    public ResponseEntity<ParkingService.ParkingExitResult> exit(
            @Valid @RequestBody ExitRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = parkingService.exitParking(
                request.vehicleNumber(),
                authentication.getName(),
                Role.SUPER_ADMIN,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AdminSessionDto>> getHistory(
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

        List<AdminSessionDto> dtos = new ArrayList<>(sessions.stream()
                .map(s -> {
                    Vehicle v = vehicleService.findByVehicleNumber(s.getVehicleNumber()).orElse(null);
                    return adminMapper.toDto(s, v);
                })
                .toList());

        // Sort by duration or entryTime
        Comparator<AdminSessionDto> comparator;
        if ("duration".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(AdminSessionDto::durationSeconds, Comparator.nullsLast(Long::compareTo));
        } else {
            comparator = Comparator.comparing(AdminSessionDto::entryTime, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        dtos.sort(comparator);

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<OperatorWaitingDto>> getWaitingQueue() {
        List<WaitingEntry> snapshot = waitingQueueService.getSnapshot();
        List<OperatorWaitingDto> result = new ArrayList<>();

        for (int i = 0; i < snapshot.size(); i++) {
            WaitingEntry entry = snapshot.get(i);
            Vehicle vehicle = vehicleService.findByVehicleNumber(entry.vehicleNumber()).orElse(null);
            String owner = vehicle != null ? vehicle.getOwnerName() : "Unknown";
            String contact = vehicle != null ? vehicle.getContact() : null;

            result.add(new OperatorWaitingDto(
                    i + 1,
                    entry.vehicleNumber(),
                    entry.type(),
                    entry.priority(),
                    owner,
                    contact, // Admin sees full contact
                    entry.enqueuedAt()
            ));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent-exits")
    public ResponseEntity<List<ExitRecord>> getRecentExits() {
        return ResponseEntity.ok(parkingService.getHistoryStack().getRecentExits());
    }
}
