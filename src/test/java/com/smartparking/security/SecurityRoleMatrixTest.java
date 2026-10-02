package com.smartparking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.common.VehicleEntryRequest;
import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Role-Based Access Control (RBAC) Matrix Tests")
class SecurityRoleMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. GET /api/admin/users
    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("SUPER_ADMIN can access /api/admin/users -> 200")
    void adminCanAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("OPERATOR cannot access /api/admin/users -> 403")
    void operatorCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("VIEWER cannot access /api/admin/users -> 403")
    void viewerCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user cannot access /api/admin/users -> 401")
    void anonymousCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // 2. POST /api/operator/parking/entry
    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("OPERATOR can submit vehicle entry -> 201/202")
    void operatorCanSubmitEntry() throws Exception {
        VehicleEntryRequest request = new VehicleEntryRequest(
                "DL01AA1111", VehicleType.CAR, "Test Operator Owner", "9876543210", Priority.NORMAL
        );

        mockMvc.perform(post("/api/operator/parking/entry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("VIEWER cannot submit vehicle entry -> 403")
    void viewerCannotSubmitEntry() throws Exception {
        VehicleEntryRequest request = new VehicleEntryRequest(
                "DL01AA2222", VehicleType.CAR, "Test Owner", "9876543210", Priority.NORMAL
        );

        mockMvc.perform(post("/api/operator/parking/entry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous cannot submit vehicle entry -> 401")
    void anonymousCannotSubmitEntry() throws Exception {
        VehicleEntryRequest request = new VehicleEntryRequest(
                "DL01AA3333", VehicleType.CAR, "Test Owner", "9876543210", Priority.NORMAL
        );

        mockMvc.perform(post("/api/operator/parking/entry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // 3. GET /api/viewer/slots
    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("VIEWER can access /api/viewer/slots -> 200")
    void viewerCanAccessViewerSlots() throws Exception {
        mockMvc.perform(get("/api/viewer/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("OPERATOR can access /api/viewer/slots -> 200")
    void operatorCanAccessViewerSlots() throws Exception {
        mockMvc.perform(get("/api/viewer/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("SUPER_ADMIN can access /api/viewer/slots -> 200")
    void adminCanAccessViewerSlots() throws Exception {
        mockMvc.perform(get("/api/viewer/slots"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous cannot access /api/viewer/slots -> 401")
    void anonymousCannotAccessViewerSlots() throws Exception {
        mockMvc.perform(get("/api/viewer/slots"))
                .andExpect(status().isUnauthorized());
    }

    // 4. GET /api/admin/audit
    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("SUPER_ADMIN can access /api/admin/audit -> 200")
    void adminCanAccessAudit() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("OPERATOR cannot access /api/admin/audit -> 403")
    void operatorCannotAccessAudit() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isForbidden());
    }
}
