package com.smartparking.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute force protection tracking failed login attempts.
 * Locks the account for 5 minutes after 5 consecutive failed attempts.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final long lockoutSeconds;

    private final ConcurrentHashMap<String, AttemptData> attemptsCache = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.max-login-attempts:5}") int maxAttempts,
            @Value("${app.security.lockout-minutes:5}") int lockoutMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockoutSeconds = lockoutMinutes * 60L;
    }

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(key.toLowerCase());
        }
    }

    public void loginFailed(String key) {
        if (key == null) return;
        String clean = key.toLowerCase();
        attemptsCache.compute(clean, (k, current) -> {
            Instant now = Instant.now();
            if (current == null || current.isExpired(lockoutSeconds, now)) {
                return new AttemptData(1, now);
            }
            return new AttemptData(current.attempts + 1, now);
        });
    }

    public boolean isLocked(String key) {
        if (key == null) return false;
        AttemptData data = attemptsCache.get(key.toLowerCase());
        if (data == null) {
            return false;
        }
        if (data.isExpired(lockoutSeconds, Instant.now())) {
            attemptsCache.remove(key.toLowerCase());
            return false;
        }
        return data.attempts >= maxAttempts;
    }

    public void clear() {
        attemptsCache.clear();
    }

    private record AttemptData(int attempts, Instant lastAttempt) {
        boolean isExpired(long lockoutDurationSeconds, Instant now) {
            return DurationBetween(lastAttempt, now) > lockoutDurationSeconds;
        }

        private static long DurationBetween(Instant start, Instant end) {
            return java.time.Duration.between(start, end).toSeconds();
        }
    }
}
