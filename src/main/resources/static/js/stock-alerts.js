/**
 * SionTrack — Stock Alerts Dashboard + Restock Modal
 * v5 — Final corregido
 * Anima rings, setea barras sin animación, genera mensajes, modal con proveedor
 */
(function () {
    'use strict';

    var alertasData = [];

    function init() {
        animateStockItems();
        generateRestockMessages();

        // Modal bindings
        var openBtn = document.getElementById('open-restock-modal');
        var overlay = document.getElementById('restock-overlay');
        var closeBtn = document.getElementById('restock-close');
        var closeBtnFooter = document.getElementById('restock-close-btn');

        if (openBtn) {
            openBtn.addEventListener('click', function (e) {
                e.preventDefault();
                openRestockModal();
            });
        }

        if (closeBtn) closeBtn.addEventListener('click', closeRestockModal);
        if (closeBtnFooter) closeBtnFooter.addEventListener('click', closeRestockModal);

        if (overlay) {
            overlay.addEventListener('click', function (e) {
                if (e.target === overlay) closeRestockModal();
            });
        }

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') closeRestockModal();
        });

        fetchAlertasData();
    }

    function fetchAlertasData() {
        fetch('/api/alertas/stock', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.ok ? r.json() : []; })
            .then(function (data) { alertasData = data; })
            .catch(function () { alertasData = []; });
    }

    // =========================================
    // ANIMACIONES DE RING + BARRA (sin animación)
    // =========================================
    function animateStockItems() {
        var items = document.querySelectorAll('.stock-item[data-cantidad]');
        if (!items.length) return;

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                var item = entry.target;
                var cant = parseInt(item.dataset.cantidad) || 0;
                var min = parseInt(item.dataset.minimo) || 1;

                // Barra: se setea directo sin animación
                var barP = Math.min((cant / min) * 100, 100);
                item.style.setProperty('--bar-width', barP + '%');

                // Ring: se anima con clase
                var ringP = Math.min((cant / (min * 2)) * 100, 100);
                var offset = 126 - (126 * ringP / 100);
                item.style.setProperty('--ring-offset', offset);

                setTimeout(function () { item.classList.add('animate'); }, 300);
                observer.unobserve(item);
            });
        }, { threshold: 0.2 });

        items.forEach(function (item) { observer.observe(item); });
    }

    // =========================================
    // MENSAJES DE RESTOCK (hover)
    // =========================================
    function generateRestockMessages() {
        document.querySelectorAll('.stock-item[data-cantidad]').forEach(function (item) {
            var el = item.querySelector('.stock-restock');
            if (!el) return;

            var c = parseInt(item.dataset.cantidad) || 0;
            var m = parseInt(item.dataset.minimo) || 1;
            var pct = (c / m) * 100;
            var need = m - c;
            var msg = '';

            if (c === 0) {
                msg = 'Sin stock — reabastecimiento inmediato';
            } else if (pct <= 25) {
                msg = 'Urgente — queda' + (c === 1 ? '' : 'n') + ' solo ' + c + ' unidad' + (c === 1 ? '' : 'es');
            } else if (pct <= 50) {
                msg = 'Necesita +' + need + ' unidad' + (need === 1 ? '' : 'es') + ' para el mínimo';
            } else if (pct <= 80) {
                msg = 'Considere comprar más stock pronto';
            } else {
                msg = 'Stock en el límite — conviene reabastecer';
            }

            var svg = el.querySelector('svg');
            el.textContent = '';
            if (svg) el.appendChild(svg);
            el.appendChild(document.createTextNode(' ' + msg));
        });
    }

    // =========================================
    // MODAL DE REABASTECIMIENTO
    // =========================================
    function openRestockModal() {
        var overlay = document.getElementById('restock-overlay');
        var body = document.getElementById('restock-body');
        if (!overlay || !body) return;

        var products = alertasData.length > 0 ? alertasData : getProductsFromDOM();

        // Ordenar: críticos primero, luego por cantidad ascendente
        products.sort(function (a, b) {
            var aNivel = (a.nivelAlerta || '').toUpperCase();
            var bNivel = (b.nivelAlerta || '').toUpperCase();
            if (aNivel !== bNivel) return aNivel === 'CRITICO' ? -1 : 1;
            return (a.cantidadDisponible || 0) - (b.cantidadDisponible || 0);
        });

        var html = '';
        products.forEach(function (p, i) {
            var nivel = (p.nivelAlerta || 'bajo').toLowerCase();
            var need = (p.stockMinimo || 0) - (p.cantidadDisponible || 0);
            if (need < 0) need = 0;
            var needText = need > 0
                ? 'Necesita <strong class="restock-need ' + nivel + '">+' + need + '</strong> unidad' + (need === 1 ? '' : 'es')
                : 'En el límite mínimo';

            html +=
                '<div class="restock-product" data-index="' + i + '">' +
                    '<div class="restock-product-header">' +
                        '<div class="restock-level-dot ' + nivel + '"></div>' +
                        '<div class="restock-product-info">' +
                            '<div class="restock-product-name">' +
                                esc(p.nombre || 'Producto') +
                                ' <span class="restock-product-badge ' + nivel + '">' +
                                    (nivel === 'critico' ? 'Crítico' : nivel === 'agotado' ? 'Agotado' : 'Bajo') +
                                '</span>' +
                            '</div>' +
                            '<div class="restock-product-stock">' +
                                'Stock: ' + (p.cantidadDisponible || 0) + ' / ' + (p.stockMinimo || 0) + ' mín.' +
                                ' &nbsp;·&nbsp; ' + needText +
                            '</div>' +
                        '</div>' +
                        '<div class="restock-chevron"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg></div>' +
                    '</div>' +
                    '<div class="restock-details">' +
                        '<div class="restock-details-inner">' +
                            '<div class="restock-grid">' +
                                '<div class="restock-field"><div class="restock-field-label">Categoría</div><div class="restock-field-value">' + esc(p.categoria || '—') + '</div></div>' +
                                '<div class="restock-field"><div class="restock-field-label">Marca</div><div class="restock-field-value">' + esc(p.marca || '—') + '</div></div>' +
                                '<div class="restock-field"><div class="restock-field-label">Ubicación</div><div class="restock-field-value">' + esc(p.ubicacion || '—') + '</div></div>' +
                                '<div class="restock-field"><div class="restock-field-label">Cantidad necesaria</div><div class="restock-field-value restock-need ' + nivel + '">+' + Math.max(need, 1) + ' unidades</div></div>' +
                            '</div>' +
                            buildSupplierCard(p) +
                        '</div>' +
                    '</div>' +
                '</div>';
        });

        body.innerHTML = html;

        // Accordion via event delegation
        body.addEventListener('click', function (e) {
            var header = e.target.closest('.restock-product-header');
            if (!header) return;
            var product = header.closest('.restock-product');
            var wasExpanded = product.classList.contains('expanded');
            // Cerrar todos
            body.querySelectorAll('.restock-product.expanded').forEach(function (p) {
                p.classList.remove('expanded');
            });
            // Toggle el que se clickeó
            if (!wasExpanded) product.classList.add('expanded');
        });

        // NO auto-expandimos ninguno — todos cerrados al abrir
        overlay.classList.add('open');
    }

    function buildSupplierCard(p) {
        if (!p.proveedorNombre) {
            return '<div style="font-size:12px; color:var(--fg-subtle); padding:10px 0;">Sin proveedor asignado</div>';
        }

        var contacts = '';

        if (p.proveedorTelefono) {
            contacts += '<a href="tel:' + p.proveedorTelefono + '" class="restock-contact">' +
                '<div class="restock-contact-icon phone"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg></div>' +
                '<div><div class="restock-contact-label">Teléfono</div><div class="restock-contact-text">' + esc(p.proveedorTelefono) + '</div></div></a>';
        }

        if (p.proveedorTelefono) {
            var waNum = p.proveedorTelefono.replace(/[^0-9]/g, '');
            contacts += '<a href="https://wa.me/' + waNum + '" target="_blank" class="restock-contact">' +
                '<div class="restock-contact-icon whatsapp"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z"/><path d="M12 0C5.373 0 0 5.373 0 12c0 2.127.555 4.126 1.528 5.86L.06 23.487a.5.5 0 0 0 .613.613l5.627-1.468A11.943 11.943 0 0 0 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 22a9.94 9.94 0 0 1-5.332-1.544l-.382-.228-3.332.869.886-3.236-.25-.396A9.935 9.935 0 0 1 2 12C2 6.477 6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z"/></svg></div>' +
                '<div><div class="restock-contact-label">WhatsApp</div><div class="restock-contact-text">' + esc(p.proveedorTelefono) + '</div></div></a>';
        }

        if (p.proveedorEmail) {
            contacts += '<a href="mailto:' + p.proveedorEmail + '" class="restock-contact">' +
                '<div class="restock-contact-icon email"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg></div>' +
                '<div><div class="restock-contact-label">Correo</div><div class="restock-contact-text">' + esc(p.proveedorEmail) + '</div></div></a>';
        }

        if (p.proveedorDireccion) {
            contacts += '<div class="restock-contact">' +
                '<div class="restock-contact-icon address"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg></div>' +
                '<div><div class="restock-contact-label">Dirección</div><div class="restock-contact-text">' + esc(p.proveedorDireccion) + '</div></div></div>';
        }

        return '<div class="restock-supplier">' +
            '<div class="restock-supplier-label">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>' +
                'Proveedor' +
            '</div>' +
            '<div class="restock-supplier-name">' + esc(p.proveedorNombre) + '</div>' +
            '<div class="restock-supplier-grid">' + contacts + '</div>' +
        '</div>';
    }

    // Fallback: leer datos del DOM si el fetch no funcionó
    function getProductsFromDOM() {
        var products = [];
        document.querySelectorAll('.stock-item[data-cantidad]').forEach(function (item) {
            products.push({
                productoId: parseInt(item.dataset.productoid) || 0,
                nombre: item.dataset.nombre || (item.querySelector('.stock-name') ? item.querySelector('.stock-name').textContent : 'Producto'),
                categoria: item.dataset.categoria || '',
                marca: item.dataset.marca || '',
                ubicacion: item.dataset.ubicacion || '',
                cantidadDisponible: parseInt(item.dataset.cantidad) || 0,
                stockMinimo: parseInt(item.dataset.minimo) || 0,
                nivelAlerta: item.classList.contains('critico') ? 'CRITICO' : 'BAJO',
                proveedorNombre: item.dataset.proveedor || null,
                proveedorTelefono: item.dataset.tel || null,
                proveedorEmail: item.dataset.email || null,
                proveedorDireccion: item.dataset.dir || null
            });
        });
        return products;
    }

    function closeRestockModal() {
        var overlay = document.getElementById('restock-overlay');
        if (overlay) overlay.classList.remove('open');
    }

    function esc(t) {
        var d = document.createElement('div');
        d.textContent = t;
        return d.innerHTML;
    }

    document.addEventListener('DOMContentLoaded', init);
})();