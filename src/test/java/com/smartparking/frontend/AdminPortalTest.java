package com.smartparking.frontend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.admin.AdminCreateSlotRequest;
import com.smartparking.dto.admin.AdminCreateUserRequest;
import com.smartparking.model.Role;
import com.smartparking.model.SlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 6: Super Admin Portal Verification Tests")
class AdminPortalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.smartparking.repository.ParkingSlotRepository slotRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Super Admin static portal views are accessible to SUPER_ADMIN role (HTTP 200)")
    void testAdminViewsAccessibleToSuperAdmin() throws Exception {
        mockMvc.perform(get("/admin/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/slots.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/entry.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/waiting.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/history.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/stats.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit.html")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Super Admin static portal views are forbidden to OPERATOR role (HTTP 403)")
    void testAdminViewsForbiddenToOperator() throws Exception {
        mockMvc.perform(get("/admin/index.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/slots.html")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Super Admin static portal views are forbidden to VIEWER role (HTTP 403)")
    void testAdminViewsForbiddenToViewer() throws Exception {
        mockMvc.perform(get("/admin/index.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit.html")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous users cannot access Super Admin static views (HTTP 401)")
    void testAnonymousCannotAccessAdminViews() throws Exception {
        mockMvc.perform(get("/admin/index.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/users.html")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Super Admin views contain required viva elements and DSA annotations")
    void testAdminViewsContainRequiredAnnotations() throws Exception {
        // Users page
        MvcResult usersResult = mockMvc.perform(get("/admin/users.html"))
                .andExpect(status().isOk())
                .andReturn();
        String usersHtml = usersResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(usersHtml.contains("Last Admin Invariant"), "users.html must note the Last Admin Invariant");
        assertTrue(usersHtml.contains("Provision New System User"), "users.html must contain provision modal");

        // Audit page
        MvcResult auditResult = mockMvc.perform(get("/admin/audit.html"))
                .andExpect(status().isOk())
                .andReturn();
        String auditHtml = auditResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(auditHtml.contains("Immutable Audit Log"), "audit.html must contain Immutable Audit Log annotation");
        assertTrue(auditHtml.contains("Export CSV"), "audit.html must contain CSV export button");

        // Slots page
        MvcResult slotsResult = mockMvc.perform(get("/admin/slots.html"))
                .andExpect(status().isOk())
                .andReturn();
        String slotsHtml = slotsResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(slotsHtml.contains("Add New Parking Bay"), "slots.html must contain Add New Parking Bay modal trigger");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Super Admin can provision new Operator and Viewer accounts and toggle status")
    void testAdminCanCreateAndManageUsers() throws Exception {
        // Create an Operator
        AdminCreateUserRequest createOp = new AdminCreateUserRequest(
                "op_test_unit",
                "Password@1234",
                Role.OPERATOR,
                true
        );

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("op_test_unit"))
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.enabled").value(true));

        // Create a Viewer
        AdminCreateUserRequest createViewer = new AdminCreateUserRequest(
                "vw_test_unit",
                "Password@1234",
                Role.VIEWER,
                true
        );

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createViewer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("vw_test_unit"))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        // Disable the operator
        mockMvc.perform(post("/api/admin/users/op_test_unit/enabled")
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().isNoContent());

        // Enable the operator back
        mockMvc.perform(post("/api/admin/users/op_test_unit/enabled")
                        .with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Super Admin cannot disable the last active SUPER_ADMIN account (HTTP 409 Conflict)")
    void testCannotDisableLastActiveSuperAdmin() throws Exception {
        // Attempt to disable 'admin'
        mockMvc.perform(post("/api/admin/users/admin/enabled")
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cannot disable the last active SUPER_ADMIN account."));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Super Admin can create a new parking slot and toggle maintenance mode")
    void testSlotCreationAndMaintenanceToggle() throws Exception {
        AdminCreateSlotRequest createSlot = new AdminCreateSlotRequest(
                "B-88",
                SlotType.LARGE,
                "B",
                1
        );

        mockMvc.perform(post("/api/admin/slots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSlot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotNumber").value("B-88"))
                .andExpect(jsonPath("$.slotType").value("LARGE"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // Put slot into maintenance
        mockMvc.perform(post("/api/admin/slots/B-88/maintenance")
                        .with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotNumber").value("B-88"))
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        // Reactivate slot from maintenance
        mockMvc.perform(post("/api/admin/slots/B-88/maintenance")
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotNumber").value("B-88"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        slotRepository.findBySlotNumber("B-88").ifPresent(slotRepository::delete);
    }
}
