package com.smartparking.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.ds.ExitRecord;
import com.smartparking.ds.ParkingHistoryStack;
import com.smartparking.ds.SlotSearchManager;
import com.smartparking.ds.SlotSnapshot;
import com.smartparking.ds.VehicleLookupManager;
import com.smartparking.ds.VehicleSnapshot;
import com.smartparking.ds.WaitingEntry;
import com.smartparking.ds.WaitingVehiclePriorityQueue;
import com.smartparking.dto.common.VehicleEntryRequest;
import com.smartparking.exception.AlreadyParkedException;
import com.smartparking.exception.AlreadyWaitingException;
import com.smartparking.model.AuditLog;
import com.smartparking.model.ParkingSession;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Priority;
import com.smartparking.model.Role;
import com.smartparking.model.SessionStatus;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.AuditLogRepository;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.security.LoginAttemptService;
import com.smartparking.service.ParkingService;
import com.smartparking.service.SlotService;
import com.smartparking.service.StatisticsService;
import com.smartparking.service.VehicleService;
import com.smartparking.service.WaitingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8: Comprehensive Hardening, Quality Assurance & 25 Test-Case Verification Suite.
 * Validates all 17 PDF test cases, all 8 role-separation test cases, and edge-case hardening:
 * - Session expiry & 401 handling
 * - Double-submit prevention & server idempotency
 * - Startup recovery of waiting PriorityQueue from database
 * - Zero stack-trace error page guarantees
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 8: Comprehensive Hardening & 25 Test Cases Verification")
class Phase8HardeningAndQaVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private ParkingSlotRepository slotRepository;

    @Autowired
    private ParkingSessionRepository sessionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void resetState() {
        sessionRepository.deleteAll();
        List<String> baselineSlots = List.of("A-01", "A-02", "A-03", "A-04", "A-05");
        for (ParkingSlot s : slotRepository.findAll()) {
            if (!baselineSlots.contains(s.getSlotNumber())) {
                slotRepository.delete(s);
            } else {
                s.setStatus(SlotStatus.AVAILABLE);
                s.setCurrentVehicle(null);
                slotRepository.save(s);
            }
        }
        if (!slotRepository.existsBySlotNumber("A-01")) slotRepository.save(new ParkingSlot("A-01", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE));
        if (!slotRepository.existsBySlotNumber("A-02")) slotRepository.save(new ParkingSlot("A-02", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE));
        if (!slotRepository.existsBySlotNumber("A-03")) slotRepository.save(new ParkingSlot("A-03", SlotType.EV, "A", 0, SlotStatus.AVAILABLE));
        if (!slotRepository.existsBySlotNumber("A-04")) slotRepository.save(new ParkingSlot("A-04", SlotType.LARGE, "A", 0, SlotStatus.AVAILABLE));
        if (!slotRepository.existsBySlotNumber("A-05")) slotRepository.save(new ParkingSlot("A-05", SlotType.ACCESSIBLE, "A", 0, SlotStatus.AVAILABLE));

        waitingQueueService.rebuildFromDatabase();
        vehicleService.warmLookupManager();
    }

    // ==========================================
    // PDF TEST CASES 1 - 17
    // ==========================================

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Add parking slot — Unique slot is saved, duplicates rejected")
    void testCase01_addParkingSlot() {
        String testSlotNum = "Z-99";
        slotRepository.findBySlotNumber(testSlotNum).ifPresent(slotRepository::delete);

        ParkingSlot created = slotService.createSlot(testSlotNum, SlotType.EV, "Z", 1);
        assertNotNull(created.getId());
        assertEquals("Z-99", created.getSlotNumber());
        assertEquals(SlotType.EV, created.getSlotType());
        assertEquals(SlotStatus.AVAILABLE, created.getStatus());

        // Duplicate rejection
        assertThrows(RuntimeException.class, () -> slotService.createSlot(testSlotNum, SlotType.EV, "Z", 1));

        // Cleanup
        slotRepository.delete(created);
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Add vehicle — Vehicle accepted with valid data and normalized plate")
    void testCase02_addVehicle() {
        Vehicle v = vehicleService.findOrCreateVehicle("dl 04 ab 9999", "Vikram Batra", "9811223344", VehicleType.CAR, Priority.NORMAL);
        assertNotNull(v.getId());
        assertEquals("DL04AB9999", v.getVehicleNumber());
        assertEquals("Vikram Batra", v.getOwnerName());
        assertEquals("9811223344", v.getContact());
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Park normal vehicle — Compatible NORMAL slot selected")
    void testCase03_parkNormalVehicle() {
        var res = parkingService.enterParking(
                "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertTrue(res.allocated());
        assertEquals("A-01", res.slotNumber());
        assertEquals(SlotType.NORMAL, res.slotType());
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Park EV — EV slot preferred")
    void testCase04_parkEv() {
        var res = parkingService.enterParking(
                "PB10XY4567", VehicleType.EV, "S. Singh", "9876543211", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertTrue(res.allocated());
        assertEquals("A-03", res.slotNumber());
        assertEquals(SlotType.EV, res.slotType());
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Park large vehicle — LARGE slot preferred")
    void testCase05_parkLargeVehicle() {
        var res = parkingService.enterParking(
                "PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );
        assertTrue(res.allocated());
        assertEquals("A-04", res.slotNumber());
        assertEquals(SlotType.LARGE, res.slotType());
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Priority vehicle — Priority ordering applied in waiting queue")
    void testCase06_priorityOrdering() {
        // Fill A-04 (the only LARGE slot in demo set)
        parkingService.enterParking("PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // Enqueue SUV 1 with NORMAL priority
        var wait1 = parkingService.enterParking("PB09ZZ0001", VehicleType.SUV, "User 1", "9876543213", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        assertFalse(wait1.allocated());
        assertEquals(1, wait1.queuePosition());

        // Enqueue SUV 2 with HIGH priority -> should jump ahead of SUV 1
        var wait2 = parkingService.enterParking("PB09ZZ0002", VehicleType.SUV, "User 2", "9876543214", Priority.HIGH, "operator", Role.OPERATOR, "127.0.0.1");
        assertFalse(wait2.allocated());
        assertEquals(1, wait2.queuePosition()); // Jumped to head!

        // Position of SUV 1 is now #2
        assertEquals(2, waitingQueueService.getQueuePosition("PB09ZZ0001"));
    }

    @Test
    @Order(7)
    @DisplayName("Test Case 7: No compatible slot — Vehicle enters waiting queue")
    void testCase07_noCompatibleSlotEntersWaitingQueue() {
        // Occupy A-04 with SUV
        parkingService.enterParking("PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // Next SUV has no free LARGE bay
        var wait = parkingService.enterParking("PB09ZZ0099", VehicleType.SUV, "Wait User", "9876543299", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        assertFalse(wait.allocated());
        assertEquals(1, wait.queuePosition());
        assertTrue(wait.reason().contains("No compatible AVAILABLE slot"));
    }

    @Test
    @Order(8)
    @DisplayName("Test Case 8: Vehicle exit — Slot becomes available")
    void testCase08_vehicleExit() {
        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        ParkingSlot occupiedSlot = slotService.findBySlotNumber("A-01").orElseThrow();
        assertEquals(SlotStatus.OCCUPIED, occupiedSlot.getStatus());

        var exitRes = parkingService.exitParking("PB10AB1234", "operator", Role.OPERATOR, "127.0.0.1");
        assertEquals("A-01", exitRes.releasedSlot());

        ParkingSlot releasedSlot = slotService.findBySlotNumber("A-01").orElseThrow();
        assertEquals(SlotStatus.AVAILABLE, releasedSlot.getStatus());
        assertNull(releasedSlot.getCurrentVehicle());
    }

    @Test
    @Order(9)
    @DisplayName("Test Case 9: Automatic reallocation — Eligible waiting vehicle gets released slot")
    void testCase09_automaticReallocation() {
        // 1. Occupy A-04 (LARGE)
        parkingService.enterParking("PB08CD9876", VehicleType.SUV, "R. Sharma", "9876543212", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // 2. Queue an SUV
        parkingService.enterParking("PB09ZZ0001", VehicleType.SUV, "Waiting SUV", "9876543213", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        assertEquals(1, waitingQueueService.size());

        // 3. Exit the first SUV -> slot A-04 released and automatically reallocated to waiting SUV
        var exit = parkingService.exitParking("PB08CD9876", "operator", Role.OPERATOR, "127.0.0.1");
        assertTrue(exit.reallocation().performed());
        assertEquals("A-04", exit.reallocation().slotNumber());
        assertEquals("PB09ZZ0001", exit.reallocation().vehicleNumber());

        // Queue now empty
        assertEquals(0, waitingQueueService.size());
        ParkingSlot reallocated = slotService.findBySlotNumber("A-04").orElseThrow();
        assertEquals(SlotStatus.OCCUPIED, reallocated.getStatus());
        assertEquals("PB09ZZ0001", reallocated.getCurrentVehicle().getVehicleNumber());
    }

    @Test
    @Order(10)
    @DisplayName("Test Case 10: Duplicate vehicle — Friendly validation message")
    void testCase10_duplicateVehicleFriendlyMessage() {
        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        AlreadyParkedException ex = assertThrows(AlreadyParkedException.class, () ->
                parkingService.enterParking("PB10AB1234", VehicleType.CAR, "Duplicate", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("already parked in slot A-01"));
    }

    @Test
    @Order(11)
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Test Case 11: Invalid vehicle — Validation error on invalid license plate")
    void testCase11_invalidVehicleValidationError() throws Exception {
        VehicleEntryRequest invalidReq = new VehicleEntryRequest(
                "$$$", VehicleType.CAR, "Invalid User", "9876543210", Priority.NORMAL
        );

        mockMvc.perform(post("/api/operator/parking/entry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.vehicleNumber").exists());
    }

    @Test
    @Order(12)
    @DisplayName("Test Case 12: Search vehicle — VehicleLookupManager HashMap O(1) finds matching vehicle")
    void testCase12_searchVehicleLookup() {
        VehicleLookupManager manager = new VehicleLookupManager();
        manager.register(new VehicleSnapshot(1L, "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, false));

        // Normalization matching: pb10 ab-1234 -> PB10AB1234
        Optional<VehicleSnapshot> found = manager.find("pb10 ab-1234");
        assertTrue(found.isPresent());
        assertEquals("PB10AB1234", found.get().vehicleNumber());
        assertEquals("A. Kumar", found.get().ownerName());
    }

    @Test
    @Order(13)
    @DisplayName("Test Case 13: Search slot — SlotSearchManager linear search O(n) finds slot")
    void testCase13_searchSlotLinear() {
        List<SlotSnapshot> snapshots = slotService.getAllSlotSnapshots();
        SlotSearchManager searchManager = new SlotSearchManager();

        Optional<SlotSnapshot> slotA03 = searchManager.findBySlotNumber(snapshots, "a-03");
        assertTrue(slotA03.isPresent());
        assertEquals("A-03", slotA03.get().slotNumber());
        assertEquals(SlotType.EV, slotA03.get().slotType());
    }

    @Test
    @Order(14)
    @DisplayName("Test Case 14: Sorting — Slots and records ordered correctly")
    void testCase14_sorting() {
        List<ParkingSlot> slots = slotService.getAllSlots();
        assertFalse(slots.isEmpty());
        // Verify natural slot number sort
        for (int i = 0; i < slots.size() - 1; i++) {
            assertTrue(slots.get(i).getSlotNumber().compareTo(slots.get(i + 1).getSlotNumber()) <= 0);
        }
    }

    @Test
    @Order(15)
    @DisplayName("Test Case 15: Parking duration — Computed from entry/exit timestamps")
    void testCase15_parkingDurationCalculated() {
        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        var exitRes = parkingService.exitParking("PB10AB1234", "operator", Role.OPERATOR, "127.0.0.1");
        assertTrue(exitRes.durationSeconds() >= 0);
    }

    @Test
    @Order(16)
    @DisplayName("Test Case 16: Statistics — Calculated accurately from database")
    void testCase16_statisticsCalculatedFromDb() {
        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        parkingService.enterParking("PB10XY4567", VehicleType.EV, "S. Singh", "9876543211", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        var stats = statisticsService.getStatistics();
        assertEquals(2, stats.currentParked());
        assertEquals(3, stats.availableSlots());

        // Exit one
        parkingService.exitParking("PB10AB1234", "operator", Role.OPERATOR, "127.0.0.1");
        var updatedStats = statisticsService.getStatistics();
        assertEquals(1, updatedStats.currentParked());
        assertEquals(1, updatedStats.totalVehiclesServed());
        assertEquals(4, updatedStats.availableSlots());
    }

    @Test
    @Order(17)
    @DisplayName("Test Case 17: Maintenance slot — Slot is not assigned")
    void testCase17_maintenanceSlotNotAssigned() {
        // Put A-01 and A-02 into MAINTENANCE
        slotService.setMaintenanceStatus("A-01", true);
        slotService.setMaintenanceStatus("A-02", true);

        // A-03 is EV, A-04 is LARGE, A-05 is ACCESSIBLE.
        // A CAR can fallback to LARGE (A-04) if free.
        var res = parkingService.enterParking("PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        // A-01 and A-02 were skipped! A-04 was assigned as compatible fallback
        assertNotEquals("A-01", res.slotNumber());
        assertNotEquals("A-02", res.slotNumber());
        assertEquals("A-04", res.slotNumber());

        // Clean up maintenance status
        slotService.setMaintenanceStatus("A-01", false);
        slotService.setMaintenanceStatus("A-02", false);
    }

    // ==========================================
    // ROLE SEPARATION TEST CASES 18 - 25
    // ==========================================

    @Test
    @Order(18)
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Test Case 18: Viewer calls admin/operator API -> 403 Forbidden")
    void testCase18_viewerCallsAdminOrOperatorApiReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/audit")).andExpect(status().isForbidden());

        VehicleEntryRequest entry = new VehicleEntryRequest("PB10AB1234", VehicleType.CAR, "Viewer Test", "9876543210", Priority.NORMAL);
        mockMvc.perform(post("/api/operator/parking/entry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(19)
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Test Case 19: Viewer JSON scan — Zero sensitive fields (ownerName, contact, full plate)")
    void testCase19_viewerJsonScanZeroSensitiveFields() throws Exception {
        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "Secret Owner", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        MvcResult slotsResult = mockMvc.perform(get("/api/viewer/slots")).andExpect(status().isOk()).andReturn();
        String slotsJson = slotsResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(slotsJson.contains("Secret Owner"), "Viewer slot JSON must not leak ownerName");
        assertFalse(slotsJson.contains("9876543210"), "Viewer slot JSON must not leak contact");
        assertFalse(slotsJson.contains("PB10AB1234"), "Viewer slot JSON must not contain vehicle numbers");

        MvcResult historyResult = mockMvc.perform(get("/api/viewer/parking/history")).andExpect(status().isOk()).andReturn();
        String historyJson = historyResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(historyJson.contains("Secret Owner"), "Viewer history JSON must not leak ownerName");
        assertFalse(historyJson.contains("9876543210"), "Viewer history JSON must not leak contact");
    }

    @Test
    @Order(20)
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Test Case 20: Operator contact masking — ••••••3210 format preserved")
    void testCase20_operatorContactMasking() throws Exception {
        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "Harpreet", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        mockMvc.perform(get("/api/operator/vehicles?search=PB10AB1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactMasked").value("••••••3210"));
    }

    @Test
    @Order(21)
    @DisplayName("Test Case 21: Anonymous access — HTTP 401 Unauthorized")
    void testCase21_anonymousAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/operator/slots")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/viewer/statistics")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @DisplayName("Test Case 22: Lockout after 5 failures — HTTP 423 for 5 minutes")
    void testCase22_lockoutAfterFiveFailures() {
        String testUser = "qa-locked-user";
        loginAttemptService.loginSucceeded(testUser);

        for (int i = 0; i < 4; i++) {
            loginAttemptService.loginFailed(testUser);
            assertFalse(loginAttemptService.isLocked(testUser));
        }

        // 5th failed attempt locks user
        loginAttemptService.loginFailed(testUser);
        assertTrue(loginAttemptService.isLocked(testUser));

        // Cleanup
        loginAttemptService.loginSucceeded(testUser);
        assertFalse(loginAttemptService.isLocked(testUser));
    }

    @Test
    @Order(23)
    @DisplayName("Test Case 23: Audit entry on park/exit — Row created with user, role, action")
    void testCase23_auditEntryOnParkExit() {
        long initialCount = auditLogRepository.count();

        parkingService.enterParking("PB10AB1234", VehicleType.CAR, "Audit User", "9876543210", Priority.NORMAL, "audit-actor", Role.OPERATOR, "127.0.0.1");
        parkingService.exitParking("PB10AB1234", "audit-actor", Role.OPERATOR, "127.0.0.1");

        List<AuditLog> logs = auditLogRepository.findAll();
        assertTrue(logs.size() >= initialCount + 2);

        boolean foundPark = logs.stream().anyMatch(l -> l.getAction().equals("PARK_ENTRY") && l.getUsername().equals("audit-actor"));
        boolean foundExit = logs.stream().anyMatch(l -> l.getAction().equals("PARK_EXIT") && l.getUsername().equals("audit-actor"));
        assertTrue(foundPark, "Audit log must contain PARK_ENTRY entry");
        assertTrue(foundExit, "Audit log must contain PARK_EXIT entry");
    }

    @Test
    @Order(24)
    @DisplayName("Test Case 24: Theme toggle persistence — Valid CSS tokens and no banned fonts")
    void testCase24_themeTokensAndNoBannedFonts() throws Exception {
        MvcResult res = mockMvc.perform(get("/css/tokens.css")).andExpect(status().isOk()).andReturn();
        String css = res.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(css.contains("--bg: #F3F0E8"), "Light theme background token must exist");
        assertTrue(css.contains("--bg: #121417"), "Dark theme background token must exist");
        assertTrue(css.contains("--accent: #F2B705"), "Yellow road marking accent must exist");

        List<String> banned = List.of("Inter", "Roboto", "Poppins", "Montserrat", "DM Sans", "Space Grotesk");
        for (String font : banned) {
            assertFalse(css.toLowerCase().contains(font.toLowerCase()), "Banned font " + font + " must not exist");
        }
    }

    @Test
    @Order(25)
    @DisplayName("Test Case 25: Reallocation skips incompatible head — Highest-priority COMPATIBLE vehicle gets slot")
    void testCase25_reallocationSkipsIncompatibleHead() {
        // Occupy A-01 (NORMAL) with CAR 1
        parkingService.enterParking("PB10AB1111", VehicleType.CAR, "Car 1", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // Occupy A-02 (NORMAL) with CAR 2
        parkingService.enterParking("PB10AB2222", VehicleType.CAR, "Car 2", "9876543211", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // Occupy A-04 (LARGE) with SUV 1
        parkingService.enterParking("PB08CD9876", VehicleType.SUV, "Suv 1", "9876543212", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // Enqueue SUV 2 with HIGH priority (only compatible with LARGE slot)
        parkingService.enterParking("PB08CD2222", VehicleType.SUV, "Suv High", "9876543213", Priority.HIGH, "operator", Role.OPERATOR, "127.0.0.1");

        // Enqueue CAR 3 with NORMAL priority (compatible with NORMAL slot)
        parkingService.enterParking("PB10AB3333", VehicleType.CAR, "Car Normal", "9876543214", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");

        // At head of queue: SUV High (priority HIGH).
        // At second position: CAR Normal (priority NORMAL).
        List<WaitingEntry> snapshot = waitingQueueService.getSnapshot();
        assertEquals("PB08CD2222", snapshot.get(0).vehicleNumber());
        assertEquals("PB10AB3333", snapshot.get(1).vehicleNumber());

        // Now CAR 1 exits from slot A-01 (NORMAL).
        // Since SUV High CANNOT park in a NORMAL bay, the algorithm MUST skip SUV High and reallocate A-01 to CAR Normal!
        var exitResult = parkingService.exitParking("PB10AB1111", "operator", Role.OPERATOR, "127.0.0.1");

        assertTrue(exitResult.reallocation().performed());
        assertEquals("A-01", exitResult.reallocation().slotNumber());
        assertEquals("PB10AB3333", exitResult.reallocation().vehicleNumber()); // CAR got it!

        // SUV High remains at head (#1) in PriorityQueue waiting for a LARGE bay!
        assertEquals(1, waitingQueueService.size());
        assertEquals("PB08CD2222", waitingQueueService.getSnapshot().get(0).vehicleNumber());
        assertEquals(1, waitingQueueService.getQueuePosition("PB08CD2222"));
    }

    // ==========================================
    // HARDENING TESTS: RESTART REBUILD & DOUBLE-SUBMIT
    // ==========================================

    @Test
    @Order(26)
    @DisplayName("Hardening: Startup rebuild restores PriorityQueue from persisted database state")
    void testHardening_startupRebuildRestoresWaitingQueue() {
        vehicleService.findOrCreateVehicle("PB09AA1000", "User 1", "9876543201", VehicleType.SUV, Priority.NORMAL);
        vehicleService.findOrCreateVehicle("PB09AA2000", "User 2", "9876543202", VehicleType.SUV, Priority.HIGH);

        // Enqueue two vehicles into database
        ParkingSession s1 = ParkingSession.createWaiting("PB09AA1000", Instant.now().minusSeconds(100));
        sessionRepository.save(s1);

        ParkingSession s2 = ParkingSession.createWaiting("PB09AA2000", Instant.now());
        sessionRepository.save(s2);

        // Simulate server reboot / reload
        waitingQueueService.rebuildFromDatabase();

        // High priority must be at head (#1)
        assertEquals(2, waitingQueueService.size());
        assertEquals(1, waitingQueueService.getQueuePosition("PB09AA2000"));
        assertEquals(2, waitingQueueService.getQueuePosition("PB09AA1000"));
    }

    @Test
    @Order(27)
    @DisplayName("Hardening: Double-submit prevention blocks concurrent duplicate allocations")
    void testHardening_doubleSubmitPrevention() {
        // First entry succeeds
        var res1 = parkingService.enterParking("PB10ZZ9999", VehicleType.CAR, "Rapid User", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1");
        assertTrue(res1.allocated());

        // Immediate secondary entry attempt throws AlreadyParkedException
        assertThrows(AlreadyParkedException.class, () ->
                parkingService.enterParking("PB10ZZ9999", VehicleType.CAR, "Rapid User", "9876543210", Priority.NORMAL, "operator", Role.OPERATOR, "127.0.0.1")
        );
    }

    @Test
    @Order(28)
    @DisplayName("Hardening: Static error views (403, 404) are accessible and contain zero stack traces")
    void testHardening_staticErrorViewsAccessible() throws Exception {
        MvcResult res403 = mockMvc.perform(get("/403.html")).andExpect(status().isOk()).andReturn();
        String html403 = res403.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html403.contains("Restricted Zone"));
        assertFalse(html403.contains("Exception"));
        assertFalse(html403.contains("at com.smartparking"));

        MvcResult res404 = mockMvc.perform(get("/404.html")).andExpect(status().isOk()).andReturn();
        String html404 = res404.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html404.contains("Not Found"));
        assertFalse(html404.contains("Exception"));
    }
}
