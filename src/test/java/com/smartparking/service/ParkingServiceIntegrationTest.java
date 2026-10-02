package com.smartparking.service;

import com.smartparking.exception.AlreadyParkedException;
import com.smartparking.exception.AlreadyWaitingException;
import com.smartparking.exception.SlotOccupiedException;
import com.smartparking.exception.VehicleNotFoundException;
import com.smartparking.model.Priority;
import com.smartparking.model.Role;
import com.smartparking.model.SessionStatus;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("ParkingService and Demo Scenario TRD §14 Integration Tests")
class ParkingServiceIntegrationTest {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private ParkingSlotRepository slotRepository;

    @Autowired
    private ParkingSessionRepository sessionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        // DataSeeder populates baseline slots and vehicles
    }

    @Test
    @DisplayName("Executes complete end-to-end TRD §14 Demonstration Scenario")
    void testTrd14DemonstrationScenario() {
        // Step 1: Park PB10AB1234 (CAR) -> A-01
        var carResult = parkingService.enterParking(
                "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertTrue(carResult.allocated());
        assertEquals("A-01", carResult.slotNumber());
        assertEquals(SlotType.NORMAL, carResult.slotType());

        // Step 2: Park PB10XY4567 (EV) -> A-03 (EV slot preferred)
        var evResult = parkingService.enterParking(
                "PB10XY4567", VehicleType.EV, "S. Singh", "9876543211", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertTrue(evResult.allocated());
        assertEquals("A-03", evResult.slotNumber());
        assertEquals(SlotType.EV, evResult.slotType());

        // Step 3: Park PB08CD9876 (SUV) -> A-04 (LARGE slot)
        var suvResult = parkingService.enterParking(
                "PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertTrue(suvResult.allocated());
        assertEquals("A-04", suvResult.slotNumber());
        assertEquals(SlotType.LARGE, suvResult.slotType());

        // Verify invariant: currentParked == OCCUPIED slots
        var stats = statisticsService.getStatistics();
        long occupiedSlotCount = slotRepository.countByStatus(SlotStatus.OCCUPIED);
        assertEquals(3, stats.currentParked());
        assertEquals(occupiedSlotCount, stats.currentParked());

        // Step 4: Park a second SUV (PB09ZZ0001) -> No LARGE slot free -> Enters WAITING at position 1
        var secondSuvResult = parkingService.enterParking(
                "PB09ZZ0001", VehicleType.SUV, "Second SUV", "9876543213", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertFalse(secondSuvResult.allocated());
        assertEquals(1, secondSuvResult.queuePosition());

        // Step 5: Park a third SUV with HIGH priority (PB09ZZ0002) -> Jumps to queue position 1
        var thirdSuvResult = parkingService.enterParking(
                "PB09ZZ0002", VehicleType.SUV, "Third SUV High", "9876543214", Priority.HIGH,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertFalse(thirdSuvResult.allocated());
        assertEquals(1, thirdSuvResult.queuePosition(), "HIGH priority SUV must jump ahead to position 1");

        // Verify queue positions
        assertEquals(1, waitingQueueService.getQueuePosition("PB09ZZ0002"));
        assertEquals(2, waitingQueueService.getQueuePosition("PB09ZZ0001"));

        // Step 6: Exit PB08CD9876 -> A-04 freed -> HIGH SUV gets A-04 automatically
        var exitResult = parkingService.exitParking("PB08CD9876", "operator", Role.OPERATOR, "127.0.0.1");
        assertEquals("A-04", exitResult.releasedSlot());
        assertTrue(exitResult.reallocation().performed(), "Automatic reallocation must take place");
        assertEquals("PB09ZZ0002", exitResult.reallocation().vehicleNumber(), "HIGH priority SUV must receive the reallocated slot");
        assertEquals("A-04", exitResult.reallocation().slotNumber());

        // Verify other SUV is now #1 in waiting queue
        assertEquals(1, waitingQueueService.getQueuePosition("PB09ZZ0001"));
        assertEquals(1, waitingQueueService.size());

        // Verify A-04 slot is now occupied by the HIGH SUV
        var a04Slot = slotService.findBySlotNumber("A-04").orElseThrow();
        assertEquals(SlotStatus.OCCUPIED, a04Slot.getStatus());
        assertEquals("PB09ZZ0002", a04Slot.getCurrentVehicle().getVehicleNumber());

        // Step 7: Verify final statistics: currentParked == OCCUPIED slots
        var updatedStats = statisticsService.getStatistics();
        long updatedOccupiedCount = slotRepository.countByStatus(SlotStatus.OCCUPIED);
        assertEquals(updatedOccupiedCount, updatedStats.currentParked());
        assertEquals(1, updatedStats.totalVehiclesServed()); // One exit completed
        assertEquals(1, updatedStats.waitingVehicles());
    }

    @Test
    @DisplayName("Guards against duplicate parking and already-waiting registration")
    void testDuplicateGuards() {
        parkingService.enterParking(
                "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );

        // Attempting to park the same vehicle again must throw AlreadyParkedException
        assertThrows(AlreadyParkedException.class, () ->
                parkingService.enterParking(
                        "pb10 ab-1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL,
                        "operator", Role.OPERATOR, "127.0.0.1"
                )
        );
    }

    @Test
    @DisplayName("Exit non-parked vehicle throws VehicleNotFoundException")
    void testExitNonParkedVehicle() {
        assertThrows(VehicleNotFoundException.class, () ->
                parkingService.exitParking("UNKNOWN999", "operator", Role.OPERATOR, "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Cannot set an occupied slot to maintenance")
    void testCannotSetOccupiedSlotToMaintenance() {
        parkingService.enterParking(
                "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );

        assertThrows(SlotOccupiedException.class, () ->
                slotService.setMaintenanceStatus("A-01", true)
        );
    }
}
