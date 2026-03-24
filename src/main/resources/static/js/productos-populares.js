/**
 * ============================================
 *  SIONTRACK v2.1 — Productos Populares
 *  Dashboard widget — fetch + render + charts
 * ============================================
 *
 *  Consume: GET /api/productos/populares?limite={n}&periodo={p}
 *  Response: [{ productoId, nombre, categoria, totalVendido }]
 *
 *  Requiere: Chart.js (CDN)
 */

document.addEventListener('DOMContentLoaded', () => {

    // Chart.js 4.x UMD: registrar todos los componentes
    if (typeof Chart !== 'undefined' && Chart.register) {
        Chart.register(...Chart.registerables || []);
    }

    const widget = new ProductosPopulares('#popular-productos');
    widget.init();
});


class ProductosPopulares {

    /* ----------------------------------------
       Config
       ---------------------------------------- */
    static DEFAULTS = {
        limite:  5,
        periodo: 'mes',
        apiUrl:  '/api/productos/populares'
    };

    static PERIODOS = [
        { key: 'semana',    label: 'Semana' },
        { key: 'mes',       label: 'Mes' },
        { key: 'trimestre', label: 'Trimestre' },
        { key: 'anio',      label: 'Año' },
        { key: 'general',   label: 'Todos' }
    ];

    static CHART_TYPES = [
        { key: 'none',       label: 'Lista',       icon: 'list' },
        { key: 'bar',        label: 'Barras',      icon: 'bar' },
        { key: 'horizontal', label: 'Horizontal',  icon: 'horizontal' },
        { key: 'doughnut',   label: 'Dona',        icon: 'doughnut' },
        { key: 'polar',      label: 'Polar',       icon: 'polar' },
        { key: 'radar',      label: 'Radar',       icon: 'radar' }
    ];

    // Paleta premium SionTrack
    static COLORS = [
        'rgba(212, 175, 55, 0.85)',   // Gold
        'rgba(59, 130, 246, 0.85)',    // Blue
        'rgba(16, 185, 129, 0.85)',    // Emerald
        'rgba(244, 63, 94, 0.85)',     // Rose
        'rgba(168, 85, 247, 0.85)',    // Purple
        'rgba(251, 146, 60, 0.85)',    // Orange
        'rgba(34, 211, 238, 0.85)',    // Cyan
        'rgba(163, 230, 53, 0.85)',    // Lime
        'rgba(232, 121, 249, 0.85)',   // Fuchsia
        'rgba(253, 224, 71, 0.85)'    // Yellow
    ];

    static COLORS_BORDER = [
        'rgba(212, 175, 55, 1)',
        'rgba(59, 130, 246, 1)',
        'rgba(16, 185, 129, 1)',
        'rgba(244, 63, 94, 1)',
        'rgba(168, 85, 247, 1)',
        'rgba(251, 146, 60, 1)',
        'rgba(34, 211, 238, 1)',
        'rgba(163, 230, 53, 1)',
        'rgba(232, 121, 249, 1)',
        'rgba(253, 224, 71, 1)'
    ];


    /* ----------------------------------------
       Constructor
       ---------------------------------------- */
    constructor(selector) {
        this.container = document.querySelector(selector);
        if (!this.container) {
            console.warn('[ProductosPopulares] Contenedor no encontrado:', selector);
            return;
        }
        this.periodo    = ProductosPopulares.DEFAULTS.periodo;
        this.limite     = ProductosPopulares.DEFAULTS.limite;
        this.chartType  = 'none';
        this.data       = [];
        this.chartInstance = null;
    }


    /* ----------------------------------------
       Init
       ---------------------------------------- */
    init() {
        if (!this.container) return;
        this._buildSkeleton();
        this._bindPeriodButtons();
        this._bindChartButtons();
        this.fetchData();
    }


