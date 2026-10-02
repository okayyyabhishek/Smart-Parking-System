package com.smartparking.config;

import com.smartparking.model.AppUser;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Priority;
import com.smartparking.model.Role;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.AppUserRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds initial demonstration data when the 'dev' profile is active.
 * All generated seed data is explicitly labelled as DEMO.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ParkingSlotRepository slotRepository;
    private final VehicleRepository vehicleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:Admin@Pass1234}")
    private String adminPassword;

    @Value("${app.seed.operator-password:Operator@Pass1234}")
    private String operatorPassword;

    @Value("${app.seed.viewer-password:Viewer@Pass1234}")
    private String viewerPassword;

    public DataSeeder(
            ParkingSlotRepository slotRepository,
            VehicleRepository vehicleRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.slotRepository = slotRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Checking database state for DEMO seed initialization...");

        // 1. Seed demo parking slots if empty
        if (slotRepository.count() == 0) {
            List<ParkingSlot> demoSlots = List.of(
                    new ParkingSlot("A-01", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE),
                    new ParkingSlot("A-02", SlotType.NORMAL, "A", 0, SlotStatus.AVAILABLE),
                    new ParkingSlot("A-03", SlotType.EV, "A", 0, SlotStatus.AVAILABLE),
                    new ParkingSlot("A-04", SlotType.LARGE, "A", 0, SlotStatus.AVAILABLE),
                    new ParkingSlot("A-05", SlotType.ACCESSIBLE, "A", 0, SlotStatus.AVAILABLE)
            );
            slotRepository.saveAll(demoSlots);
            log.info("[DEMO SEED] Created 5 baseline parking slots: A-01 (NORMAL), A-02 (NORMAL), A-03 (EV), A-04 (LARGE), A-05 (ACCESSIBLE)");
        }

        // 2. Seed baseline demo vehicles if empty
        if (vehicleRepository.count() == 0) {
            List<Vehicle> demoVehicles = List.of(
                    new Vehicle("PB10AB1234", "A. Kumar", "9876543210", VehicleType.CAR, Priority.NORMAL),
                    new Vehicle("PB10XY4567", "S. Singh", "9876543211", VehicleType.EV, Priority.NORMAL),
                    new Vehicle("PB08CD9876", "R. Sharma", "9876543212", VehicleType.SUV, Priority.NORMAL)
            );
            vehicleRepository.saveAll(demoVehicles);
            log.info("[DEMO SEED] Created 3 baseline demo vehicles: PB10AB1234 (CAR), PB10XY4567 (EV), PB08CD9876 (SUV)");
        }

        // 3. Seed demo user accounts if empty
        if (userRepository.count() == 0) {
            AppUser admin = new AppUser("admin", passwordEncoder.encode(adminPassword), Role.SUPER_ADMIN, true, true);
            AppUser operator = new AppUser("operator", passwordEncoder.encode(operatorPassword), Role.OPERATOR, true, true);
            AppUser viewer = new AppUser("viewer", passwordEncoder.encode(viewerPassword), Role.VIEWER, true, true);

            userRepository.saveAll(List.of(admin, operator, viewer));
            log.info("[DEMO SEED] Created 3 default user accounts: admin (SUPER_ADMIN), operator (OPERATOR), viewer (VIEWER)");
        }
    }
}
