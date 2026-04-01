/**
 * SionTrack - Servicios Detalle Modal
 * Modal de vista detallada al hacer clic en el ojo (btn-ver-servicio)
 * Lee data-attributes del botón y construye el contenido del modal
 */
(function() {
    'use strict';

    var modalBody = document.getElementById('srv-modal-body');
    var modalSubtitle = document.getElementById('srv-modal-subtitle');
    var modalBadge = document.getElementById('srv-modal-badge');
    var modalCreado = document.getElementById('srv-modal-creado');

    // Inicializar modal con patrón reutilizable
    var modal = SionUtils.crearModal({
        overlayId: 'srv-modal-overlay',
        closeBtnIds: ['srv-modal-close', 'srv-modal-close-btn']
    });

    if (!modal) return;

    // ===== ABRIR MODAL =====
    document.addEventListener('click', function(e) {
        var btn = e.target.closest('.btn-ver-servicio');
        if (!btn) return;

        e.preventDefault();
        e.stopPropagation();
        abrirModal(btn);
    });

    function abrirModal(btn) {
        var servicioId = btn.dataset.id;

        // Muestra el modal de inmediato con estado de carga
        modalSubtitle.textContent = 'Servicio #' + (servicioId || '0');
        modalBadge.textContent = '...';
        modalBadge.className = 'badge badge-neutral';
        modalCreado.textContent = 'Cargando...';
        modalBody.innerHTML = '<div class="srv-detail-loading"><div class="srv-detail-spinner"></div><span>Cargando detalle...</span></div>';
        modal.abrir();

        // Obtiene el detalle completo del servicio desde la API
        fetch('/api/servicios/' + servicioId)
            .then(function(res) {
                if (!res.ok) throw new Error('Error al obtener el servicio');
                return res.json();
            })
            .then(function(srv) {
                renderizarContenido(srv);
            })
            .catch(function() {
                modalBody.innerHTML = '<div class="srv-detail-empty">No se pudo cargar el detalle del servicio.</div>';
            });
    }

    function renderizarContenido(srv) {
        var tipoServicioMap = {
            'PRODUCTO':     { text: 'Producto',     cls: 'badge-info' },
            'MANO_DE_OBRA': { text: 'Mano de Obra', cls: 'badge-warning' }
        };
        var tipoSrv = tipoServicioMap[srv.tipo_servicio] || { text: srv.tipo_servicio || '-', cls: 'badge-neutral' };

        // Header
        modalSubtitle.textContent = 'Servicio #' + srv.servicio_id;
        modalBadge.textContent = tipoSrv.text;
        modalBadge.className = 'badge ' + tipoSrv.cls;

        // Footer
        if (srv.creadoEn) {
            modalCreado.textContent = 'Creado: ' + formatearFechaHora(new Date(srv.creadoEn));
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
        html +=     campo('Fecha del Servicio', formatearFecha(srv.fecha_servicio));
        html +=     campo('Kilometraje', srv.kilometraje_servicio ? srv.kilometraje_servicio + ' km' : 'No registrado');
        html +=     campo('Total', srv.total ? '$ ' + parseFloat(srv.total).toLocaleString('es-CO', { minimumFractionDigits: 2 }) : '$ 0.00');
        html +=     campo('Tipo de Servicio', tipoSrv.text);
        html +=   '</div>';
        html += '</div>';

        // --- Sección: Cliente ---
        var cli = srv.cliente || {};
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
        html +=     'Cliente';
        html +=   '</div>';
        html +=   '<div class="srv-detail-grid">';
        html +=     campo('Nombre',        cli.nombre      || 'Sin asignar');
        html +=     campo('Cédula / NIT',  cli.cedula_ruc  || '-');
        html +=     campo('Tipo',          cli.tipo_cliente || '-');
        html +=   '</div>';
        html += '</div>';

        // --- Sección: Vehículo ---
        var veh = srv.vehiculo;
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 16H9m10 0h3v-3.15a1 1 0 0 0-.84-.99L16 11l-2.7-3.6a1 1 0 0 0-.8-.4H5.24a2 2 0 0 0-1.8 1.1l-.8 1.63A6 6 0 0 0 2 12.42V16h2"/><circle cx="6.5" cy="16.5" r="2.5"/><circle cx="16.5" cy="16.5" r="2.5"/></svg>';
        html +=     'Vehículo';
        html +=   '</div>';
        if (veh && veh.placa) {
            html += '<div class="srv-detail-grid">';
            html +=   campo('Placa', veh.placa || '-');
            html +=   campo('Kilometraje', veh.kilometraje_actual ? veh.kilometraje_actual + ' km' : '-');
            html += '</div>';
        } else {
            html += '<div class="srv-detail-empty">Sin vehículo asignado</div>';
        }
        html += '</div>';

        // --- Sección: Productos / Items ---
        var detalles = srv.detalles || [];
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>';
        html +=     'Productos / Items';
        html +=   '</div>';
        if (detalles.length > 0) {
            html += '<div class="srv-detail-table">';
            html +=   '<div class="srv-detail-table-head">';
            html +=     '<span>Producto</span><span>Tipo</span><span>Cant.</span><span>P. Unit.</span><span>Subtotal</span>';
            html +=   '</div>';
            detalles.forEach(function(det) {
                var cant    = parseFloat(det.cantidad) || 0;
                var precio  = parseFloat(det.precio_unitario_congelado) || 0;
                var subtotal = cant * precio;
                html += '<div class="srv-detail-table-row">';
                html +=   '<span class="srv-detail-producto-nombre">' + esc(det.nombre_producto || '-') + '</span>';
                html +=   '<span class="srv-detail-tipo">' + esc(det.tipoItem || 'PRODUCTO') + '</span>';
                html +=   '<span>' + cant + '</span>';
                html +=   '<span>$ ' + precio.toLocaleString('es-CO', { minimumFractionDigits: 2 }) + '</span>';
                html +=   '<span class="srv-detail-subtotal">$ ' + subtotal.toLocaleString('es-CO', { minimumFractionDigits: 2 }) + '</span>';
                html += '</div>';
            });
            html += '</div>';
        } else {
            html += '<div class="srv-detail-empty">Sin productos registrados</div>';
        }
        html += '</div>';

        // --- Sección: Observaciones ---
        var obs = srv.observaciones || '';
        html += '<div class="srv-detail-section">';
        html +=   '<div class="srv-detail-section-title">';
        html +=     '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>';
        html +=     'Observaciones';
        html +=   '</div>';
        if (obs && obs.trim()) {
            html += '<div class="srv-detail-observaciones">' + esc(obs) + '</div>';
        } else {
            html += '<div class="srv-detail-empty">Sin observaciones</div>';
        }
        html += '</div>';

        modalBody.innerHTML = html;
    }

    // ===== HELPERS (delegados a SionUtils) =====

    var esc = function(text) { return SionUtils.esc(text, '-'); };
    var formatearFecha = function(str) { return SionUtils.formatearFecha(str, '-'); };
    var formatearFechaHora = SionUtils.formatearFechaHora;

    function campo(label, valor) {
        return '<div class="srv-detail-field">' +
            '<div class="srv-detail-field-label">' + label + '</div>' +
            '<div class="srv-detail-field-value">' + esc(valor) + '</div>' +
        '</div>';
    }

})();;