    /* ----------------------------------------
       Skeleton
       ---------------------------------------- */
    _buildSkeleton() {
        this.container.innerHTML = `
        <div class="popular-card">
            <!-- HEADER -->
            <div class="popular-card-header">
                <div class="popular-header-left">
                    <div class="popular-header-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="18" y1="20" x2="18" y2="10"/>
                            <line x1="12" y1="20" x2="12" y2="4"/>
                            <line x1="6"  y1="20" x2="6"  y2="14"/>
                        </svg>
                    </div>
                    <div>
                        <div class="popular-header-title">Productos Populares</div>
                        <div class="popular-header-subtitle">
                            Top ${this.limite} por cantidad vendida
                        </div>
                    </div>
                </div>
                <div class="popular-header-controls">
                    <div class="popular-periods" id="popular-periods">
                        ${ProductosPopulares.PERIODOS.map(p => `
                            <button class="popular-period-btn ${p.key === this.periodo ? 'active' : ''}"
                                    data-periodo="${p.key}">
                                ${p.label}
                            </button>
                        `).join('')}
                    </div>
                </div>
            </div>

            <!-- CHART TYPE SELECTOR -->
            <div class="popular-chart-bar" id="popular-chart-bar">
                <div class="popular-chart-bar-label">Visualización</div>
                <div class="popular-chart-types" id="popular-chart-types">
                    ${ProductosPopulares.CHART_TYPES.map(c => `
                        <button class="popular-chart-btn ${c.key === this.chartType ? 'active' : ''}"
                                data-chart="${c.key}" title="${c.label}">
                            ${this._getChartIcon(c.icon)}
                            <span>${c.label}</span>
                        </button>
                    `).join('')}
                </div>
            </div>

            <!-- BODY -->
            <div class="popular-body" id="popular-body">
                <div class="popular-list" id="popular-list"></div>
                <div class="popular-chart-container" id="popular-chart-container" style="display:none;">
                    <canvas id="popular-chart-canvas"></canvas>
                </div>
            </div>

            <!-- FOOTER -->
            <div class="popular-card-footer">
                <div class="popular-footer-info">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                         stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="16" x2="12" y2="12"/>
                        <line x1="12" y1="8" x2="12.01" y2="8"/>
                    </svg>
                    <span id="popular-footer-text">Basado en servicios realizados</span>
                </div>
                <a href="/web/productos" class="popular-footer-link">
                    Ver todos los productos
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                         stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="5" y1="12" x2="19" y2="12"/>
                        <polyline points="12 5 19 12 12 19"/>
                    </svg>
                </a>
            </div>
        </div>`;

        this.listEl       = this.container.querySelector('#popular-list');
        this.chartWrap     = this.container.querySelector('#popular-chart-container');
        this.chartCanvas   = this.container.querySelector('#popular-chart-canvas');
        this.footerTx      = this.container.querySelector('#popular-footer-text');
    }


    /* ----------------------------------------
       SVG icons for chart types
       ---------------------------------------- */
    _getChartIcon(type) {
        const icons = {
            list: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                     <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/>
                     <line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/>
                     <line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
                   </svg>`,
            bar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
                    <line x1="6" y1="20" x2="6" y2="14"/>
                  </svg>`,
            horizontal: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                           <line x1="4" y1="6" x2="14" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/>
                           <line x1="4" y1="18" x2="10" y2="18"/>
                         </svg>`,
            doughnut: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                         <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
                         <path d="M22 12A10 10 0 0 0 12 2v10z"/>
                       </svg>`,
            polar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/>
                      <circle cx="12" cy="12" r="2"/>
                    </svg>`,
            radar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"/>
                      <polygon points="12 6 17 9.5 17 14.5 12 18 7 14.5 7 9.5 12 6"/>
                    </svg>`
        };
        return icons[type] || icons.bar;
    }


