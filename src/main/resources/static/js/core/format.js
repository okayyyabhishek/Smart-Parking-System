/**
 * SMART PARKING ALLOCATION SYSTEM — FORMATTING HELPERS
 * Clean, consistent display formatting for plates, timestamps, and durations.
 */

/**
 * Format Indian license plate numbers
 * e.g., 'PB10AB1234' -> 'PB 10 AB 1234'
 */
export function formatPlate(plate) {
    if (!plate) return '';
    const clean = plate.trim().toUpperCase();
    if (clean.length === 10 && !clean.includes('•')) {
        return `${clean.substring(0, 2)} ${clean.substring(2, 4)} ${clean.substring(4, 6)} ${clean.substring(6)}`;
    }
    return clean;
}

/**
 * Format ISO-8601 timestamp to localized readable string
 */
export function formatDateTime(isoString) {
    if (!isoString) return '—';
    try {
        const date = new Date(isoString);
        if (isNaN(date.getTime())) return isoString;
        return new Intl.DateTimeFormat('en-IN', {
            dateStyle: 'medium',
            timeStyle: 'short',
            hour12: true
        }).format(date);
    } catch (e) {
        return isoString;
    }
}

/**
 * Format elapsed duration in seconds to human readable string (e.g., '4m 32s', '2h 15m')
 */
export function formatDuration(seconds) {
    if (seconds === null || seconds === undefined || isNaN(seconds)) return '—';
    const s = Math.max(0, Math.floor(seconds));
    if (s < 60) {
        return `${s}s`;
    }
    const mins = Math.floor(s / 60);
    const remSecs = s % 60;
    if (mins < 60) {
        return `${mins}m ${remSecs}s`;
    }
    const hours = Math.floor(mins / 60);
    const remMins = mins % 60;
    return `${hours}h ${remMins}m`;
}

/**
 * Safe HTML escaping for dynamic DOM inserts
 */
export function escapeHtml(unsafe) {
    if (unsafe === null || unsafe === undefined) return '';
    return String(unsafe)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
