package com.smartparking.ds;

import com.smartparking.util.VehicleNumberUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages in-memory lookup of registered vehicles by registration number.
 *
 * DSA: {@link java.util.HashMap} keyed by normalized vehicle registration number.
 * Why: High-performance constant-time checks for duplicate registrations and active parking status.
 * Big-O Complexity:
 *   - Lookup (get): O(1) average, O(n) worst case on hash collisions
 *   - Insertion (put): O(1) average, O(n) worst case
 *   - Duplicate / contains check: O(1) average
 *   - Removal: O(1) average
 * UI Location:
 *   - Vehicle Entry form (prevents duplicate and already-parked entries)
 *   - Vehicle search bar on Operator/Admin portals
 */
public class VehicleLookupManager {

    private final Map<String, VehicleSnapshot> lookupMap = new HashMap<>();

    /**
     * Stores or updates a vehicle in the hash map using its normalized registration number.
     *
     * @param vehicle vehicle snapshot to register
     */
    public void register(VehicleSnapshot vehicle) {
        if (vehicle == null || vehicle.vehicleNumber() == null) {
            return;
        }
        String key = VehicleNumberUtil.normalize(vehicle.vehicleNumber());
        lookupMap.put(key, vehicle);
    }

    /**
     * Looks up a vehicle by registration number.
     *
     * @param vehicleNumber raw or normalized vehicle number
     * @return Optional containing the VehicleSnapshot if present
     */
    public Optional<VehicleSnapshot> find(String vehicleNumber) {
        if (vehicleNumber == null) {
            return Optional.empty();
        }
        String key = VehicleNumberUtil.normalize(vehicleNumber);
        return Optional.ofNullable(lookupMap.get(key));
    }

    /**
     * Checks if a vehicle number is already registered.
     *
     * @param vehicleNumber raw or normalized vehicle number
     * @return true if registered, false otherwise
     */
    public boolean contains(String vehicleNumber) {
        if (vehicleNumber == null) {
            return false;
        }
        return lookupMap.containsKey(VehicleNumberUtil.normalize(vehicleNumber));
    }

    /**
     * Checks if a vehicle is currently marked as parked.
     *
     * @param vehicleNumber raw or normalized vehicle number
     * @return true if currently parked, false otherwise
     */
    public boolean isParked(String vehicleNumber) {
        return find(vehicleNumber)
                .map(VehicleSnapshot::isParked)
                .orElse(false);
    }

    /**
     * Updates the parked status for a vehicle.
     *
     * @param vehicleNumber raw or normalized vehicle number
     * @param parked true if parked, false if exited
     */
    public void setParkedStatus(String vehicleNumber, boolean parked) {
        find(vehicleNumber).ifPresent(v -> {
            VehicleSnapshot updated = new VehicleSnapshot(
                    v.id(),
                    v.vehicleNumber(),
                    v.type(),
                    v.ownerName(),
                    v.contact(),
                    v.priority(),
                    parked
            );
            register(updated);
        });
    }

    /**
     * Removes a vehicle from the lookup map.
     *
     * @param vehicleNumber vehicle number to remove
     */
    public void remove(String vehicleNumber) {
        if (vehicleNumber != null) {
            lookupMap.remove(VehicleNumberUtil.normalize(vehicleNumber));
        }
    }

    /**
     * Clears all entries from the lookup map.
     */
    public void clear() {
        lookupMap.clear();
    }

    /**
     * Returns current number of indexed vehicles.
     *
     * @return count of vehicles
     */
    public int size() {
        return lookupMap.size();
    }
}
