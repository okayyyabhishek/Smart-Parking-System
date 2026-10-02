package com.smartparking.controller.auth;

import com.smartparking.dto.common.AuthMeResponse;
import com.smartparking.dto.common.ChangePasswordRequest;
import com.smartparking.dto.common.LoginRequest;
import com.smartparking.dto.common.LoginResponse;
import com.smartparking.exception.AccountLockedException;
import com.smartparking.model.AppUser;
import com.smartparking.security.LoginAttemptService;
import com.smartparking.service.AuditService;
import com.smartparking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            AuditService auditService,
            LoginAttemptService loginAttemptService
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.auditService = auditService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.username().trim().toLowerCase();

        if (loginAttemptService.isLocked(username)) {
            auditService.record(username, null, "AUTH_LOCKED", "Account locked due to multiple failed attempts", httpRequest.getRemoteAddr());
            throw new AccountLockedException();
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            loginAttemptService.loginSucceeded(username);

            AppUser user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User authenticated but not found in repository"));

            auditService.record(user.getUsername(), user.getRole(), "AUTH_LOGIN", "Login successful", httpRequest.getRemoteAddr());

            return ResponseEntity.ok(new LoginResponse(
                    user.getUsername(),
                    user.getRole(),
                    user.isMustChangePassword(),
                    "Authentication successful."
            ));
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(username);
            auditService.record(username, null, "AUTH_FAILURE", "Failed password attempt", httpRequest.getRemoteAddr());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, Authentication authentication) {
        if (authentication != null) {
            auditService.record(authentication.getName(), null, "AUTH_LOGOUT", "User signed out", httpRequest.getRemoteAddr());
        }
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        AppUser user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return ResponseEntity.ok(new AuthMeResponse(
                user.getUsername(),
                user.getRole(),
                user.isMustChangePassword()
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String username = authentication.getName();
        userService.changePassword(username, request.newPassword());
        auditService.record(username, null, "AUTH_PASSWORD_CHANGE", "Password updated", httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
