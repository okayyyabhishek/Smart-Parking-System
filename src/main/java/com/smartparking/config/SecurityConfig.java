package com.smartparking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.common.ApiError;
import com.smartparking.security.AppUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security configuration enforcing role-separated access rules, CSRF protection,
 * secure headers, and JSON error responses.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public SecurityConfig(AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF Protection with cookie repository (accessible by client JavaScript for X-XSRF-TOKEN)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/login", "/h2-console/**")
                )
                // Role-based URL access rules per Architecture §5.2
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints and static assets
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/403.html",
                                "/404.html",
                                "/error",
                                "/error/**",
                                "/css/**",
                                "/js/**",
                                "/fonts/**",
                                "/api/auth/login",
                                "/h2-console/**"
                        ).permitAll()

                        // Role-separated APIs
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/operator/**").hasAnyRole("SUPER_ADMIN", "OPERATOR")
                        .requestMatchers("/api/viewer/**").hasAnyRole("SUPER_ADMIN", "OPERATOR", "VIEWER")

                        // Static portal page protections
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/operator/**").hasAnyRole("SUPER_ADMIN", "OPERATOR")
                        .requestMatchers("/viewer/**").hasAnyRole("SUPER_ADMIN", "OPERATOR", "VIEWER")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                // Session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                // Custom JSON error responses for unauthenticated and access denied
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            ApiError error = ApiError.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "Please sign in.");
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            ApiError error = ApiError.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "You do not have access to this area.");
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                )
                // Secure HTTP headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()) // Allow same-origin for H2 console
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                );

        return http.build();
    }
}
