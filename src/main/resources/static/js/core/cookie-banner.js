/**
 * SMART PARKING — Cookie Consent Banner
 * Drop <script type="module" src="/js/core/cookie-banner.js"></script>
 * into any page that needs it. Stores consent in localStorage.
 * Does NOT track. Banner is purely informational (no analytics here).
 */

const STORAGE_KEY = 'sp_cookie_consent';
const ACCEPTED    = 'accepted';
const DECLINED    = 'declined';

function injectStyles() {
    const style = document.createElement('style');
    style.textContent = `
        #sp-cookie-banner {
            position: fixed;
            bottom: 0; left: 0; right: 0;
            z-index: 9999;
            background: rgba(15, 17, 23, 0.95);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border-top: 1px solid rgba(255,255,255,0.10);
            padding: 16px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 20px;
            flex-wrap: wrap;
            box-shadow: 0 -8px 32px rgba(0,0,0,0.35);
            animation: cookieSlideUp 0.45s cubic-bezier(0.16,1,0.3,1) both;
        }

        @keyframes cookieSlideUp {
            from { transform: translateY(100%); opacity: 0; }
            to   { transform: translateY(0); opacity: 1; }
        }

        #sp-cookie-banner.dismissing {
            animation: cookieSlideDown 0.35s cubic-bezier(0.4,0,1,1) both;
        }

        @keyframes cookieSlideDown {
            from { transform: translateY(0); opacity: 1; }
            to   { transform: translateY(100%); opacity: 0; }
        }

        .sp-cookie-text {
            font-family: system-ui, sans-serif;
            font-size: 0.82rem;
            color: rgba(255,255,255,0.70);
            line-height: 1.5;
            flex: 1;
            min-width: 220px;
        }

        .sp-cookie-text strong {
            color: #F2B705;
            font-weight: 700;
        }

        .sp-cookie-text a {
            color: rgba(255,255,255,0.55);
            text-decoration: underline;
            text-underline-offset: 2px;
        }

        .sp-cookie-text a:hover { color: #F2B705; }

        .sp-cookie-actions {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-shrink: 0;
        }

        .sp-cookie-btn {
            height: 38px;
            padding: 0 18px;
            border-radius: 10px;
            font-family: system-ui, sans-serif;
            font-size: 0.8rem;
            font-weight: 700;
            cursor: pointer;
            border: none;
            transition: transform 0.12s ease, box-shadow 0.12s ease;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .sp-cookie-btn:hover { transform: translateY(-2px); }
        .sp-cookie-btn:active { transform: translateY(0); }

        .sp-cookie-btn-accept {
            background: linear-gradient(135deg, #F2B705, #C98B00);
            color: #17191C;
            box-shadow: 0 4px 14px rgba(242,183,5,0.4);
        }

        .sp-cookie-btn-accept:hover {
            box-shadow: 0 8px 22px rgba(242,183,5,0.55);
        }

        .sp-cookie-btn-decline {
            background: rgba(255,255,255,0.08);
            color: rgba(255,255,255,0.60);
            border: 1px solid rgba(255,255,255,0.15);
        }

        .sp-cookie-btn-decline:hover {
            background: rgba(255,255,255,0.14);
            color: #fff;
        }

        @media (max-width: 520px) {
            #sp-cookie-banner { flex-direction: column; align-items: flex-start; }
            .sp-cookie-actions { width: 100%; }
            .sp-cookie-btn { flex: 1; justify-content: center; }
        }
    `;
    document.head.appendChild(style);
}

function dismiss(banner) {
    banner.classList.add('dismissing');
    banner.addEventListener('animationend', () => banner.remove(), { once: true });
}

function init() {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === ACCEPTED || stored === DECLINED) return; // already decided

    injectStyles();

    const banner = document.createElement('div');
    banner.id = 'sp-cookie-banner';
    banner.setAttribute('role', 'region');
    banner.setAttribute('aria-label', 'Cookie consent');
    banner.innerHTML = `
        <p class="sp-cookie-text">
            <strong>🍪 Cookie Notice</strong> — Smart Parking uses essential session cookies to keep you authenticated.
            No tracking, no analytics, no third-party cookies.
            <a href="/terms.html" target="_blank" rel="noopener">Terms of Use</a>
        </p>
        <div class="sp-cookie-actions">
            <button type="button" class="sp-cookie-btn sp-cookie-btn-decline" id="sp-cookie-decline">
                Decline
            </button>
            <button type="button" class="sp-cookie-btn sp-cookie-btn-accept" id="sp-cookie-accept">
                ✓ Got it
            </button>
        </div>
    `;

    document.body.appendChild(banner);

    document.getElementById('sp-cookie-accept').addEventListener('click', () => {
        localStorage.setItem(STORAGE_KEY, ACCEPTED);
        dismiss(banner);
    });

    document.getElementById('sp-cookie-decline').addEventListener('click', () => {
        localStorage.setItem(STORAGE_KEY, DECLINED);
        dismiss(banner);
    });
}

// Run after DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
