/**
 * SMART PARKING ALLOCATION SYSTEM — ACCESSIBLE DATA TABLE HELPER
 * Manages sticky headers, client-side sorting (aria-sort), zebra striping, and empty states.
 */

import { escapeHtml } from './format.js';

export class DataTable {
    /**
     * @param {Object} options
     * @param {HTMLElement} options.container - Container element to render table in
     * @param {string} options.caption - Table caption (for screen readers and header)
     * @param {Array<{ key: string, label: string, sortable?: boolean, render?: (val, row) => string }>} options.columns
     * @param {string} [options.emptyMessage='No records found.']
     * @param {string} [options.dsaAnnotation] - Optional viva caption (e.g. 'Slot Search · Linear Search · O(n)')
     */
    constructor(options) {
        this.container = options.container;
        this.caption = options.caption;
        this.columns = options.columns;
        this.emptyMessage = options.emptyMessage || 'No records found.';
        this.dsaAnnotation = options.dsaAnnotation || '';

        this.data = [];
        this.currentSortKey = null;
        this.currentSortOrder = 'asc'; // 'asc' | 'desc'
    }

    setData(data) {
        this.data = Array.isArray(data) ? [...data] : [];
        if (this.currentSortKey) {
            this.sortData(this.currentSortKey, this.currentSortOrder, false);
        }
        this.render();
    }

    sortData(key, order = 'asc', shouldRender = true) {
        this.currentSortKey = key;
        this.currentSortOrder = order;

        const col = this.columns.find(c => c.key === key);
        this.data.sort((a, b) => {
            let valA = a[key];
            let valB = b[key];

            if (valA === valB) return 0;
            if (valA === null || valA === undefined) return 1;
            if (valB === null || valB === undefined) return -1;

            if (typeof valA === 'number' && typeof valB === 'number') {
                return order === 'asc' ? valA - valB : valB - valA;
            }

            const strA = String(valA).toLowerCase();
            const strB = String(valB).toLowerCase();
            return order === 'asc' ? strA.localeCompare(strB) : strB.localeCompare(strA);
        });

        if (shouldRender) {
            this.render();
        }
    }

    render() {
        if (!this.container) return;

        let html = `
            <div class="table-container">
                <table class="data-table" role="table">
                    <caption>
                        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
                            <span>${escapeHtml(this.caption)}</span>
                            ${this.dsaAnnotation ? `<span class="dsa-badge">${escapeHtml(this.dsaAnnotation)}</span>` : ''}
                        </div>
                    </caption>
                    <thead>
                        <tr>
        `;

        this.columns.forEach(col => {
            const isSorted = this.currentSortKey === col.key;
            const ariaSort = isSorted ? (this.currentSortOrder === 'asc' ? 'ascending' : 'descending') : (col.sortable ? 'none' : null);
            const sortIcon = isSorted ? (this.currentSortOrder === 'asc' ? ' ↑' : ' ↓') : (col.sortable ? ' ↕' : '');

            html += `
                <th scope="col" 
                    class="${col.sortable ? 'sortable' : ''}" 
                    data-key="${escapeHtml(col.key)}"
                    ${ariaSort ? `aria-sort="${ariaSort}"` : ''}>
                    ${escapeHtml(col.label)}${sortIcon}
                </th>
            `;
        });

        html += `
                        </tr>
                    </thead>
                    <tbody>
        `;

        if (this.data.length === 0) {
            html += `
                <tr>
                    <td colspan="${this.columns.length}" style="text-align: center; padding: var(--space-8); color: var(--text-muted);">
                        ${escapeHtml(this.emptyMessage)}
                    </td>
                </tr>
            `;
        } else {
            this.data.forEach((row, idx) => {
                html += `<tr>`;
                this.columns.forEach(col => {
                    const rawVal = row[col.key];
                    const content = col.render ? col.render(rawVal, row) : escapeHtml(rawVal !== undefined && rawVal !== null ? rawVal : '—');
                    html += `<td>${content}</td>`;
                });
                html += `</tr>`;
            });
        }

        html += `
                    </tbody>
                </table>
            </div>
        `;

        this.container.innerHTML = html;

        // Attach sorting listeners
        const headers = this.container.querySelectorAll('th.sortable');
        headers.forEach(th => {
            th.addEventListener('click', () => {
                const key = th.getAttribute('data-key');
                const nextOrder = (this.currentSortKey === key && this.currentSortOrder === 'asc') ? 'desc' : 'asc';
                this.sortData(key, nextOrder);
            });
        });
    }
}
