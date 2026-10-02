/**
 * SMART PARKING ALLOCATION SYSTEM — TOAST NOTIFICATIONS
 * Positioned bottom-left, auto-dismiss in 5s, accessible role="status".
 */

import { escapeHtml } from './format.js';

let toastContainer = null;

function getOrCreateContainer() {
    if (!toastContainer || !document.body.contains(toastContainer)) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container';
        toastContainer.setAttribute('role', 'status');
        toastContainer.setAttribute('aria-live', 'polite');
        document.body.appendChild(toastContainer);
    }
    return toastContainer;
}

export function showToast(message, type = 'info', title = '') {
    const container = getOrCreateContainer();

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let iconSvg = '';
    if (type === 'success') {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
    } else if (type === 'error') {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
    } else {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
    }

    toast.innerHTML = `
        <div style="flex-shrink:0; margin-top: 2px;">${iconSvg}</div>
        <div style="flex:1;">
            ${title ? `<div style="font-weight:700; margin-bottom:2px;">${escapeHtml(title)}</div>` : ''}
            <div>${escapeHtml(message)}</div>
        </div>
        <button type="button" class="btn-icon" aria-label="Dismiss notification" style="width:24px; height:24px; min-height:24px; border:none; background:transparent;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
    `;

    const closeBtn = toast.querySelector('button');
    closeBtn.addEventListener('click', () => {
        dismissToast(toast);
    });

    container.appendChild(toast);

    // Auto-dismiss after 5 seconds
    const timer = setTimeout(() => {
        dismissToast(toast);
    }, 5000);

    toast.dataset.timer = timer;
    return toast;
}

function dismissToast(toast) {
    if (!toast) return;
    if (toast.dataset.timer) {
        clearTimeout(Number(toast.dataset.timer));
    }
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 150ms ease-out';
    setTimeout(() => {
        if (toast.parentElement) {
            toast.parentElement.removeChild(toast);
        }
    }, 150);
}

export const toast = {
    success: (msg, title = '') => showToast(msg, 'success', title),
    error: (msg, title = '') => showToast(msg, 'error', title),
    info: (msg, title = '') => showToast(msg, 'info', title),
    warning: (msg, title = '') => showToast(msg, 'warning', title)
};
