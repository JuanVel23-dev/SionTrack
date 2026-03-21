/**
 * SionTrack - Servicios Detalle Modal
 * Modal de vista detallada al hacer clic en el ojo (btn-ver-servicio)
 * Lee data-attributes del botón y construye el contenido del modal
 */
(function() {
    'use strict';

    var overlay = document.getElementById('srv-modal-overlay');
    var modalBody = document.getElementById('srv-modal-body');
    var modalSubtitle = document.getElementById('srv-modal-subtitle');
    var modalBadge = document.getElementById('srv-modal-badge');
    var modalCreado = document.getElementById('srv-modal-creado');
    var closeBtn = document.getElementById('srv-modal-close');
    var closeBtnFooter = document.getElementById('srv-modal-close-btn');

    if (!overlay) return;

    // ===== ABRIR MODAL =====
    document.addEventListener('click', function(e) {
        var btn = e.target.closest('.btn-ver-servicio');
        if (!btn) return;

        e.preventDefault();
        e.stopPropagation();
        abrirModal(btn);
    });

    // ===== CERRAR MODAL =====
    if (closeBtn) closeBtn.addEventListener('click', cerrarModal);
    if (closeBtnFooter) closeBtnFooter.addEventListener('click', cerrarModal);

    overlay.addEventListener('click', function(e) {
        if (e.target === overlay) cerrarModal();
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') cerrarModal();
    });

    function cerrarModal() {
        overlay.classList.remove('open');
    }

    function abrirModal(btn) {
        var d = btn.dataset;

        // Header
        modalSubtitle.textContent = 'Servicio #' + (d.id || '0');

        // Badge de estado
        var estadoMap = {
            'EN_PROCESO': { text: 'En Proceso', cls: 'badge-warning' },
            'COMPLETADO': { text: 'Completado', cls: 'badge-success' },
            'CANCELADO':  { text: 'Cancelado', cls: 'badge-error' }
        };
        var estado = estadoMap[d.estado] || { text: d.estado || '-', cls: 'badge-neutral' };
        modalBadge.textContent = estado.text;
        modalBadge.className = 'badge ' + estado.cls;

        // Footer - fecha creación
        if (d.creado) {
            var creadoDate = new Date(d.creado);
            modalCreado.textContent = 'Creado: ' + formatearFechaHora(creadoDate);
        } else {
            modalCreado.textContent = 'Creado: -';
        }

        // ===== CONSTRUIR BODY =====
        var html = '';

        // --- Sección: Información General ---
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>';
        html +=     'Información General';
        html +=   '</div>';
        html +=   '<div class="srv-detail-grid">';
        html +=     campo('Fecha del Servicio', formatearFecha(d.fecha));
        html +=     campo('Kilometraje', d.km ? d.km + ' km' : 'No registrado');
        html +=     campo('Total', d.total ? '$ ' + parseFloat(d.total).toLocaleString('es-CO', { minimumFractionDigits: 2 }) : '$ 0.00');
        html +=     campo('Estado', estado.text);
        html +=   '</div>';
        html += '</div>';

        // --- Sección: Cliente ---
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
        html +=     'Cliente';
        html +=   '</div>';
        html +=   '<div class="srv-detail-grid">';
        html +=     campo('Nombre', d.clienteNombre || 'Sin asignar');
        html +=     campo('Cédula / NIT', d.clienteCedula || '-');
        html +=     campo('Tipo', d.clienteTipo || '-');
        html +=   '</div>';
        html += '</div>';

        // --- Sección: Vehículo ---
        var tieneVehiculo = d.vehiculoPlaca;
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 16H9m10 0h3v-3.15a1 1 0 0 0-.84-.99L16 11l-2.7-3.6a1 1 0 0 0-.8-.4H5.24a2 2 0 0 0-1.8 1.1l-.8 1.63A6 6 0 0 0 2 12.42V16h2"/><circle cx="6.5" cy="16.5" r="2.5"/><circle cx="16.5" cy="16.5" r="2.5"/></svg>';
        html +=     'Vehículo';
        html +=   '</div>';

        if (tieneVehiculo) {
            html += '<div class="srv-detail-grid">';
            html +=   campo('Placa', d.vehiculoPlaca || '-');
            html +=   campo('Kilometraje', d.vehiculoKm ? d.vehiculoKm + ' km' : '-');
            html += '</div>';
        } else {
            html += '<div class="srv-detail-empty">Sin vehículo asignado</div>';
        }
        html += '</div>';

        // --- Sección: Productos / Items ---
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>';
        html +=     'Productos / Items';
        html +=   '</div>';

        // Los detalles vienen como data-detalles-json (string de objetos Thymeleaf)
        // Parseamos las filas de la tabla como fallback
        var detallesRows = obtenerDetallesDeFila(btn);
        if (detallesRows.length > 0) {
            html += '<div class="srv-detail-table">';
            html +=   '<div class="srv-detail-table-head">';
            html +=     '<span>Producto</span>';
            html +=     '<span>Tipo</span>';
            html +=     '<span>Cant.</span>';
            html +=     '<span>P. Unit.</span>';
            html +=     '<span>Subtotal</span>';
            html +=   '</div>';
            detallesRows.forEach(function(det) {
                var subtotal = (det.cantidad * det.precio).toFixed(2);
                html += '<div class="srv-detail-table-row">';
                html +=   '<span class="srv-detail-producto-nombre">' + esc(det.nombre) + '</span>';
                html +=   '<span class="srv-detail-tipo">' + esc(det.tipo) + '</span>';
                html +=   '<span>' + det.cantidad + '</span>';
                html +=   '<span>$ ' + parseFloat(det.precio).toLocaleString('es-CO', { minimumFractionDigits: 2 }) + '</span>';
                html +=   '<span class="srv-detail-subtotal">$ ' + parseFloat(subtotal).toLocaleString('es-CO', { minimumFractionDigits: 2 }) + '</span>';
                html += '</div>';
            });
            html += '</div>';
        } else {
            html += '<div class="srv-detail-empty">Sin productos registrados</div>';
        }
        html += '</div>';

        // --- Sección: Observaciones ---
        var obs = d.observaciones || '';
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>';
        html +=     'Observaciones';
        html +=   '</div>';
        if (obs.trim()) {
            html += '<div class="srv-detail-observaciones">' + esc(obs) + '</div>';
        } else {
            html += '<div class="srv-detail-empty">Sin observaciones</div>';
        }
        html += '</div>';

        modalBody.innerHTML = html;
        overlay.classList.add('open');
    }

    // ===== HELPERS =====

    function campo(label, valor) {
        return '<div class="srv-detail-field">' +
            '<div class="srv-detail-field-label">' + label + '</div>' +
            '<div class="srv-detail-field-value">' + esc(valor || '-') + '</div>' +
        '</div>';
    }

    /**
     * Intenta obtener detalles desde el data-attribute JSON,
     * si falla intenta parsear un formato simple
     */
    function obtenerDetallesDeFila(btn) {
        var raw = btn.getAttribute('data-detalles-json');
        if (!raw || raw === '[]') return [];

        try {
            var parsed = JSON.parse(raw);
            if (Array.isArray(parsed)) {
                return parsed.map(function(d) {
                    return {
                        nombre: d.nombre_producto || d.nombre || 'Producto',
                        tipo: d.tipoItem || d.tipo || 'PRODUCTO',
                        cantidad: parseFloat(d.cantidad) || 0,
                        precio: parseFloat(d.precio_unitario_congelado || d.precio) || 0
                    };
                });
            }
        } catch(e) {
            // No es JSON válido, ignorar
        }

        return [];
    }

    function formatearFecha(str) {
        if (!str) return '-';
        var parts = str.split('-');
        if (parts.length === 3) {
            return parts[2] + '/' + parts[1] + '/' + parts[0];
        }
        return str;
    }

    function formatearFechaHora(date) {
        if (!(date instanceof Date) || isNaN(date)) return '-';
        var d = pad(date.getDate()) + '/' + pad(date.getMonth() + 1) + '/' + date.getFullYear();
        var t = pad(date.getHours()) + ':' + pad(date.getMinutes());
        return d + ' ' + t;
    }

    function pad(n) { return n < 10 ? '0' + n : n; }

    function esc(text) {
        if (!text) return '-';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

})();