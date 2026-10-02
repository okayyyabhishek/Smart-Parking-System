package com.smartparking.repository;

import com.smartparking.model.AppUser;
import com.smartparking.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRoleAndEnabledTrue(Role role);
}
