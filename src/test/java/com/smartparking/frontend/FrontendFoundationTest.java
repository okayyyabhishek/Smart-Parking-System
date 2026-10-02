package com.smartparking.frontend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 4: Frontend Foundation & Design System Verification")
class FrontendFoundationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public static assets are served without authentication (HTTP 200)")
    void testPublicStaticAssetsAccessible() throws Exception {
        mockMvc.perform(get("/login.html")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/css/tokens.css")).andExpect(status().isOk());
        mockMvc.perform(get("/css/base.css")).andExpect(status().isOk());
        mockMvc.perform(get("/css/components.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/core/api.js")).andExpect(status().isOk());
        mockMvc.perform(get("/js/core/theme.js")).andExpect(status().isOk());
        mockMvc.perform(get("/js/core/auth.js")).andExpect(status().isOk());
        mockMvc.perform(get("/js/core/toast.js")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSS Design System enforces Highway-sign typography and strictly bans AI default fonts")
    void testTypographyRulesAndBannedFonts() throws Exception {
        MvcResult tokensResult = mockMvc.perform(get("/css/tokens.css"))
                .andExpect(status().isOk())
                .andReturn();
        String tokensCss = tokensResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Required non-AI fonts present
        assertTrue(tokensCss.contains("Overpass"), "tokens.css must contain Overpass font");
        assertTrue(tokensCss.contains("Source Sans 3"), "tokens.css must contain Source Sans 3 font");
        assertTrue(tokensCss.contains("Overpass Mono"), "tokens.css must contain Overpass Mono font");

        // Banned fonts must be ABSENT
        List<String> bannedFonts = List.of(
                "Inter", "Roboto", "Poppins", "Montserrat", "DM Sans",
                "Space Grotesk", "Plus Jakarta", "Outfit", "Sora",
                "Manrope", "Geist", "Lexend", "Urbanist"
        );

        for (String banned : bannedFonts) {
            assertFalse(
                    tokensCss.toLowerCase().contains(banned.toLowerCase()),
                    "Banned font '" + banned + "' must NOT be present in CSS tokens"
            );
        }
    }

    @Test
    @DisplayName("Theme tokens cover both Daylight Lot (light) and Night Garage (dark)")
    void testLightAndDarkThemeTokens() throws Exception {
        MvcResult tokensResult = mockMvc.perform(get("/css/tokens.css"))
                .andExpect(status().isOk())
                .andReturn();
        String tokensCss = tokensResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Core variables
        assertTrue(tokensCss.contains("--bg: #F3F0E8"), "Light theme background #F3F0E8 must exist");
        assertTrue(tokensCss.contains("--bg: #121417"), "Dark theme background #121417 must exist");
        assertTrue(tokensCss.contains("--accent: #F2B705"), "Road marking yellow accent #F2B705 must exist");
        assertTrue(tokensCss.contains("--st-available"), "Available status token must exist");
        assertTrue(tokensCss.contains("--st-occupied"), "Occupied status token must exist");
        assertTrue(tokensCss.contains("--bay-border-color"), "Painted bay lines border token must exist");
    }

    @Test
    @DisplayName("Login page contains viva demo credentials, accessible form, and theme toggle")
    void testLoginPageStructure() throws Exception {
        MvcResult loginResult = mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andReturn();
        String loginHtml = loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(loginHtml.contains("data-user=\"admin\""), "Login page must have admin demo quick-fill chip");
        assertTrue(loginHtml.contains("data-user=\"operator\""), "Login page must have operator demo quick-fill chip");
        assertTrue(loginHtml.contains("data-user=\"viewer\""), "Login page must have viewer demo quick-fill chip");
        assertTrue(loginHtml.contains("id=\"theme-toggle\""), "Login page must have theme toggle button");
        assertTrue(loginHtml.contains("id=\"toggle-password-btn\""), "Login page must have password visibility toggle");
        assertTrue(loginHtml.contains("type=\"module\""), "Scripts must load as ES modules");
    }
}
