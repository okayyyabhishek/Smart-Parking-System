package com.smartparking.ds;

import com.smartparking.util.VehicleNumberUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VehicleNumberUtil Normalization and Validation Tests")
class VehicleNumberUtilTest {

    @Test
    @DisplayName("Normalizes lower-case, spaced, and hyphenated vehicle numbers")
    void testNormalization() {
        assertEquals("PB10AB1234", VehicleNumberUtil.normalize("pb10 ab-1234"));
        assertEquals("PB10AB1234", VehicleNumberUtil.normalize("  PB-10-AB-1234  "));
        assertEquals("DL01C1111", VehicleNumberUtil.normalize("dl 01.c 1111"));
        assertEquals("", VehicleNumberUtil.normalize(null));
        assertEquals("", VehicleNumberUtil.normalize("   "));
    }

    @Test
    @DisplayName("Validates compliant and non-compliant registration numbers")
    void testValidation() {
        assertTrue(VehicleNumberUtil.isValid("PB10AB1234"));
        assertTrue(VehicleNumberUtil.isValid("DL01"));
        assertFalse(VehicleNumberUtil.isValid("AB")); // too short (< 4 chars)
        assertFalse(VehicleNumberUtil.isValid("PB10-AB1234")); // non-normalized hyphen
        assertFalse(VehicleNumberUtil.isValid(null));
    }
}
