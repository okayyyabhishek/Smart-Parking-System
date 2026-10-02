package com.smartparking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.common.ApiError;
import com.smartparking.security.AppUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

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
        SpaCsrfTokenRequestHandler requestHandler = new SpaCsrfTokenRequestHandler();

        http
                // CSRF Protection with cookie repository and SPA handler (supports raw X-XSRF-TOKEN header)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/auth/login", "/h2-console/**")
                )
                // Ensure deferred CSRF token is loaded and cookie written to HTTP response
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                // Role-based URL access rules per Architecture §5.2
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints and static assets
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/403.html",
                                "/404.html",
                                "/favicon.svg",
                                "/favicon.ico",
                                "/error",
                                "/error/**",
                                "/css/**",
                                "/js/**",
                                "/fonts/**",
                                "/api/auth/login",
                                "/api/auth/csrf",
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
                            String message = (accessDeniedException instanceof CsrfException)
                                    ? "Security token validation failed. Please refresh and try again."
                                    : "You do not have access to this area.";
                            ApiError error = ApiError.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", message);
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

    /**
     * Resolves raw CSRF tokens sent in X-XSRF-TOKEN by SPAs while maintaining BREACH protection.
     */
    static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, csrfToken);
            }
            return this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /**
     * OncePerRequestFilter that proactively resolves deferred CSRF tokens, ensuring CookieCsrfTokenRepository
     * saves the XSRF-TOKEN cookie on every response.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
