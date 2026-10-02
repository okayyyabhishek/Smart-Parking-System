/**
 * SMART PARKING ALLOCATION SYSTEM — VIEWER PORTAL SHELL
 * Manages Viewer read-only sidebar, topbar, theme toggle, and role authorization.
 */

import { requireAuth, logout } from '../core/auth.js';
import { escapeHtml } from '../core/format.js';

export async function setupViewerShell(activeNavId) {
    // VIEWER, OPERATOR, and SUPER_ADMIN are all permitted to view the read-only portal
    const user = await requireAuth(['VIEWER', 'OPERATOR', 'SUPER_ADMIN']);
    if (!user) return null;

    // Render Sidebar
    const sidebarEl = document.getElementById('viewer-sidebar');
    if (sidebarEl) {
        sidebarEl.innerHTML = `
            <div class="sidebar-header">
                <div class="brand-badge" style="background-color: var(--surface-2); color: var(--text-muted); border: 1px solid var(--border); font-weight: 800;">READ ONLY</div>
                <h2 class="brand-title">Smart Parking</h2>
                <p class="brand-subtitle">Public Viewer Terminal</p>
            </div>

            <nav class="sidebar-nav" aria-label="Public Viewer Navigation">
                <a href="/viewer/index.html" class="nav-link ${activeNavId === 'dashboard' ? 'active' : ''}" id="nav-dashboard">
                    <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                    <span>Overview</span>
                </a>
                <a href="/viewer/slots.html" class="nav-link ${activeNavId === 'slots' ? 'active' : ''}" id="nav-slots">
                    <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.5 2.8C2.1 11.2 2 11.6 2 12v4c0 .6.4 1 1 1h2"></path><circle cx="7" cy="17" r="2"></circle><path d="M9 17h6"></path><circle cx="17" cy="17" r="2"></circle></svg>
                    <span>Bay Availability</span>
                </a>
                <a href="/viewer/waiting.html" class="nav-link ${activeNavId === 'waiting' ? 'active' : ''}" id="nav-waiting">
                    <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="12 6 12 12 14 14"></polyline><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle></svg>
                    <span>Waiting Queue</span>
                </a>
                <a href="/viewer/history.html" class="nav-link ${activeNavId === 'history' ? 'active' : ''}" id="nav-history">
                    <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                    <span>Public History</span>
                </a>
                <a href="/viewer/stats.html" class="nav-link ${activeNavId === 'stats' ? 'active' : ''}" id="nav-stats">
                    <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                    <span>Analytics</span>
                </a>
            </nav>

            <div class="sidebar-footer">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <div style="font-weight:700; font-size:var(--text-sm);" id="user-display-name">${escapeHtml(user.username)}</div>
                        <div class="role-badge viewer" style="font-size:0.65rem; padding:1px 5px;">${escapeHtml(user.role)}</div>
                    </div>
                    <button type="button" id="logout-btn" class="btn-icon" aria-label="Sign out" title="Sign out">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                    </button>
                </div>
            </div>
        `;
    }

    // Render Topbar
    const topbarEl = document.getElementById('viewer-topbar');
    if (topbarEl) {
        topbarEl.innerHTML = `
            <div class="portal-stripe viewer"></div>
            <div class="topbar-content">
                <div class="topbar-left">
                    <span class="role-badge viewer">READ ONLY</span>
                    <span class="dsa-badge">Public Telemetry · Zero PII Leakage</span>
                </div>
                <div class="topbar-right">
                    <button type="button" id="theme-toggle" class="btn-icon" aria-label="Toggle daylight or night theme" aria-pressed="false" title="Switch daylight/night mode">
                        <span class="theme-icon">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"></circle></svg>
                        </span>
                    </button>
                </div>
            </div>
        `;
    }

    // Attach logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            await logout();
        });
    }

    return user;
}
