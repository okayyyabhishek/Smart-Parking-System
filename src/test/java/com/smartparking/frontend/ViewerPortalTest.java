package com.smartparking.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.model.Priority;
import com.smartparking.model.Role;
import com.smartparking.model.VehicleType;
import com.smartparking.service.ParkingService;
import com.smartparking.service.VehicleService;
import com.smartparking.service.WaitingQueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 7: Viewer Portal Verification & Zero-PII Tests")
class ViewerPortalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private VehicleService vehicleService;

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Viewer static portal views are accessible to VIEWER role (HTTP 200)")
    void testViewerViewsAccessibleToViewer() throws Exception {
        mockMvc.perform(get("/viewer/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/slots.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/waiting.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/history.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/stats.html")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Viewer static portal views are accessible to OPERATOR role (HTTP 200)")
    void testViewerViewsAccessibleToOperator() throws Exception {
        mockMvc.perform(get("/viewer/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/slots.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/waiting.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/history.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/stats.html")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Viewer static portal views are accessible to SUPER_ADMIN role (HTTP 200)")
    void testViewerViewsAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/viewer/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/slots.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/waiting.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/history.html")).andExpect(status().isOk());
        mockMvc.perform(get("/viewer/stats.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous users cannot access Viewer static views or APIs (HTTP 401)")
    void testAnonymousCannotAccessViewerEndpoints() throws Exception {
        mockMvc.perform(get("/viewer/index.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/viewer/slots.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/viewer/waiting.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/viewer/history.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/viewer/stats.html")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/viewer/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/viewer/slots")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/viewer/parking/waiting")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/viewer/parking/history")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/viewer/statistics")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Viewer views contain privacy notices, DSA annotations, and telemetry sections")
    void testViewerViewsContainExpectedElements() throws Exception {
        // Index / Dashboard
        MvcResult indexResult = mockMvc.perform(get("/viewer/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = indexResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("Public Facility Terminal"), "viewer index.html must contain Public Facility Terminal title");
        assertTrue(indexHtml.contains("Zero PII Leakage"), "viewer index.html must contain Zero PII Leakage badge");
        assertTrue(indexHtml.contains("Privacy Safeguard Active"), "viewer index.html must contain Privacy Safeguard notice");

        // Slots / Bays
        MvcResult slotsResult = mockMvc.perform(get("/viewer/slots.html"))
                .andExpect(status().isOk())
                .andReturn();
        String slotsHtml = slotsResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(slotsHtml.contains("Facility Bay Availability"), "viewer slots.html must contain Facility Bay Availability title");
        assertTrue(slotsHtml.contains("Linear Search"), "viewer slots.html must contain Linear Search DSA annotation");

        // Waiting Queue
        MvcResult waitingResult = mockMvc.perform(get("/viewer/waiting.html"))
                .andExpect(status().isOk())
                .andReturn();
        String waitingHtml = waitingResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(waitingHtml.contains("Public Waiting Queue"), "viewer waiting.html must contain Public Waiting Queue title");
        assertTrue(waitingHtml.contains("PriorityQueue"), "viewer waiting.html must reference PriorityQueue DSA");

        // History
        MvcResult historyResult = mockMvc.perform(get("/viewer/history.html"))
                .andExpect(status().isOk())
                .andReturn();
        String historyHtml = historyResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(historyHtml.contains("Public Parking History"), "viewer history.html must contain Public Parking History title");
        assertTrue(historyHtml.contains("Bounded Deque"), "viewer history.html must reference Bounded Deque DSA");

        // Statistics
        MvcResult statsResult = mockMvc.perform(get("/viewer/stats.html"))
                .andExpect(status().isOk())
                .andReturn();
        String statsHtml = statsResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(statsHtml.contains("Public Analytics"), "viewer stats.html must contain Public Analytics title");
        assertTrue(statsHtml.contains("100% Computed From DB"), "viewer stats.html must reference 100% Computed From DB");
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Viewer API endpoints strictly enforce Zero PII leakage and plate masking")
    void testViewerApiEnforcesZeroPiiLeakage() throws Exception {
        // 1. Prepare vehicle and enter parking
        String plate = "PB10XY9999";
        String secretOwner = "SecretDriverJames";
        String secretPhone = "+91-9988776655";

        vehicleService.findOrCreateVehicle(
                plate,
                secretOwner,
                secretPhone,
                VehicleType.CAR,
                Priority.NORMAL
        );

        try {
            parkingService.enterParking(
                    plate,
                    VehicleType.CAR,
                    secretOwner,
                    secretPhone,
                    Priority.NORMAL,
                    "operator",
                    Role.OPERATOR,
                    "127.0.0.1"
            );
        } catch (Exception ignored) {
            // Already allocated or handled
        }

        // 2. Enqueue in waiting list to test waiting endpoint
        String waitPlate = "DL01AB5555";
        vehicleService.findOrCreateVehicle(waitPlate, "WaitOwnerCharlie", "+91-1122334455", VehicleType.SUV, Priority.HIGH);
        waitingQueueService.enqueue(waitPlate, VehicleType.SUV, Priority.HIGH, Instant.now());

        // 3. Test GET /api/viewer/slots
        MvcResult slotsResult = mockMvc.perform(get("/api/viewer/slots"))
                .andExpect(status().isOk())
                .andReturn();
        String slotsJson = slotsResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(slotsJson.contains(secretOwner), "slots API response must not leak owner name");
        assertFalse(slotsJson.contains(secretPhone), "slots API response must not leak contact number");
        assertFalse(slotsJson.contains("currentVehicleNumber"), "slots API response must not leak currentVehicleNumber");
        assertFalse(slotsJson.contains("currentOwnerName"), "slots API response must not leak currentOwnerName");

        JsonNode slotsTree = objectMapper.readTree(slotsJson);
        assertTrue(slotsTree.isArray() && slotsTree.size() > 0, "slots response must return an array of bays");
        JsonNode firstSlot = slotsTree.get(0);
        assertTrue(firstSlot.has("slotNumber"), "slot must have slotNumber");
        assertTrue(firstSlot.has("slotType"), "slot must have slotType");
        assertTrue(firstSlot.has("status"), "slot must have status");
        assertNull(firstSlot.get("currentVehicle"), "slot must not contain currentVehicle entity");

        // 4. Test GET /api/viewer/parking/waiting
        MvcResult waitingResult = mockMvc.perform(get("/api/viewer/parking/waiting"))
                .andExpect(status().isOk())
                .andReturn();
        String waitingJson = waitingResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(waitingJson.contains("WaitOwnerCharlie"), "waiting API response must not leak owner name");
        assertFalse(waitingJson.contains("1122334455"), "waiting API response must not leak phone number");
        assertFalse(waitingJson.contains(waitPlate), "waiting API response must not leak raw vehicle license plate");

        JsonNode waitingTree = objectMapper.readTree(waitingJson);
        assertTrue(waitingTree.isArray() && waitingTree.size() > 0, "waiting queue must contain anonymous entries");
        JsonNode firstWait = waitingTree.get(0);
        assertTrue(firstWait.has("position"), "waiting entry must have position");
        assertTrue(firstWait.has("vehicleType"), "waiting entry must have vehicleType");
        assertTrue(firstWait.has("priority"), "waiting entry must have priority");
        assertFalse(firstWait.has("vehicleNumber"), "waiting entry must not expose vehicleNumber");
        assertFalse(firstWait.has("ownerName"), "waiting entry must not expose ownerName");

        // 5. Test GET /api/viewer/parking/history
        MvcResult historyResult = mockMvc.perform(get("/api/viewer/parking/history"))
                .andExpect(status().isOk())
                .andReturn();
        String historyJson = historyResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(historyJson.contains(secretOwner), "history API response must not leak owner name");
        assertFalse(historyJson.contains(secretPhone), "history API response must not leak contact number");
        if (historyJson.contains("PB10")) {
            assertTrue(historyJson.contains("PB10••••99"), "history API response must mask vehicle plate to PB10••••99");
        }

        // 6. Test GET /api/viewer/dashboard
        mockMvc.perform(get("/api/viewer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").isNumber())
                .andExpect(jsonPath("$.availableSlots").isNumber())
                .andExpect(jsonPath("$.occupiedSlots").isNumber());

        // 7. Test GET /api/viewer/statistics
        mockMvc.perform(get("/api/viewer/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").isNumber())
                .andExpect(jsonPath("$.currentParked").isNumber())
                .andExpect(jsonPath("$.waitingVehicles").isNumber());
    }
}
