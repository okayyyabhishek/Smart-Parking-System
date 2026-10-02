/**
 * SMART PARKING ALLOCATION SYSTEM — AUTH & ROLE GUARD
 * Validates active session via /api/auth/me, verifies role hierarchy,
 * and redirects unauthorized attempts.
 */

import { api, ApiException } from './api.js';

let currentUserCache = null;

export function getPortalHome(role) {
    switch (role) {
        case 'SUPER_ADMIN':
            return '/admin/index.html';
        case 'OPERATOR':
            return '/operator/index.html';
        case 'VIEWER':
            return '/viewer/index.html';
        default:
            return '/login.html';
    }
}

export async function getCurrentUser(forceRefresh = false) {
    if (currentUserCache && !forceRefresh) {
        return currentUserCache;
    }
    try {
        const user = await api.get('/api/auth/me');
        currentUserCache = user;
        return user;
    } catch (err) {
        currentUserCache = null;
        return null;
    }
}

/**
 * Route protection guard for portal pages.
 * @param {string[]} allowedRoles - List of allowed roles, e.g. ['SUPER_ADMIN', 'OPERATOR']
 */
export async function requireAuth(allowedRoles = []) {
    const user = await getCurrentUser();
    if (!user) {
        const currentPath = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/login.html?redirect=${currentPath}`;
        return null;
    }

    if (allowedRoles.length > 0) {
        // SUPER_ADMIN has global access across all portals
        const hasAccess = user.role === 'SUPER_ADMIN' || allowedRoles.includes(user.role);
        if (!hasAccess) {
            // Redirect to their own permitted home
            window.location.href = getPortalHome(user.role);
            return null;
        }
    }

    // Populate user indicators in UI if present
    const nameEl = document.getElementById('user-display-name');
    if (nameEl) nameEl.textContent = user.username;

    const roleEl = document.getElementById('user-display-role');
    if (roleEl) roleEl.textContent = user.role;

    // Attach logout handler if button exists
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            await logout();
        });
    }

    return user;
}

export async function login(username, password) {
    const res = await api.post('/api/auth/login', { username, password });
    currentUserCache = {
        username: res.username,
        role: res.role,
        mustChangePassword: res.mustChangePassword
    };
    return res;
}

export async function logout() {
    try {
        await api.post('/api/auth/logout');
    } catch (e) {
        // Ignore logout errors
    } finally {
        currentUserCache = null;
        window.location.href = '/login.html';
    }
}
