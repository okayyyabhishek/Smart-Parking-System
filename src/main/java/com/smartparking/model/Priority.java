package com.smartparking.model;

/**
 * Priority levels for vehicle admission and waiting queue ordering.
 * Higher level has higher queue precedence: EMERGENCY(3) > HIGH(2) > NORMAL(1).
 * Priority affects only waiting queue ordering, never slot compatibility.
 */
public enum Priority {
    NORMAL(1),
    HIGH(2),
    EMERGENCY(3);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
