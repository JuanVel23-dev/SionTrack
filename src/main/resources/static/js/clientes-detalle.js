/**
 * SionTrack - Clientes Detalle Modal
 * Modal de vista detallada al hacer clic en el ojo (btn-ver-cliente)
 * 
 * Carga los datos completos via fetch a /api/clientes/{id}
 * para obtener teléfonos, correos, direcciones y vehículos como JSON limpio.
 */
(function() {
    'use strict';

    var modalBody = document.getElementById('cli-modal-body');
    var modalSubtitle = document.getElementById('cli-modal-subtitle');
    var modalBadge = document.getElementById('cli-modal-badge');
    var modalFecha = document.getElementById('cli-modal-fecha');

    // Inicializar modal con patrón reutilizable
    var modal = SionUtils.crearModal({
        overlayId: 'cli-modal-overlay',
        closeBtnIds: ['cli-modal-close', 'cli-modal-close-btn']
    });

    if (!modal) return;

    // ===== ABRIR MODAL =====
    document.addEventListener('click', function(e) {
        var btn = e.target.closest('.btn-ver-cliente');
        if (!btn) return;

        e.preventDefault();
        e.stopPropagation();

        var clienteId = btn.dataset.id;
        if (!clienteId) return;

        cargarCliente(clienteId);
    });

    function cargarCliente(clienteId) {
        // Mostrar loading
        modalSubtitle.textContent = 'Cargando...';
        modalBadge.textContent = '';
        modalBadge.className = 'badge';
        modalFecha.textContent = '';
        modalBody.innerHTML =
            '<div style="display:flex;align-items:center;justify-content:center;padding:60px 24px;gap:12px;">' +
                '<div class="spinner"></div>' +
                '<span style="color:var(--fg-muted);font-size:14px;">Cargando datos del cliente...</span>' +
            '</div>';

        modal.abrir();

        // Fetch datos completos del cliente
        fetch('/api/clientes/' + clienteId, {
            headers: { 'Accept': 'application/json' }
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Error ' + response.status);
            return response.json();
        })
        .then(function(cliente) {
            renderizarCliente(cliente);
        })
        .catch(function(err) {
            console.error('Error cargando cliente:', err);
            modalBody.innerHTML =
                '<div style="text-align:center;padding:48px 24px;color:var(--error);">' +
                    '<p style="font-size:14px;margin-bottom:8px;">Error al cargar los datos del cliente</p>' +
                    '<p style="font-size:12px;color:var(--fg-subtle);">' + esc(err.message) + '</p>' +
                '</div>';
        });
    }

    function renderizarCliente(c) {
        // Header
        modalSubtitle.textContent = c.nombre || 'Cliente';

        // Badge tipo cliente
        var tipoMap = {
            'Persona Natural': { text: 'Persona Natural', cls: 'badge-success' },
            'Empresa':         { text: 'Empresa', cls: 'badge-info' }
        };
        var tipo = tipoMap[c.tipo_cliente] || { text: c.tipo_cliente || 'Sin tipo', cls: 'badge-neutral' };
        modalBadge.textContent = tipo.text;
        modalBadge.className = 'badge ' + tipo.cls;

        // Footer
        modalFecha.textContent = c.fecha_registro
            ? 'Registrado: ' + formatearFecha(c.fecha_registro)
            : 'Registrado: —';

        // ===== CONSTRUIR BODY =====
        var html = '';

        // --- Sección: Información Básica ---
        html += seccionInicio(svgUser(), 'Información Básica');
        html += '<div class="cli-detail-info-list">';
        html +=   infoRow(svgIdCard(), 'Nombre', c.nombre);
        html +=   infoRow(svgHash(), 'Cédula / NIT', c.cedula_ruc);
        html +=   infoRow(svgTag(), 'Tipo de Cliente', c.tipo_cliente);
        html +=   infoRow(svgCalendar(), 'Fecha de Registro', formatearFecha(c.fecha_registro));
        html +=   infoRow(svgClock(), 'Última Modificación', c.fecha_modificacion ? formatearFecha(c.fecha_modificacion) : 'Sin modificaciones');
        html += '</div>';

        // Notificaciones
        var notifActivo = c.recibe_notificaciones;
        html += '<div class="cli-detail-notif-row ' + (notifActivo ? 'active' : 'inactive') + '">';
        html +=   '<div class="cli-detail-notif-icon-wrap ' + (notifActivo ? 'active' : 'inactive') + '">' + svgBell() + '</div>';
        html +=   '<div class="cli-detail-notif-info">';
        html +=     '<div class="cli-detail-notif-label">Notificaciones WhatsApp</div>';
        html +=     '<div class="cli-detail-notif-status">';
        html +=       '<span class="cli-detail-notif-dot ' + (notifActivo ? 'active' : 'inactive') + '"></span>';
        html +=       '<span>' + (notifActivo ? 'Activadas' : 'Desactivadas') + '</span>';
        html +=     '</div>';
        html +=   '</div>';
        html += '</div>';
        html += seccionFin();

        // --- Sección: Teléfonos ---
        var telefonos = c.telefonos || [];
        html += seccionInicio(svgPhone(), 'Teléfonos (' + telefonos.length + ')');
        if (telefonos.length > 0) {
            html += '<div class="cli-detail-item-list">';
            telefonos.forEach(function(tel, idx) {
                html += itemRow('phone', svgPhone(), formatearTelefono(esc(tel.telefono)), 'Teléfono ' + (idx + 1));
            });
            html += '</div>';
        } else {
            html += emptyState(svgPhone(), 'Sin teléfonos registrados');
        }
        html += seccionFin();

        // --- Sección: Correos ---
        var correos = c.correos || [];
        html += seccionInicio(svgMail(), 'Correos Electrónicos (' + correos.length + ')');
        if (correos.length > 0) {
            html += '<div class="cli-detail-item-list">';
            correos.forEach(function(cor, idx) {
                html += itemRow('email', svgMail(), esc(cor.correo), 'Correo ' + (idx + 1));
            });
            html += '</div>';
        } else {
            html += emptyState(svgMail(), 'Sin correos registrados');
        }
        html += seccionFin();

        // --- Sección: Direcciones ---
        var direcciones = c.direcciones || [];
        html += seccionInicio(svgMapPin(), 'Direcciones (' + direcciones.length + ')');
        if (direcciones.length > 0) {
            html += '<div class="cli-detail-item-list">';
            direcciones.forEach(function(dir, idx) {
                var mapsUrl = 'https://www.google.com/maps/search/' + encodeURIComponent(dir.direccion);
                html += itemRowLink('address', svgMapPin(), esc(dir.direccion), 'Dirección ' + (idx + 1), mapsUrl);
            });
            html += '</div>';
        } else {
            html += emptyState(svgMapPin(), 'Sin direcciones registradas');
        }
        html += seccionFin();

        // --- Sección: Vehículos ---
        var vehiculos = c.vehiculos || [];
        html += seccionInicio(svgCar(), 'Vehículos (' + vehiculos.length + ')');
        if (vehiculos.length > 0) {
            html += '<div class="cli-detail-item-list">';
            vehiculos.forEach(function(v, idx) {
                var placa = v.placa || 'Sin placa';
                var km = v.kilometraje_actual;
                var kmTexto = km ? formatearKm(km) : '—';

                html += '<div class="cli-detail-item">';
                html +=   '<div class="cli-detail-item-icon vehicle">' + svgCar() + '</div>';
                html +=   '<div class="cli-detail-item-content">';
                html +=     '<div class="cli-detail-item-primary">' + esc(placa) + '</div>';
                html +=     '<div class="cli-detail-item-secondary">';
                html +=       '<span class="cli-detail-item-meta-icon">' + svgGauge() + '</span>';
                html +=       '<span>' + esc(kmTexto) + '</span>';
                html +=     '</div>';
                html +=   '</div>';
                html +=   '<div class="cli-detail-item-index">' + (idx + 1) + '</div>';
                html += '</div>';
            });
            html += '</div>';
        } else {
            html += emptyState(svgCar(), 'Sin vehículos registrados');
        }
        html += seccionFin();

        modalBody.innerHTML = html;
    }

    // ===== HELPERS =====

    var esc = SionUtils.esc;
    var formatearFecha = SionUtils.formatearFecha;
    var formatearTelefono = SionUtils.formatearTelefono;

    function seccionInicio(icon, titulo) {
        return '<div class="cli-detail-section">' +
            '<div class="cli-detail-section-title">' + icon + titulo + '</div>';
    }

    function seccionFin() { return '</div>'; }

    // Fila de información básica (icono + label + valor)
    function infoRow(icon, label, valor) {
        return '<div class="cli-detail-info-row">' +
            '<div class="cli-detail-info-icon">' + icon + '</div>' +
            '<div class="cli-detail-info-content">' +
                '<div class="cli-detail-info-label">' + label + '</div>' +
                '<div class="cli-detail-info-value">' + esc(valor) + '</div>' +
            '</div>' +
        '</div>';
    }

    // Item genérico de lista (teléfono, correo, vehículo)
    function itemRow(tipo, icon, texto, subtexto) {
        return '<div class="cli-detail-item">' +
            '<div class="cli-detail-item-icon ' + tipo + '">' + icon + '</div>' +
            '<div class="cli-detail-item-content">' +
                '<div class="cli-detail-item-primary">' + texto + '</div>' +
                '<div class="cli-detail-item-secondary">' + esc(subtexto) + '</div>' +
            '</div>' +
        '</div>';
    }

    // Item con enlace externo (direcciones → Maps)
    function itemRowLink(tipo, icon, texto, subtexto, url) {
        return '<a href="' + url + '" target="_blank" rel="noopener" class="cli-detail-item cli-detail-item-link">' +
            '<div class="cli-detail-item-icon ' + tipo + '">' + icon + '</div>' +
            '<div class="cli-detail-item-content">' +
                '<div class="cli-detail-item-primary">' + texto + '</div>' +
                '<div class="cli-detail-item-secondary">' + esc(subtexto) + ' · Abrir en Maps' + '</div>' +
            '</div>' +
            '<div class="cli-detail-item-arrow">' + svgExternalLink() + '</div>' +
        '</a>';
    }

    // Estado vacío con icono
    function emptyState(icon, texto) {
        return '<div class="cli-detail-empty-state">' +
            '<div class="cli-detail-empty-icon">' + icon + '</div>' +
            '<span>' + texto + '</span>' +
        '</div>';
    }

    function formatearKm(km) {
        var num = parseFloat(('' + km).replace(/[^\d.]/g, ''));
        if (isNaN(num)) return km + ' km';
        return num.toLocaleString('es-CO') + ' km';
    }

    // ===== SVG ICONS =====
    function svgUser() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
    }

    function svgPhone() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>';
    }

    function svgMail() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>';
    }

    function svgMapPin() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>';
    }

    function svgCar() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 16H9m10 0h3v-3.15a1 1 0 0 0-.84-.99L16 11l-2.7-3.6a1 1 0 0 0-.8-.4H5.24a2 2 0 0 0-1.8 1.1l-.8 1.63A6 6 0 0 0 2 12.42V16h2"/><circle cx="6.5" cy="16.5" r="2.5"/><circle cx="16.5" cy="16.5" r="2.5"/></svg>';
    }

    function svgGauge() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/><path d="M12 6v6l4 2"/></svg>';
    }

    function svgBell() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>';
    }

    function svgIdCard() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>';
    }

    function svgHash() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="9" x2="20" y2="9"/><line x1="4" y1="15" x2="20" y2="15"/><line x1="10" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="14" y2="21"/></svg>';
    }

    function svgTag() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>';
    }

    function svgCalendar() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>';
    }

    function svgClock() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>';
    }

    function svgExternalLink() {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>';
    }

})();