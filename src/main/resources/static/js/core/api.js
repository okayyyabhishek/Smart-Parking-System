/**
 * SMART PARKING ALLOCATION SYSTEM — API CLIENT
 * Handles CSRF tokens (X-XSRF-TOKEN from XSRF-TOKEN cookie),
 * session expiration (401 redirection), and structured ApiError parsing.
 */

export class ApiException extends Error {
    constructor(status, code, message, fieldErrors = null) {
        super(message || 'An error occurred');
        this.name = 'ApiException';
        this.status = status;
        this.code = code || 'API_ERROR';
        this.fieldErrors = fieldErrors || {};
    }
}

/**
 * Extracts CSRF token from document.cookie
 */
export function getCsrfToken() {
    try {
        const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
        return match ? decodeURIComponent(match[1]) : '';
    } catch (e) {
        return '';
    }
}

/**
 * Robust fetch wrapper with automatic CSRF, 401 handling, and ApiError parsing.
 */
export async function apiRequest(url, options = {}) {
    const config = {
        headers: {
            'Accept': 'application/json',
            ...(options.headers || {})
        },
        credentials: 'same-origin',
        ...options
    };

    const method = (config.method || 'GET').toUpperCase();

    // Attach CSRF header for mutating HTTP requests
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        let csrf = getCsrfToken();
        if (!csrf && !url.includes('/api/auth/login') && !url.includes('/api/auth/csrf')) {
            try {
                await fetch('/api/auth/csrf', { credentials: 'same-origin' });
                csrf = getCsrfToken();
            } catch (ignored) {}
        }
        if (csrf) {
            config.headers['X-XSRF-TOKEN'] = csrf;
        }
    }

    if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json';
        config.body = JSON.stringify(config.body);
    }

    let response;
    try {
        response = await fetch(url, config);
    } catch (networkError) {
        throw new ApiException(0, 'NETWORK_ERROR', 'Unable to reach the server. Please check your connection.');
    }

    // Handle 403 Forbidden due to CSRF token expiration/desync — retry once with freshly minted token
    if (response.status === 403 && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method) && !options._csrfRetried && !url.includes('/api/auth/login')) {
        try {
            await fetch('/api/auth/csrf', { credentials: 'same-origin' });
            const freshCsrf = getCsrfToken();
            if (freshCsrf) {
                return await apiRequest(url, { ...options, _csrfRetried: true });
            }
        } catch (ignored) {}
    }

    // Handle 401 Unauthorized (session expired or unauthenticated)
    if (response.status === 401) {
        const isLoginPage = window.location.pathname.endsWith('login.html');
        if (!isLoginPage && !url.includes('/api/auth/login')) {
            const redirectUrl = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.href = `/login.html?redirect=${redirectUrl}&expired=true`;
            return;
        }
    }

    // Handle 204 No Content
    if (response.status === 204) {
        return null;
    }

    let data;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
        try {
            data = await response.json();
        } catch (e) {
            data = null;
        }
    } else {
        try {
            data = await response.text();
        } catch (e) {
            data = null;
        }
    }

    if (!response.ok) {
        if (data && typeof data === 'object') {
            throw new ApiException(
                response.status,
                data.code || `HTTP_${response.status}`,
                data.message || response.statusText,
                data.fieldErrors
            );
        }
        throw new ApiException(response.status, `HTTP_${response.status}`, response.statusText || 'Request failed');
    }

    return data;
}

export const api = {
    get: (url, options = {}) => apiRequest(url, { ...options, method: 'GET' }),
    post: (url, body = null, options = {}) => apiRequest(url, { ...options, method: 'POST', body }),
    put: (url, body = null, options = {}) => apiRequest(url, { ...options, method: 'PUT', body }),
    delete: (url, options = {}) => apiRequest(url, { ...options, method: 'DELETE' }),
    request: apiRequest
};