    /* ----------------------------------------
       Event bindings
       ---------------------------------------- */
    _bindPeriodButtons() {
        const wrap = this.container.querySelector('#popular-periods');
        if (!wrap) return;
        wrap.addEventListener('click', (e) => {
            const btn = e.target.closest('.popular-period-btn');
            if (!btn || btn.classList.contains('active')) return;
            wrap.querySelectorAll('.popular-period-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            this.periodo = btn.dataset.periodo;
            this.fetchData();
        });
    }

    _bindChartButtons() {
        const wrap = this.container.querySelector('#popular-chart-types');
        if (!wrap) return;
        wrap.addEventListener('click', (e) => {
            const btn = e.target.closest('.popular-chart-btn');
            if (!btn || btn.classList.contains('active')) return;
            wrap.querySelectorAll('.popular-chart-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            this.chartType = btn.dataset.chart;
            this._updateView();
        });
    }


    /* ----------------------------------------
       Fetch
       ---------------------------------------- */
    async fetchData() {
        this._showLoading();
        try {
            const url = `${ProductosPopulares.DEFAULTS.apiUrl}?limite=${this.limite}&periodo=${this.periodo}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            this.data = await res.json();
            this._updateView();
        } catch (err) {
            console.error('[ProductosPopulares] Error:', err);
            this._showError();
        }
    }


    /* ----------------------------------------
       View switcher
       ---------------------------------------- */
    _updateView() {
        if (!this.data || this.data.length === 0) {
            this._showEmpty();
            return;
        }

        if (this.chartType === 'none') {
            this.listEl.style.display = '';
            this.chartWrap.style.display = 'none';
            this._destroyChart();
            this._renderList();
        } else {
            this.listEl.style.display = 'none';
            this.chartWrap.style.display = '';
            this._renderChart();
        }
        this._updateFooter();
    }


    /* ----------------------------------------
       Render LIST (original)
       ---------------------------------------- */
    _renderList() {
        const maxVendido = this.data[0].totalVendido || 1;

        this.listEl.innerHTML = this.data.map((p, i) => {
            const rank     = i + 1;
            const rankCls  = rank <= 3 ? `top-${rank}` : 'top-other';
            const pct      = Math.round((p.totalVendido / maxVendido) * 100);
            const categoria = p.categoria || 'Sin categoría';

            // Insignia de estrella compacta (igual que en la campana)
            const starTag = rank <= 3
                ? `<span class="popular-star-tag top-${rank}" title="Top ${rank} más vendido">
                       <svg viewBox="0 0 24 24"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                   </span>`
                : '';

            return `
            <div class="popular-item">
                <div class="popular-rank ${rankCls}">${rank}</div>
                <div class="popular-info">
                    <div class="popular-name" title="${this._escape(p.nombre)}">
                        ${this._escape(p.nombre)}
                        ${starTag}
                    </div>
                    <div class="popular-category">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83
                                     0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/>
                            <line x1="7" y1="7" x2="7.01" y2="7"/>
                        </svg>
                        ${this._escape(categoria)}
                    </div>
                </div>
                <div class="popular-bar-wrap">
                    <div class="popular-bar-track">
                        <div class="popular-bar-fill" data-width="${pct}"></div>
                    </div>
                </div>
                <div class="popular-sold">
                    <div class="popular-sold-number">${p.totalVendido.toLocaleString('es-CO')}</div>
                    <div class="popular-sold-label">vendidos</div>
                </div>
            </div>`;
        }).join('');

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                this.listEl.querySelectorAll('.popular-bar-fill').forEach(bar => {
                    bar.style.width = bar.dataset.width + '%';
                });
            });
        });
    }


    /* ----------------------------------------
       Render CHART
       ---------------------------------------- */
    _renderChart() {
        this._destroyChart();

        // Resetear el canvas para evitar problemas de contexto
        const parent = this.chartCanvas.parentNode;
        const oldCanvas = this.chartCanvas;
        const newCanvas = document.createElement('canvas');
        newCanvas.id = 'popular-chart-canvas';
        parent.replaceChild(newCanvas, oldCanvas);
        this.chartCanvas = newCanvas;

        const labels    = this.data.map(p => this._truncLabel(p.nombre, 20));
        const values    = this.data.map(p => p.totalVendido);
        const colors    = ProductosPopulares.COLORS.slice(0, this.data.length);
        const borders   = ProductosPopulares.COLORS_BORDER.slice(0, this.data.length);
        const ctx       = this.chartCanvas.getContext('2d');

        // Detect dark mode
        const isDark = getComputedStyle(document.documentElement)
                       .getPropertyValue('--bg-surface').trim() !== '#ffffff';

        const textColor = isDark ? 'rgba(255,255,255,0.75)' : 'rgba(0,0,0,0.7)';
        const gridColor = isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)';

        // Chart.js global defaults
        Chart.defaults.color = textColor;
        Chart.defaults.font.family = getComputedStyle(document.body).fontFamily || 'system-ui, sans-serif';

        let config;

        switch (this.chartType) {

            case 'bar':
                config = {
                    type: 'bar',
                    data: {
                        labels,
                        datasets: [{
                            label: 'Unidades vendidas',
                            data: values,
                            backgroundColor: colors,
                            borderColor: borders,
                            borderWidth: 1,
                            borderRadius: 6,
                            borderSkipped: false
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        indexAxis: 'x',
                        plugins: {
                            legend: { display: false },
                            tooltip: this._tooltipConfig()
                        },
                        scales: {
                            x: {
                                grid: { display: false },
                                ticks: { font: { size: 11 } }
                            },
                            y: {
                                grid: { color: gridColor },
                                ticks: { precision: 0, font: { size: 11 } },
                                beginAtZero: true
                            }
                        },
                        animation: { duration: 800, easing: 'easeOutQuart' }
                    }
                };
                break;

            case 'horizontal':
                config = {
                    type: 'bar',
                    data: {
                        labels,
                        datasets: [{
                            label: 'Unidades vendidas',
                            data: values,
                            backgroundColor: colors,
                            borderColor: borders,
                            borderWidth: 1,
                            borderRadius: 6,
                            borderSkipped: false
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        indexAxis: 'y',
                        plugins: {
                            legend: { display: false },
                            tooltip: this._tooltipConfig()
                        },
                        scales: {
                            y: {
                                grid: { display: false },
                                ticks: { font: { size: 11 } }
                            },
                            x: {
                                grid: { color: gridColor },
                                ticks: { precision: 0, font: { size: 11 } },
                                beginAtZero: true
                            }
                        },
                        animation: { duration: 800, easing: 'easeOutQuart' }
                    }
                };
                break;

            case 'doughnut':
                config = {
                    type: 'doughnut',
                    data: {
                        labels,
                        datasets: [{
                            data: values,
                            backgroundColor: colors,
                            borderColor: isDark ? 'rgba(30,30,30,1)' : 'rgba(255,255,255,1)',
                            borderWidth: 3,
                            hoverOffset: 8
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '55%',
                        plugins: {
                            legend: this._legendConfig(),
                            tooltip: this._tooltipConfig()
                        },
                        animation: { animateRotate: true, duration: 1000, easing: 'easeOutQuart' }
                    }
                };
                break;

            case 'polar':
                config = {
                    type: 'polarArea',
                    data: {
                        labels,
                        datasets: [{
                            data: values,
                            backgroundColor: colors.map(c => c.replace('0.85', '0.6')),
                            borderColor: borders,
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: this._legendConfig(),
                            tooltip: this._tooltipConfig()
                        },
                        scales: {
                            r: {
                                grid: { color: gridColor },
                                ticks: { display: false, precision: 0 },
                                beginAtZero: true
                            }
                        },
                        animation: { animateRotate: true, duration: 1000, easing: 'easeOutQuart' }
                    }
                };
                break;

            case 'radar':
                config = {
                    type: 'radar',
                    data: {
                        labels,
                        datasets: [{
                            label: 'Unidades vendidas',
                            data: values,
                            backgroundColor: 'rgba(212, 175, 55, 0.15)',
                            borderColor: 'rgba(212, 175, 55, 1)',
                            borderWidth: 2,
                            pointBackgroundColor: borders,
                            pointBorderColor: '#fff',
                            pointBorderWidth: 1,
                            pointRadius: 5,
                            pointHoverRadius: 7
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false },
                            tooltip: this._tooltipConfig()
                        },
                        scales: {
                            r: {
                                grid: { color: gridColor },
                                angleLines: { color: gridColor },
                                ticks: { precision: 0, font: { size: 10 }, backdropColor: 'transparent' },
                                pointLabels: { font: { size: 11 }, color: textColor },
                                beginAtZero: true
                            }
                        },
                        animation: { duration: 800, easing: 'easeOutQuart' }
                    }
                };
                break;

            default:
                return;
        }

        this.chartInstance = new Chart(ctx, config);
    }


    /* ----------------------------------------
       Chart helpers
       ---------------------------------------- */
    _tooltipConfig() {
        return {
            backgroundColor: 'rgba(20,20,20,0.9)',
            titleColor: '#fff',
            bodyColor: 'rgba(255,255,255,0.85)',
            borderColor: 'rgba(212,175,55,0.3)',
            borderWidth: 1,
            cornerRadius: 8,
            padding: 10,
            titleFont: { weight: '600', size: 12 },
            bodyFont: { size: 12 },
            displayColors: true,
            boxWidth: 10,
            boxHeight: 10,
            boxPadding: 4,
            callbacks: {
                label: (ctx) => {
                    const val = ctx.parsed?.y ?? ctx.parsed?.x ?? ctx.raw;
                    return ` ${val.toLocaleString('es-CO')} unidades`;
                }
            }
        };
    }

    _legendConfig() {
        return {
            position: 'bottom',
            labels: {
                padding: 16,
                usePointStyle: true,
                pointStyle: 'rectRounded',
                font: { size: 11 }
            }
        };
    }

    _destroyChart() {
        if (this.chartInstance) {
            this.chartInstance.destroy();
            this.chartInstance = null;
        }
    }


    /* ----------------------------------------
       States
       ---------------------------------------- */
    _showLoading() {
        this.listEl.style.display = '';
        this.chartWrap.style.display = 'none';
        this.listEl.innerHTML = `
        <div class="popular-loading">
            <div class="popular-loading-spinner"></div>
            <div class="popular-loading-text">Cargando productos...</div>
        </div>`;
    }

    _showEmpty() {
        this._destroyChart();
        this.chartWrap.style.display = 'none';
        this.listEl.style.display = '';
        this.listEl.innerHTML = `
        <div class="popular-empty">
            <div class="popular-empty-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/>
                    <line x1="3" y1="6" x2="21" y2="6"/>
                    <path d="M16 10a4 4 0 01-8 0"/>
                </svg>
            </div>
            <div class="popular-empty-title">Sin datos disponibles</div>
            <p class="popular-empty-desc">No se encontraron productos vendidos en este periodo.</p>
        </div>`;
        this.footerTx.textContent = 'Sin resultados para el periodo seleccionado';
    }

    _showError() {
        this._destroyChart();
        this.chartWrap.style.display = 'none';
        this.listEl.style.display = '';
        this.listEl.innerHTML = `
        <div class="popular-error">
            <div class="popular-error-text">No se pudieron cargar los productos populares.</div>
            <button class="popular-retry-btn" id="popular-retry">Reintentar</button>
        </div>`;
        this.listEl.querySelector('#popular-retry')
                   ?.addEventListener('click', () => this.fetchData());
    }


    /* ----------------------------------------
       Helpers
       ---------------------------------------- */
    _updateFooter() {
        const label = ProductosPopulares.PERIODOS
            .find(p => p.key === this.periodo)?.label || this.periodo;
        this.footerTx.textContent = `Periodo: ${label} · ${this.data.length} productos`;
    }

    _truncLabel(str, max) {
        return str.length > max ? str.substring(0, max) + '…' : str;
    }

    _escape(str) {
        return SionUtils.esc(str, '');
    }
}