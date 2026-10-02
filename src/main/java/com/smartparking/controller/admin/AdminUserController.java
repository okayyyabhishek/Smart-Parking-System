package com.smartparking.controller.admin;

import com.smartparking.dto.admin.AdminCreateUserRequest;
import com.smartparking.dto.admin.AdminUserDto;
import com.smartparking.mapper.AdminMapper;
import com.smartparking.model.AppUser;
import com.smartparking.model.Role;
import com.smartparking.service.AuditService;
import com.smartparking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final AdminMapper adminMapper;
    private final AuditService auditService;

    public AdminUserController(UserService userService, AdminMapper adminMapper, AuditService auditService) {
        this.userService = userService;
        this.adminMapper = adminMapper;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        List<AdminUserDto> users = userService.getAllUsers().stream()
                .map(adminMapper::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<AdminUserDto> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        AppUser created = userService.createUser(
                request.username(),
                request.password(),
                request.role(),
                request.mustChangePassword()
        );

        auditService.record(authentication.getName(), Role.SUPER_ADMIN, "USER_CREATE",
                "Created user " + created.getUsername() + " (" + created.getRole() + ")", httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(adminMapper.toDto(created));
    }

    @PostMapping("/{username}/enabled")
    public ResponseEntity<Void> toggleEnabled(
            @PathVariable String username,
            @RequestParam boolean enabled,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        userService.toggleEnabled(username, enabled);
        auditService.record(authentication.getName(), Role.SUPER_ADMIN, "USER_TOGGLE_ENABLED",
                "Set enabled=" + enabled + " for user " + username, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{username}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> body,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String tempPass = body.getOrDefault("password", "Temp@Pass1234");
        userService.resetPassword(username, tempPass);
        auditService.record(authentication.getName(), Role.SUPER_ADMIN, "USER_RESET_PASSWORD",
                "Password reset for " + username, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
