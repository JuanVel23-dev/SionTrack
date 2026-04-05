/**
 * SionTrack - Promociones Form
 * Custom select, custom calendar, precio formateado, preview de mensaje,
 * selección de clientes con carga AJAX y contador en tiempo real.
 */
(function() {
    'use strict';

    var MESES = ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
                 'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
    var DIAS_SEMANA = ['Do','Lu','Ma','Mi','Ju','Vi','Sa'];

    var state = {
        productoNombre: '',
        productoId: null,
        fechaInicio: null,
        fechaFin: null,
        calInicioMes: null, calInicioAnio: null,
        calFinMes: null, calFinAnio: null,
        // Lista de clientes cargados desde la API
        clientes: []
    };

    document.addEventListener('DOMContentLoaded', function() {
        var form = document.getElementById('promocionForm');
        var productoHidden = document.getElementById('productoId');
        var promocionInput = document.getElementById('promocion');
        var precioDisplay = document.getElementById('precioDisplay');
        var precioHidden = document.getElementById('precioOferta');
        var rangoHidden = document.getElementById('rangoFechas');
        var previewContainer = document.getElementById('promo-preview-content');
        var hoy = new Date();

        // ===== CUSTOM SELECT CON BÚSQUEDA =====
        var selectWrap = document.getElementById('producto-select');
        if (selectWrap) {
            var selectBtn = selectWrap.querySelector('.promo-select-btn');
            var selectTexto = selectWrap.querySelector('.promo-select-texto');
            var searchInput = document.getElementById('promo-search-input');
            var emptyMsg = document.getElementById('promo-select-empty');
            var opciones = selectWrap.querySelectorAll('.promo-select-option:not(.disabled)');
            var highlightIndex = -1;

            // Abre/cierra el dropdown
            selectBtn.addEventListener('click', function(e) {
                e.preventDefault(); e.stopPropagation();
                var abriendo = !selectWrap.classList.contains('open');
                selectWrap.classList.toggle('open');
                if (abriendo && searchInput) {
                    // Limpiar búsqueda anterior y enfocar
                    searchInput.value = '';
                    filtrarProductos('');
                    highlightIndex = -1;
                    setTimeout(function() { searchInput.focus(); }, 60);
                }
            });

            // Seleccionar una opción
            opciones.forEach(function(op) {
                op.addEventListener('click', function() {
                    seleccionarProducto(this);
                });
            });

            function seleccionarProducto(opcion) {
                opciones.forEach(function(o) { o.classList.remove('selected'); });
                opcion.classList.add('selected');
                selectTexto.textContent = opcion.textContent.trim();
                selectTexto.classList.remove('placeholder');
                productoHidden.value = opcion.dataset.value;
                state.productoId = parseInt(opcion.dataset.value);
                state.productoNombre = opcion.textContent.trim();
                selectWrap.classList.remove('open');
                actualizarPreview();
                cargarClientesPreview(state.productoId);
            }

            // Filtrado de productos en tiempo real
            function filtrarProductos(termino) {
                var term = termino.toLowerCase().trim();
                var hayVisibles = false;

                opciones.forEach(function(op) {
                    var texto = op.textContent.toLowerCase();
                    var visible = !term || texto.indexOf(term) !== -1;
                    op.style.display = visible ? '' : 'none';
                    op.classList.remove('highlighted');
                    if (visible) hayVisibles = true;
                });

                // Ocultar/mostrar la opción disabled de placeholder al buscar
                var placeholderOp = selectWrap.querySelector('.promo-select-option.disabled');
                if (placeholderOp) placeholderOp.style.display = term ? 'none' : '';

                // Estado vacío
                if (emptyMsg) emptyMsg.style.display = (!term || hayVisibles) ? 'none' : 'flex';

                highlightIndex = -1;
            }

            if (searchInput) {
                searchInput.addEventListener('input', function() {
                    filtrarProductos(this.value);
                });

                // Navegación con teclado
                searchInput.addEventListener('keydown', function(e) {
                    var visibles = [];
                    opciones.forEach(function(op) {
                        if (op.style.display !== 'none') visibles.push(op);
                    });

                    if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        highlightIndex = Math.min(highlightIndex + 1, visibles.length - 1);
                        actualizarHighlight(visibles);
                    } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        highlightIndex = Math.max(highlightIndex - 1, 0);
                        actualizarHighlight(visibles);
                    } else if (e.key === 'Enter') {
                        e.preventDefault();
                        if (highlightIndex >= 0 && visibles[highlightIndex]) {
                            seleccionarProducto(visibles[highlightIndex]);
                        }
                    } else if (e.key === 'Escape') {
                        selectWrap.classList.remove('open');
                    }
                });

                // Evitar que el click en el input cierre el dropdown
                searchInput.addEventListener('click', function(e) {
                    e.stopPropagation();
                });
            }

            function actualizarHighlight(visibles) {
                opciones.forEach(function(op) { op.classList.remove('highlighted'); });
                if (highlightIndex >= 0 && visibles[highlightIndex]) {
                    visibles[highlightIndex].classList.add('highlighted');
                    // Scroll al elemento visible
                    visibles[highlightIndex].scrollIntoView({ block: 'nearest' });
                }
            }

            // Cerrar al hacer click fuera
            document.addEventListener('click', function(e) {
                if (!selectWrap.contains(e.target)) selectWrap.classList.remove('open');
            });
        }

        // ===== PRECIO =====
        if (precioDisplay) {
            precioDisplay.addEventListener('input', function() {
                var raw = this.value.replace(/[^0-9]/g, '');
                if (!raw) { this.value = ''; precioHidden.value = ''; actualizarPreview(); return; }
                var num = parseInt(raw);
                this.value = num.toLocaleString('es-CO');
                precioHidden.value = '$' + num.toLocaleString('es-CO');
                actualizarPreview();
            });
            precioDisplay.addEventListener('keydown', function(e) {
                if ([8,9,13,27,46,37,38,39,40].indexOf(e.keyCode) !== -1) return;
                if ((e.ctrlKey || e.metaKey) && [65,67,86,88].indexOf(e.keyCode) !== -1) return;
                if ((e.keyCode < 48 || e.keyCode > 57) && (e.keyCode < 96 || e.keyCode > 105)) e.preventDefault();
            });
        }

        // ===== CALENDAR =====
        var now = new Date();
        state.calInicioMes = now.getMonth(); state.calInicioAnio = now.getFullYear();
        state.calFinMes = now.getMonth(); state.calFinAnio = now.getFullYear();

        initCalendar('inicio'); initCalendar('fin');

        var calInicio = document.getElementById('cal-inicio');
        var calFin = document.getElementById('cal-fin');
        if (calInicio) calInicio.addEventListener('click', handleCalClick);
        if (calFin) calFin.addEventListener('click', handleCalClick);

        function initCalendar(tipo) {
            var wrap = document.getElementById('fecha-' + tipo + '-wrap');
            var btn = document.getElementById('fecha-' + tipo + '-btn');
            if (!wrap || !btn) return;

            btn.addEventListener('click', function(e) {
                e.preventDefault(); e.stopPropagation();
                document.querySelectorAll('.fecha-campo.active').forEach(function(f) {
                    if (f !== wrap) f.classList.remove('active');
                });
                wrap.classList.toggle('active');
                if (wrap.classList.contains('active')) renderCalendar(tipo);
            });
            document.addEventListener('click', function(e) {
                if (!wrap.contains(e.target)) wrap.classList.remove('active');
            });
        }

        function renderCalendar(tipo) {
            var calEl = document.getElementById('cal-' + tipo);
            var mes = tipo === 'inicio' ? state.calInicioMes : state.calFinMes;
            var anio = tipo === 'inicio' ? state.calInicioAnio : state.calFinAnio;
            var sel = tipo === 'inicio' ? state.fechaInicio : state.fechaFin;

            var primerDia = new Date(anio, mes, 1).getDay();
            var diasEnMes = new Date(anio, mes + 1, 0).getDate();
            var diasMesAnt = new Date(anio, mes, 0).getDate();

            var html = '<div class="cal-header">';
            html += '<button type="button" class="cal-nav-btn" data-action="prev" data-tipo="'+tipo+'"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"></polyline></svg></button>';
            html += '<span class="cal-title">' + MESES[mes] + ' ' + anio + '</span>';
            html += '<button type="button" class="cal-nav-btn" data-action="next" data-tipo="'+tipo+'"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg></button>';
            html += '</div><div class="cal-grid"><div class="cal-weekdays">';
            DIAS_SEMANA.forEach(function(d) { html += '<span class="cal-weekday">' + d + '</span>'; });
            html += '</div><div class="cal-days">';

            for (var i = primerDia - 1; i >= 0; i--)
                html += '<button type="button" class="cal-day other-month disabled">' + (diasMesAnt - i) + '</button>';

            var hoyInicio = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
            for (var d = 1; d <= diasEnMes; d++) {
                var cls = 'cal-day';
                var fechaDia = new Date(anio, mes, d);
                var isToday = d === hoy.getDate() && mes === hoy.getMonth() && anio === hoy.getFullYear();
                var isSel = sel && d === sel.day && mes === sel.month && anio === sel.year;
                var isDis = false;
                // No permitir fechas pasadas
                if (fechaDia < hoyInicio) isDis = true;
                // En fecha fin, no permitir antes de fecha inicio
                if (tipo === 'fin' && state.fechaInicio) {
                    if (fechaDia < new Date(state.fechaInicio.year,state.fechaInicio.month,state.fechaInicio.day)) isDis = true;
                }
                if (isToday) cls += ' today';
                if (isSel) cls += ' selected';
                if (isDis) cls += ' disabled';
                html += '<button type="button" class="'+cls+'" data-day="'+d+'" data-month="'+mes+'" data-year="'+anio+'" data-tipo="'+tipo+'">'+d+'</button>';
            }

            var total = primerDia + diasEnMes;
            var rem = total % 7 === 0 ? 0 : 7 - (total % 7);
            for (var r = 1; r <= rem; r++)
                html += '<button type="button" class="cal-day other-month disabled">' + r + '</button>';

            html += '</div></div><div class="cal-footer">';
            html += '<button type="button" class="cal-footer-btn" data-action="clear" data-tipo="'+tipo+'">Limpiar</button>';
            html += '<button type="button" class="cal-footer-btn today-btn" data-action="today" data-tipo="'+tipo+'">Hoy</button>';
            html += '</div>';
            calEl.innerHTML = html;
        }

        function handleCalClick(e) {
            e.stopPropagation();
            var btn = e.target.closest('[data-action]');
            var dayBtn = e.target.closest('.cal-day:not(.disabled):not(.other-month)');
            if (btn) {
                var a = btn.dataset.action, t = btn.dataset.tipo;
                if (a === 'prev') {
                    if (t==='inicio') { state.calInicioMes--; if(state.calInicioMes<0){state.calInicioMes=11;state.calInicioAnio--;} }
                    else { state.calFinMes--; if(state.calFinMes<0){state.calFinMes=11;state.calFinAnio--;} }
                    renderCalendar(t);
                } else if (a === 'next') {
                    if (t==='inicio') { state.calInicioMes++; if(state.calInicioMes>11){state.calInicioMes=0;state.calInicioAnio++;} }
                    else { state.calFinMes++; if(state.calFinMes>11){state.calFinMes=0;state.calFinAnio++;} }
                    renderCalendar(t);
                } else if (a === 'today') { selFecha(t, hoy.getFullYear(), hoy.getMonth(), hoy.getDate()); }
                else if (a === 'clear') {
                    if (t==='inicio') state.fechaInicio=null; else state.fechaFin=null;
                    updTexto(t); updRango(); actualizarPreview();
                    document.getElementById('fecha-'+t+'-wrap').classList.remove('active');
                }
                return;
            }
            if (dayBtn) selFecha(dayBtn.dataset.tipo, +dayBtn.dataset.year, +dayBtn.dataset.month, +dayBtn.dataset.day);
        }

        function selFecha(t,y,m,d) {
            var f = {year:y,month:m,day:d};
            if (t==='inicio') {
                state.fechaInicio = f;
                if (state.fechaFin && new Date(state.fechaFin.year,state.fechaFin.month,state.fechaFin.day) < new Date(y,m,d)) {
                    state.fechaFin = null; updTexto('fin');
                }
            } else state.fechaFin = f;
            updTexto(t); updRango(); actualizarPreview();
            document.getElementById('fecha-'+t+'-wrap').classList.remove('active');
        }

        function updTexto(t) {
            var el = document.getElementById('fecha-'+t+'-texto');
            var btn = document.getElementById('fecha-'+t+'-btn');
            var f = t==='inicio' ? state.fechaInicio : state.fechaFin;
            if (!el) return;
            if (!f) { el.textContent='Seleccionar'; el.classList.add('placeholder-text'); if(btn)btn.classList.remove('has-value'); }
            else { el.textContent=f.day+' de '+MESES[f.month]+', '+f.year; el.classList.remove('placeholder-text'); if(btn)btn.classList.add('has-value'); }
        }

        function updRango() {
            if (!state.fechaInicio||!state.fechaFin) { rangoHidden.value=''; return; }
            var fi=state.fechaInicio, ff=state.fechaFin;
            rangoHidden.value = (fi.month===ff.month && fi.year===ff.year)
                ? fi.day+' al '+ff.day+' de '+MESES[ff.month]
                : fi.day+' de '+MESES[fi.month]+' al '+ff.day+' de '+MESES[ff.month];
        }

        // ===== PREVIEW DEL MENSAJE =====
        function actualizarPreview() {
            if (!previewContainer) return;
            var producto = state.productoNombre||'', promo = promocionInput?promocionInput.value.trim():'',
                pr = precioDisplay?precioDisplay.value.trim():'', precio = pr?'$'+pr:'', fechas = rangoHidden.value||'';
            if (!producto&&!promo&&!pr&&!fechas) {
                previewContainer.innerHTML = '<div class="promo-preview-empty"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg><p>Completa el formulario para ver la vista previa</p></div>';
                return;
            }
            previewContainer.innerHTML =
                '<div class="promo-preview-bubble"><div class="promo-preview-header"><div class="promo-preview-wa-icon"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z"/><path d="M12 0C5.373 0 0 5.373 0 12c0 2.127.555 4.126 1.528 5.86L.06 23.487a.5.5 0 0 0 .613.613l5.627-1.468A11.943 11.943 0 0 0 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 22a9.94 9.94 0 0 1-5.332-1.544l-.382-.228-3.332.869.886-3.236-.25-.396A9.935 9.935 0 0 1 2 12C2 6.477 6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z"/></svg></div><div><div class="promo-preview-wa-title">SionTrack</div><div class="promo-preview-wa-subtitle">Plantilla: promociones</div></div></div>' +
                '<div class="promo-preview-body">Hola <span class="promo-highlight">{nombre_cliente}</span><br>Te saluda Jenny de Sion Filtros.<br><br><strong>'+esc(promo||'___')+'</strong><br><br>Incluye mano de obra y revisión de 10 puntos GRATIS.<br>Todo por solo <span class="promo-highlight">'+esc(precio||'___')+'</span><br><br>Válido del <span class="promo-highlight">'+esc(fechas||'___')+'</span><br><br>Muestra este mensaje y reclama la PROMO.<br>📍 CALLE 170 #17A 77</div></div>';
        }
        if (promocionInput) promocionInput.addEventListener('input', actualizarPreview);
        actualizarPreview();

        // ===== CARGA DE CLIENTES VÍA AJAX =====
        function cargarClientesPreview(productoId) {
            var seccion     = document.getElementById('clientes-preview-section');
            var loading     = document.getElementById('clientes-loading');
            var tablaWrap   = document.getElementById('clientes-tabla-wrap');
            var vacio       = document.getElementById('clientes-vacio');
            var divider     = document.getElementById('divider-clientes');

            // Muestra la sección y el spinner
            seccion.style.display = 'block';
            loading.style.display = 'flex';
            tablaWrap.style.display = 'none';
            vacio.style.display = 'none';
            divider.style.display = 'block';

            // Anima la aparición de la sección
            requestAnimationFrame(function() { seccion.classList.add('visible'); });

            fetch('/api/promociones/preview?productoId=' + productoId)
                .then(function(res) {
                    if (!res.ok) throw new Error('Error al cargar clientes');
                    return res.json();
                })
                .then(function(clientes) {
                    state.clientes = clientes;
                    loading.style.display = 'none';
                    if (!clientes || clientes.length === 0) {
                        vacio.style.display = 'flex';
                    } else {
                        renderizarTablaClientes(clientes);
                        tablaWrap.style.display = 'block';
                    }
                    actualizarContador();
                })
                .catch(function() {
                    loading.style.display = 'none';
                    vacio.style.display = 'flex';
                    if (typeof showToast === 'function') showToast('No se pudo cargar la lista de clientes', 'error');
                });
        }

        // Construye las filas de la tabla de clientes
        function renderizarTablaClientes(clientes) {
            var tbody = document.getElementById('clientes-tbody');
            var html = '';

            clientes.forEach(function(c) {
                var elegible = c.tieneConsentimiento && c.tieneTelefono;
                var checked = elegible && !c.contactadoRecientemente;
                var rowClass = c.contactadoRecientemente ? 'fila-reciente' : '';

                // Badge de estado
                var badge = '';
                if (!c.tieneConsentimiento) {
                    badge = '<span class="cliente-badge badge-sin-consentimiento">Sin consentimiento</span>';
                } else if (!c.tieneTelefono) {
                    badge = '<span class="cliente-badge badge-sin-telefono">Sin teléfono</span>';
                } else if (c.contactadoRecientemente) {
                    var texto = c.diasDesdeUltimaPromocion === 0
                        ? 'Contactado hoy'
                        : 'Contactado hace ' + c.diasDesdeUltimaPromocion + (c.diasDesdeUltimaPromocion === 1 ? ' día' : ' días');
                    badge = '<span class="cliente-badge badge-reciente">' + esc(texto) + '</span>';
                } else if (c.diasDesdeUltimaPromocion !== null && c.diasDesdeUltimaPromocion !== undefined) {
                    badge = '<span class="cliente-badge badge-ok">Último contacto: ' + c.diasDesdeUltimaPromocion + 'd</span>';
                } else {
                    badge = '<span class="cliente-badge badge-nuevo">Nuevo</span>';
                }

                html += '<tr class="' + rowClass + '">';
                html += '<td class="col-check">';
                if (elegible) {
                    html += '<input type="checkbox" class="cliente-check" data-id="' + c.clienteId + '"' + (checked ? ' checked' : '') + '>';
                } else {
                    html += '<input type="checkbox" class="cliente-check" data-id="' + c.clienteId + '" disabled>';
                }
                html += '</td>';
                html += '<td class="col-nombre">' + esc(c.nombre) + '</td>';
                html += '<td class="col-telefono">' + esc(c.telefono || '—') + '</td>';
                html += '<td class="col-estado">' + badge + '</td>';
                html += '</tr>';
            });

            tbody.innerHTML = html;

            // Eventos de los checkboxes individuales
            tbody.querySelectorAll('.cliente-check').forEach(function(chk) {
                chk.addEventListener('change', function() {
                    actualizarContador();
                    sincronizarCheckMaestro();
                });
            });

            sincronizarCheckMaestro();
        }

        // ===== CONTADOR EN TIEMPO REAL =====
        function actualizarContador() {
            var checks = document.querySelectorAll('#clientes-tbody .cliente-check:checked');
            var total = checks.length;

            var textoContador = document.getElementById('contador-texto');
            var badge = document.getElementById('contador-badge');
            var btnTexto = document.getElementById('btn-enviar-texto');

            if (textoContador) {
                textoContador.textContent = total === 0
                    ? 'Sin destinatarios seleccionados'
                    : total === 1
                    ? '1 mensaje a enviar'
                    : total + ' mensajes a enviar';
            }

            // Cambia el color del badge según la cantidad
            if (badge) {
                badge.classList.remove('badge-cero', 'badge-normal');
                badge.classList.add(total === 0 ? 'badge-cero' : 'badge-normal');
            }

            // Actualiza el texto del botón de envío
            if (btnTexto) {
                btnTexto.textContent = total === 0
                    ? 'Enviar Promoción'
                    : 'Enviar a ' + total + (total === 1 ? ' cliente' : ' clientes');
            }
        }

        // Sincroniza el checkbox maestro del thead con el estado de los individuales
        function sincronizarCheckMaestro() {
            var checkMaestro = document.getElementById('check-maestro');
            if (!checkMaestro) return;
            var todos = document.querySelectorAll('#clientes-tbody .cliente-check:not(:disabled)');
            var marcados = document.querySelectorAll('#clientes-tbody .cliente-check:not(:disabled):checked');
            checkMaestro.checked = todos.length > 0 && marcados.length === todos.length;
            checkMaestro.indeterminate = marcados.length > 0 && marcados.length < todos.length;
        }

        // Checkbox maestro — selecciona o deselecciona todos los habilitados
        document.addEventListener('change', function(e) {
            if (e.target && e.target.id === 'check-maestro') {
                var marcado = e.target.checked;
                document.querySelectorAll('#clientes-tbody .cliente-check:not(:disabled)').forEach(function(c) {
                    c.checked = marcado;
                });
                actualizarContador();
            }
        });

        // Botones de selección rápida con animación en cascada
        var btnTodos = document.getElementById('btn-seleccionar-todos');
        var btnNinguno = document.getElementById('btn-deseleccionar-todos');

        function animarCheckboxes(checks, marcar) {
            checks.forEach(function(c, i) {
                setTimeout(function() {
                    c.checked = marcar;
                    // Efecto de escala breve al marcar
                    c.style.transform = 'scale(1.25)';
                    setTimeout(function() { c.style.transform = ''; }, 150);
                }, i * 20);
            });
            // Actualizar contador al final de la animación
            setTimeout(function() {
                actualizarContador();
                sincronizarCheckMaestro();
            }, checks.length * 20 + 50);
        }

        if (btnTodos) {
            btnTodos.addEventListener('click', function() {
                var checks = document.querySelectorAll('#clientes-tbody .cliente-check:not(:disabled)');
                animarCheckboxes(checks, true);
            });
        }
        if (btnNinguno) {
            btnNinguno.addEventListener('click', function() {
                var checks = document.querySelectorAll('#clientes-tbody .cliente-check:not(:disabled)');
                animarCheckboxes(checks, false);
            });
        }

        // ===== SUBMIT VÍA API REST =====
        var btnEnviar = document.getElementById('btn-enviar-promo');
        if (btnEnviar) {
            btnEnviar.addEventListener('click', function(e) {
                e.preventDefault();

                // Validaciones del formulario
                var err = [];
                if (!productoHidden.value) err.push('Selecciona un producto');
                if (!promocionInput || !promocionInput.value.trim()) err.push('Describe la promoción');
                if (!precioDisplay || !precioDisplay.value.trim()) err.push('Ingresa el precio');
                if (!state.fechaInicio) err.push('Selecciona fecha de inicio');
                if (!state.fechaFin) err.push('Selecciona fecha de fin');

                // Valida que haya al menos un cliente seleccionado
                var checksSeleccionados = document.querySelectorAll('#clientes-tbody .cliente-check:checked');
                if (checksSeleccionados.length === 0 && state.clientes.length > 0) {
                    err.push('Selecciona al menos un destinatario');
                }

                if (err.length) {
                    if (typeof showToast === 'function') showToast(err[0], 'error');
                    return;
                }

                updRango();

                // Recopila los IDs seleccionados
                var idsSeleccionados = [];
                checksSeleccionados.forEach(function(c) {
                    idsSeleccionados.push(parseInt(c.dataset.id));
                });

                var total = idsSeleccionados.length;
                var textoConfirm = 'Se enviará esta promoción de <strong>' + esc(state.productoNombre) + '</strong> a <strong>' + total + (total === 1 ? ' cliente' : ' clientes') + '</strong>. ¿Continuar?';

                function ejecutarEnvio() {
                    var payload = {
                        productoId: state.productoId,
                        promocion: promocionInput.value.trim(),
                        precioOferta: precioHidden.value,
                        rangoFechas: rangoHidden.value,
                        clientesSeleccionados: idsSeleccionados
                    };

                    // Estado de carga en el botón
                    btnEnviar.disabled = true;
                    var btnTexto = document.getElementById('btn-enviar-texto');
                    if (btnTexto) btnTexto.textContent = 'Enviando...';

                    SionUtils.fetchSeguro('/api/promociones/enviar', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    })
                    .then(function(res) {
                        if (!res.ok) throw new Error('Error al enviar');
                        return res.json();
                    })
                    .then(function(resultado) {
                        // Redirige con mensaje flash construido en el frontend como parámetro de URL
                        var enviados = resultado.enviados || 0;
                        var clientes = resultado.clientesEncontrados || 0;
                        var fallidos = resultado.fallidos || 0;
                        var sinTel = resultado.sinTelefono || 0;
                        var sinCons = resultado.sinConsentimiento || 0;
                        var prod = resultado.producto || '';

                        var tipo = enviados > 0 ? 'success' : 'error';
                        var msg;
                        if (clientes === 0) {
                            msg = 'No se encontraron clientes para "' + prod + '"';
                        } else {
                            msg = 'Promoción enviada — ' + enviados + ' de ' + clientes + ' clientes notificados.';
                            if (fallidos > 0) msg += ' Fallidos: ' + fallidos + '.';
                            if (sinTel > 0) msg += ' Sin teléfono: ' + sinTel + '.';
                            if (sinCons > 0) msg += ' Sin consentimiento: ' + sinCons + '.';
                        }

                        // Guarda el mensaje en sessionStorage para mostrarlo al llegar al listado
                        try { sessionStorage.setItem('promoToast', JSON.stringify({ tipo: tipo, msg: msg })); } catch(e) {}
                        window.location.href = '/web/notificaciones';
                    })
                    .catch(function() {
                        btnEnviar.disabled = false;
                        if (btnTexto) btnTexto.textContent = 'Enviar a ' + total + (total === 1 ? ' cliente' : ' clientes');
                        if (typeof showToast === 'function') showToast('Ocurrió un error al enviar la promoción. Intenta nuevamente.', 'error');
                    });
                }

                if (typeof confirmAction === 'function') {
                    confirmAction(textoConfirm, ejecutarEnvio, { title: 'Confirmar Envío', confirmText: 'Enviar', type: 'primary' });
                } else {
                    if (confirm('¿Enviar promoción a ' + total + ' clientes?')) ejecutarEnvio();
                }
            });
        }

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                document.querySelectorAll('.fecha-campo.active').forEach(function(f) { f.classList.remove('active'); });
                document.querySelectorAll('.promo-select.open').forEach(function(s) { s.classList.remove('open'); });
            }
        });
    });

    function esc(t) { return SionUtils.esc(t, ''); }
})();
