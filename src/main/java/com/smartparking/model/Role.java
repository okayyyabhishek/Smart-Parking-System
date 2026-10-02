package com.smartparking.model;

/**
 * User authorization roles forming a hierarchy:
 * SUPER_ADMIN > OPERATOR > VIEWER
 */
public enum Role {
    SUPER_ADMIN,
    OPERATOR,
    VIEWER
}
