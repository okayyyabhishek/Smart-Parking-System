package com.smartparking.frontend;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 5: Operator Portal Verification Tests")
class OperatorPortalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Operator static portal views are accessible to OPERATOR role (HTTP 200)")
    void testOperatorViewsAccessibleToOperator() throws Exception {
        mockMvc.perform(get("/operator/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/operator/entry.html")).andExpect(status().isOk());
        mockMvc.perform(get("/operator/slots.html")).andExpect(status().isOk());
        mockMvc.perform(get("/operator/waiting.html")).andExpect(status().isOk());
        mockMvc.perform(get("/operator/history.html")).andExpect(status().isOk());
        mockMvc.perform(get("/operator/stats.html")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("Operator static portal views are forbidden to VIEWER role (HTTP 403)")
    void testOperatorViewsForbiddenToViewer() throws Exception {
        mockMvc.perform(get("/operator/index.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/operator/entry.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/operator/slots.html")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous users cannot access Operator static views (HTTP 401)")
    void testAnonymousCannotAccessOperatorViews() throws Exception {
        mockMvc.perform(get("/operator/index.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/operator/entry.html")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Operator views contain viva DSA badges explaining Big-O complexity")
    void testOperatorViewsContainDsaBadges() throws Exception {
        // Entry page DSA badge
        MvcResult entryResult = mockMvc.perform(get("/operator/entry.html"))
                .andExpect(status().isOk())
                .andReturn();
        String entryHtml = entryResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(entryHtml.contains("Rank Comparator"), "entry.html must contain DSA rank comparator annotation");

        // Waiting queue DSA badge
        MvcResult waitingResult = mockMvc.perform(get("/operator/waiting.html"))
                .andExpect(status().isOk())
                .andReturn();
        String waitingHtml = waitingResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(waitingHtml.contains("PriorityQueue · O(log n)"), "waiting.html must contain PriorityQueue O(log n) annotation");

        // Slots bay search DSA badge
        MvcResult slotsResult = mockMvc.perform(get("/operator/slots.html"))
                .andExpect(status().isOk())
                .andReturn();
        String slotsHtml = slotsResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(slotsHtml.contains("Linear Search · O(n)"), "slots.html must contain Linear Search O(n) annotation");
    }
}
