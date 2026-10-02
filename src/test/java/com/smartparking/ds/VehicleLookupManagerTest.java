package com.smartparking.ds;

import com.smartparking.model.Priority;
import com.smartparking.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VehicleLookupManager HashMap Tests")
class VehicleLookupManagerTest {

    private VehicleLookupManager lookupManager;

    @BeforeEach
    void setUp() {
        lookupManager = new VehicleLookupManager();
    }

    @Test
    @DisplayName("Registers and finds vehicle with case-insensitive / hyphen-normalized key")
    void testRegisterAndFind() {
        VehicleSnapshot vehicle = new VehicleSnapshot(
                1L, "PB10AB1234", VehicleType.CAR, "A. Kumar", "9876543210", Priority.NORMAL, false
        );
        lookupManager.register(vehicle);

        assertTrue(lookupManager.contains("pb10 ab-1234"));
        assertTrue(lookupManager.contains("PB-10-AB-1234"));
        assertFalse(lookupManager.contains("HR26DK9999"));

        var found = lookupManager.find("pb10 ab-1234");
        assertTrue(found.isPresent());
        assertEquals("PB10AB1234", found.get().vehicleNumber());
        assertEquals("A. Kumar", found.get().ownerName());
    }

    @Test
    @DisplayName("Tracks and updates vehicle parked status")
    void testParkedStatusTracking() {
        VehicleSnapshot vehicle = new VehicleSnapshot(
                2L, "PB10XY4567", VehicleType.EV, "S. Singh", "9876543211", Priority.HIGH, false
        );
        lookupManager.register(vehicle);

        assertFalse(lookupManager.isParked("PB10XY4567"));

        lookupManager.setParkedStatus("pb10xy4567", true);
        assertTrue(lookupManager.isParked("PB10XY4567"));

        lookupManager.setParkedStatus("PB10XY4567", false);
        assertFalse(lookupManager.isParked("PB10XY4567"));
    }
}
