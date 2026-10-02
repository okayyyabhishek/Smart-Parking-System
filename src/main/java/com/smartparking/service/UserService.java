package com.smartparking.service;

import com.smartparking.exception.DuplicateVehicleException;
import com.smartparking.model.AppUser;
import com.smartparking.model.Role;
import com.smartparking.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<AppUser> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return userRepository.findByUsername(username.trim().toLowerCase());
    }

    @Transactional
    public AppUser createUser(String username, String rawPassword, Role role, boolean mustChangePassword) {
        String cleanUser = username != null ? username.trim().toLowerCase() : "";
        if (userRepository.existsByUsername(cleanUser)) {
            throw new DuplicateVehicleException("Username already exists: " + cleanUser);
        }

        String encoded = passwordEncoder.encode(rawPassword);
        AppUser user = new AppUser(cleanUser, encoded, role, true, mustChangePassword);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        AppUser user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public void toggleEnabled(String username, boolean enabled) {
        AppUser user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Prevent disabling the last active SUPER_ADMIN
        if (!enabled && user.getRole() == Role.SUPER_ADMIN) {
            long adminCount = userRepository.countByRoleAndEnabledTrue(Role.SUPER_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot disable the last active SUPER_ADMIN account.");
            }
        }

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String username, String tempPassword) {
        AppUser user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }
}
