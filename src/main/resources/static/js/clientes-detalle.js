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
        html +=   '<div class="cli-detail-grid">';
        html +=     campo('Nombre', c.nombre);
        html +=     campo('Cédula / NIT', c.cedula_ruc);
        html +=     campo('Tipo de Cliente', c.tipo_cliente);
        html +=     campo('Fecha de Registro', formatearFecha(c.fecha_registro));
        html +=     campo('Última Modificación', c.fecha_modificacion ? formatearFecha(c.fecha_modificacion) : 'Sin modificaciones');
        html +=   '</div>';

        // Notificaciones
        html += '<div style="margin-top: 14px;">';
        html +=   '<div class="cli-detail-field-label" style="margin-bottom: 6px;">Notificaciones WhatsApp</div>';
        if (c.recibe_notificaciones) {
            html += '<span class="cli-detail-notif-badge active"><span class="cli-detail-notif-dot"></span> Acepta notificaciones</span>';
        } else {
            html += '<span class="cli-detail-notif-badge inactive"><span class="cli-detail-notif-dot"></span> No acepta notificaciones</span>';
        }
        html += '</div>';
        html += seccionFin();

        // --- Sección: Teléfonos ---
        var telefonos = c.telefonos || [];
        html += seccionInicio(svgPhone(), 'Teléfonos (' + telefonos.length + ')');
        if (telefonos.length > 0) {
            html += '<div class="cli-detail-list">';
            telefonos.forEach(function(tel) {
                html += '<div class="cli-detail-list-item">';
                html +=   '<div class="cli-detail-list-icon phone">' + svgPhone() + '</div>';
                html +=   '<span class="cli-detail-list-text">' + formatearTelefono(esc(tel.telefono)) + '</span>';
                html += '</div>';
            });
            html += '</div>';
        } else {
            html += '<div class="cli-detail-empty">Sin teléfonos registrados</div>';
        }
        html += seccionFin();

        // --- Sección: Correos ---
        var correos = c.correos || [];
        html += seccionInicio(svgMail(), 'Correos Electrónicos (' + correos.length + ')');
        if (correos.length > 0) {
            html += '<div class="cli-detail-list">';
            correos.forEach(function(cor) {
                html += '<div class="cli-detail-list-item">';
                html +=   '<div class="cli-detail-list-icon email">' + svgMail() + '</div>';
                html +=   '<span class="cli-detail-list-text">' + esc(cor.correo) + '</span>';
                html += '</div>';
            });
            html += '</div>';
        } else {
            html += '<div class="cli-detail-empty">Sin correos registrados</div>';
        }
        html += seccionFin();

        // --- Sección: Direcciones ---
        var direcciones = c.direcciones || [];
        html += seccionInicio(svgMapPin(), 'Direcciones (' + direcciones.length + ')');
        if (direcciones.length > 0) {
            html += '<div class="cli-detail-list">';
            direcciones.forEach(function(dir) {
                html += '<div class="cli-detail-list-item">';
                html +=   '<div class="cli-detail-list-icon address">' + svgMapPin() + '</div>';
                html +=   '<span class="cli-detail-list-text">' + esc(dir.direccion) + '</span>';
                html += '</div>';
            });
            html += '</div>';
        } else {
            html += '<div class="cli-detail-empty">Sin direcciones registradas</div>';
        }
        html += seccionFin();

        // --- Sección: Vehículos ---
        var vehiculos = c.vehiculos || [];
        html += seccionInicio(svgCar(), 'Vehículos (' + vehiculos.length + ')');
        if (vehiculos.length > 0) {
            vehiculos.forEach(function(v) {
                var marca = v.marca || '';
                var modelo = v.modelo || '';
                var placa = v.placa || 'Sin placa';
                var anio = v.anio || '—';
                var motor = v.tipo_motor || '—';
                var km = v.kilometraje_actual || '—';

                html += '<div class="cli-detail-vehicle-card">';
                html +=   '<div class="cli-detail-vehicle-header">';
                html +=     '<span class="cli-detail-vehicle-name">' + esc(marca + ' ' + modelo) + '</span>';
                html +=     '<span class="cli-detail-vehicle-plate">' + esc(placa) + '</span>';
                html +=   '</div>';
                html +=   '<div class="cli-detail-vehicle-grid">';
                html +=     vehicleCampo('Año', '' + anio);
                html +=     vehicleCampo('Motor', motor);
                html +=     vehicleCampo('Kilometraje', km !== '—' && km !== null ? km + ' km' : '—');
                html +=   '</div>';
                html += '</div>';
            });
        } else {
            html += '<div class="cli-detail-empty">Sin vehículos registrados</div>';
        }
        html += seccionFin();

        modalBody.innerHTML = html;
    }

    // ===== HELPERS (delegados a SionUtils) =====

    var esc = SionUtils.esc;
    var formatearFecha = SionUtils.formatearFecha;
    var formatearTelefono = SionUtils.formatearTelefono;

    function seccionInicio(icon, titulo) {
        return '<div class="cli-detail-section">' +
            '<div class="cli-detail-section-title">' + icon + titulo + '</div>';
    }

    function seccionFin() {
        return '</div>';
    }

    function campo(label, valor) {
        return '<div class="cli-detail-field">' +
            '<div class="cli-detail-field-label">' + label + '</div>' +
            '<div class="cli-detail-field-value">' + esc(valor) + '</div>' +
        '</div>';
    }

    function vehicleCampo(label, valor) {
        return '<div>' +
            '<div class="cli-detail-vehicle-field-label">' + label + '</div>' +
            '<div class="cli-detail-vehicle-field-value">' + esc(valor) + '</div>' +
        '</div>';
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

})();