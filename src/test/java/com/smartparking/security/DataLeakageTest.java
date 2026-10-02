package com.smartparking.security;

import com.smartparking.dto.operator.OperatorVehicleDto;
import com.smartparking.dto.operator.OperatorWaitingDto;
import com.smartparking.dto.viewer.ViewerDashboardDto;
import com.smartparking.dto.viewer.ViewerSessionDto;
import com.smartparking.dto.viewer.ViewerSlotDto;
import com.smartparking.dto.viewer.ViewerWaitingDto;
import com.smartparking.model.Priority;
import com.smartparking.model.Role;
import com.smartparking.model.VehicleType;
import com.smartparking.service.ParkingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Data Leakage Prevention Tests (Reflection & JSON Scans)")
class DataLeakageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParkingService parkingService;

    @Test
    @DisplayName("Reflection: Viewer DTO classes compile-time guarantee of field absence")
    void testViewerDtoReflectionFieldExclusion() {
        // ViewerSessionDto must not declare ownerName or contact
        List<String> sessionFields = Arrays.stream(ViewerSessionDto.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertFalse(sessionFields.contains("ownerName"), "ViewerSessionDto must NOT have ownerName field");
        assertFalse(sessionFields.contains("contact"), "ViewerSessionDto must NOT have contact field");
        assertFalse(sessionFields.contains("vehicleNumber"), "ViewerSessionDto must use vehicleNumberMasked");

        // ViewerSlotDto must have no vehicle reference
        List<String> slotFields = Arrays.stream(ViewerSlotDto.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertFalse(slotFields.contains("currentVehicleNumber"), "ViewerSlotDto must NOT have currentVehicleNumber");
        assertFalse(slotFields.contains("currentVehicleOwner"), "ViewerSlotDto must NOT have currentVehicleOwner");
        assertFalse(slotFields.contains("vehicle"), "ViewerSlotDto must NOT have vehicle object");

        // ViewerWaitingDto must have no plate or owner reference
        List<String> waitingFields = Arrays.stream(ViewerWaitingDto.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertFalse(waitingFields.contains("vehicleNumber"), "ViewerWaitingDto must NOT have vehicleNumber");
        assertFalse(waitingFields.contains("ownerName"), "ViewerWaitingDto must NOT have ownerName");
        assertFalse(waitingFields.contains("contact"), "ViewerWaitingDto must NOT have contact");

        // OperatorVehicleDto & OperatorWaitingDto must not have unmasked contact field
        List<String> opVehicleFields = Arrays.stream(OperatorVehicleDto.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertFalse(opVehicleFields.contains("contact"), "OperatorVehicleDto must use contactMasked");

        List<String> opWaitingFields = Arrays.stream(OperatorWaitingDto.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertFalse(opWaitingFields.contains("contact"), "OperatorWaitingDto must use contactMasked");
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("JSON Scan: Viewer API endpoints never leak ownerName, contact, or unmasked plates")
    void testViewerApiJsonScanNoSensitiveData() throws Exception {
        // Park a vehicle with sensitive identifiable owner details
        String plate = "PB10XY4567";
        String sensitiveOwner = "Suhani Confidential Owner";
        String sensitiveContact = "9876543211";

        parkingService.enterParking(
                plate, VehicleType.EV, sensitiveOwner, sensitiveContact, Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );

        // 1. Scan /api/viewer/slots
        MvcResult slotResult = mockMvc.perform(get("/api/viewer/slots"))
                .andExpect(status().isOk())
                .andReturn();
        String slotJson = slotResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(slotJson.contains(sensitiveOwner), "Viewer slots JSON must not leak owner name");
        assertFalse(slotJson.contains(sensitiveContact), "Viewer slots JSON must not leak contact");
        assertFalse(slotJson.contains(plate), "Viewer slots JSON must not leak vehicle plate");
        assertFalse(slotJson.contains("\"ownerName\""), "Viewer slots JSON must not contain ownerName key");
        assertFalse(slotJson.contains("\"contact\""), "Viewer slots JSON must not contain contact key");

        // 2. Scan /api/viewer/parking/history
        MvcResult historyResult = mockMvc.perform(get("/api/viewer/parking/history"))
                .andExpect(status().isOk())
                .andReturn();
        String historyJson = historyResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(historyJson.contains(sensitiveOwner), "Viewer history JSON must not leak owner name");
        assertFalse(historyJson.contains(sensitiveContact), "Viewer history JSON must not leak contact");
        assertFalse(historyJson.contains(plate), "Viewer history JSON must not leak full plate");
        assertFalse(historyJson.contains("\"ownerName\""), "Viewer history JSON must not contain ownerName key");
        assertFalse(historyJson.contains("\"contact\""), "Viewer history JSON must not contain contact key");
        // Plate must be present in masked format
        assertTrue(historyJson.contains("PB10••••67"), "Viewer history JSON must mask plate as PB10••••67");

        // 3. Scan /api/viewer/dashboard
        MvcResult dashResult = mockMvc.perform(get("/api/viewer/dashboard"))
                .andExpect(status().isOk())
                .andReturn();
        String dashJson = dashResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(dashJson.contains(sensitiveOwner), "Viewer dashboard must not leak owner name");
        assertFalse(dashJson.contains(sensitiveContact), "Viewer dashboard must not leak contact");
        assertFalse(dashJson.contains("\"ownerName\""), "Viewer dashboard must not contain ownerName key");
        assertFalse(dashJson.contains("\"contact\""), "Viewer dashboard must not contain contact key");
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Operator API masks contact numbers as ••••••3210")
    void testOperatorContactMasking() throws Exception {
        parkingService.enterParking(
                "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL,
                "operator", Role.OPERATOR, "127.0.0.1"
        );

        mockMvc.perform(get("/api/operator/vehicles?search=PB10AB1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactMasked").value("••••••3210"));
    }
}
