
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {

        
        var tabs = document.querySelectorAll('.ntab-btn');
        var track = document.getElementById('ntab-track');
        var pendientesCargados = false;

        if (tabs.length && track) {
            tabs.forEach(function(tab, index) {
                tab.addEventListener('click', function() {
                    if (this.classList.contains('active')) return;

                    tabs.forEach(function(t) { t.classList.remove('active'); });
                    this.classList.add('active');

                    track.style.transform = 'translateX(-' + (index * 100) + '%)';

                    
                    if (tab.dataset.tab === 'pendientes' && !pendientesCargados) {
                        cargarPendientes();
                    }
                });
            });

            
            var container = document.querySelector('.table-container[data-tab-activo]');
            var tabActivo = container ? container.getAttribute('data-tab-activo') : null;
            if (tabActivo && tabActivo !== 'promociones') {
                var tabBtn = document.querySelector('.ntab-btn[data-tab="' + tabActivo + '"]');
                if (tabBtn) tabBtn.click();
            }
        }

        
        document.querySelectorAll('.ntab-search').forEach(function(input) {
            input.addEventListener('input', SionUtils.debounce(function() {
                var target = input.dataset.target;
                var term = input.value.toLowerCase().trim();

                if (target === 'pendientes') {
                    cargarPendientes(0, term);
                    return;
                }

                var rows = document.querySelectorAll('.data-row-' + target);

                rows.forEach(function(row) {
                    var visible = row.textContent.toLowerCase().indexOf(term) !== -1;

                    if (visible && row.style.display === 'none') {
                        row.style.display = '';
                        row.style.opacity = '0';
                        requestAnimationFrame(function() {
                            row.style.transition = 'opacity 0.2s ease';
                            row.style.opacity = '1';
                        });
                    } else if (!visible && row.style.display !== 'none') {
                        row.style.transition = 'opacity 0.15s ease';
                        row.style.opacity = '0';
                        (function(r) {
                            setTimeout(function() {
                                if (r.style.opacity === '0') r.style.display = 'none';
                            }, 150);
                        })(row);
                    }
                });
            }, 200));
        });

        
        var flash = document.getElementById('flashMessages');
        if (flash) {
            var sMsg = flash.getAttribute('data-success');
            var eMsg = flash.getAttribute('data-error');
            if (sMsg && typeof showToast === 'function') showToast(sMsg, 'success');
            if (eMsg && typeof showToast === 'function') showToast(eMsg, 'error');
        }

        
        try {
            var toastData = sessionStorage.getItem('promoToast');
            if (toastData) {
                sessionStorage.removeItem('promoToast');
                var parsed = JSON.parse(toastData);
                if (parsed && parsed.msg && typeof showToast === 'function') {
                    showToast(parsed.msg, parsed.tipo || 'success');
                }
            }
        } catch(e) {}

        

        var pendientesData = [];
        var pendientesPageActual = 0;
        var tbody = document.getElementById('pendientes-tbody');
        var loading = document.getElementById('pendientes-loading');
        var tablaWrap = document.getElementById('pendientes-tabla-wrap');
        var vacio = document.getElementById('pendientes-vacio');
        var footer = document.getElementById('pendientes-footer');
        var textoSeleccion = document.getElementById('pendientes-seleccion-texto');
        var btnConfirmar = document.getElementById('btn-confirmar-envio-pend');
        var checkMaestro = document.getElementById('check-maestro-pend');
        var countBadge = document.getElementById('pendientes-count');

        var pendientesSearchActual = '';

        function cargarPendientes(pagina, busqueda) {
            if (!tbody || !loading) return;
            pendientesCargados = true;
            if (pagina === undefined) pagina = 0;
            if (busqueda !== undefined) pendientesSearchActual = busqueda;
            pendientesPageActual = pagina;

            loading.style.display = 'flex';
            tablaWrap.style.display = 'none';
            vacio.style.display = 'none';
            footer.style.display = 'none';

            var url = '/api/notificaciones/pendientes?page=' + pagina;
            if (pendientesSearchActual) url += '&search=' + encodeURIComponent(pendientesSearchActual);

            SionUtils.fetchSeguro(url)
                .then(function(res) { return res.json(); })
                .then(function(pageData) {
                    pendientesData = pageData.content || [];
                    renderizarPendientes(pendientesData);
                    renderizarPaginacionPendientes(pageData);
                    if (countBadge) countBadge.textContent = pageData.totalElements || 0;
                })
                .catch(function(err) {
                    if (typeof showToast === 'function') showToast('Error al cargar usuarios pendientes', 'error');
                    vacio.style.display = 'block';
                })
                .finally(function() {
                    loading.style.display = 'none';
                });
        }

        function renderizarPendientes(datos) {
            tbody.innerHTML = '';

            if (!datos || datos.length === 0) {
                tablaWrap.style.display = 'none';
                vacio.style.display = 'block';
                footer.style.display = 'none';
                
                var pagVieja = document.getElementById('pendientes-paginacion');
                if (pagVieja) pagVieja.remove();
                return;
            }

            datos.forEach(function(cliente) {
                var tr = document.createElement('tr');
                tr.className = 'data-row data-row-pendientes';
                tr.dataset.id = cliente.id;

                var fechaTexto = '—';
                if (cliente.fechaCreacion) {
                    var partes = cliente.fechaCreacion.split('-');
                    if (partes.length === 3) {
                        fechaTexto = partes[2] + '/' + partes[1] + '/' + partes[0];
                    }
                }

                tr.innerHTML =
                    '<td>' +
                        '<input type="checkbox" class="cliente-check pend-check" data-id="' + cliente.id + '">' +
                    '</td>' +
                    '<td class="text-left pend-cell-nombre">' + escapeHtml(cliente.nombre) + '</td>' +
                    '<td class="text-left pend-cell-cedula">' + escapeHtml(cliente.cedula || '—') + '</td>' +
                    '<td class="text-left pend-cell-telefono">' + escapeHtml(cliente.telefono || '—') + '</td>' +
                    '<td class="text-left pend-cell-fecha">' + escapeHtml(fechaTexto) + '</td>';

                tbody.appendChild(tr);
            });

            tablaWrap.style.display = 'block';
            vacio.style.display = 'none';
            footer.style.display = 'flex';
            actualizarContador();
        }

        function renderizarPaginacionPendientes(pageData) {
            var existente = document.getElementById('pendientes-paginacion');
            if (existente) existente.remove();

            if (pageData.totalPages <= 1) return;

            var current = pageData.number;
            var total = pageData.totalPages;
            var totalElements = pageData.totalElements;
            var size = pageData.size;

            var container = document.createElement('div');
            container.id = 'pendientes-paginacion';
            container.className = 'table-pagination';

            
            var info = document.createElement('div');
            info.className = 'pagination-info';
            var desde = (current * size) + 1;
            var hasta = Math.min((current + 1) * size, totalElements);
            info.innerHTML = 'Mostrando <strong>' + desde + '</strong> a <strong>' + hasta + '</strong> de <strong>' + totalElements + '</strong>';
            container.appendChild(info);

            
            var controls = document.createElement('div');
            controls.className = 'pagination-controls';

            
            controls.appendChild(crearBtnPag('«', 0, current === 0));
            
            controls.appendChild(crearBtnPag('‹', current - 1, current === 0));

            
            var start = Math.max(0, current - 2);
            var end = Math.min(total - 1, start + 4);
            start = Math.max(0, end - 4);

            if (start > 0) {
                var dots = document.createElement('span');
                dots.className = 'pagination-btn disabled';
                dots.textContent = '...';
                controls.appendChild(dots);
            }

            for (var i = start; i <= end; i++) {
                var btn = crearBtnPag(String(i + 1), i, false);
                if (i === current) btn.classList.add('active');
                controls.appendChild(btn);
            }

            if (end < total - 1) {
                var dots2 = document.createElement('span');
                dots2.className = 'pagination-btn disabled';
                dots2.textContent = '...';
                controls.appendChild(dots2);
            }

            
            controls.appendChild(crearBtnPag('›', current + 1, current >= total - 1));
            
            controls.appendChild(crearBtnPag('»', total - 1, current >= total - 1));

            container.appendChild(controls);

            
            tablaWrap.parentNode.insertBefore(container, tablaWrap.nextSibling);
        }

        function crearBtnPag(texto, pagina, deshabilitado) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'pagination-btn' + (deshabilitado ? ' disabled' : '');
            btn.textContent = texto;
            if (!deshabilitado) {
                btn.addEventListener('click', function() {
                    cargarPendientes(pagina);
                });
            }
            return btn;
        }

        function escapeHtml(text) {
            if (!text) return '';
            var div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        function escapeAttr(text) {
            if (!text) return '';
            return text.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }

        function filtrarPendientes(term) {
            var rows = document.querySelectorAll('.data-row-pendientes');
            rows.forEach(function(row) {
                var visible = row.textContent.toLowerCase().indexOf(term) !== -1;
                if (visible && row.style.display === 'none') {
                    row.style.display = '';
                    row.style.opacity = '0';
                    requestAnimationFrame(function() {
                        row.style.transition = 'opacity 0.2s ease';
                        row.style.opacity = '1';
                    });
                } else if (!visible && row.style.display !== 'none') {
                    row.style.transition = 'opacity 0.15s ease';
                    row.style.opacity = '0';
                    (function(r) {
                        setTimeout(function() {
                            if (r.style.opacity === '0') r.style.display = 'none';
                        }, 150);
                    })(row);
                }
            });
        }

        function obtenerSeleccionados() {
            var checks = document.querySelectorAll('.pend-check:checked');
            var ids = [];
            checks.forEach(function(c) { ids.push(parseInt(c.dataset.id, 10)); });
            return ids;
        }

        function actualizarContador() {
            var total = obtenerSeleccionados().length;
            if (textoSeleccion) {
                textoSeleccion.textContent = total + ' usuario' + (total !== 1 ? 's' : '') + ' seleccionado' + (total !== 1 ? 's' : '');
            }
            if (btnConfirmar) {
                btnConfirmar.disabled = total === 0;
            }
            // Sincronizar check maestro
            var allChecks = document.querySelectorAll('.pend-check');
            if (checkMaestro && allChecks.length > 0) {
                checkMaestro.checked = total === allChecks.length;
                checkMaestro.indeterminate = total > 0 && total < allChecks.length;
            }
        }

        // Delegación de eventos para checkboxes
        if (tbody) {
            tbody.addEventListener('change', function(e) {
                if (e.target.classList.contains('pend-check')) {
                    actualizarContador();
                }
            });
        }

        // Check maestro
        if (checkMaestro) {
            checkMaestro.addEventListener('change', function() {
                var checks = document.querySelectorAll('.pend-check');
                var checked = checkMaestro.checked;
                checks.forEach(function(c) { c.checked = checked; });
                actualizarContador();
            });
        }

        // Botón Todos
        var btnTodos = document.getElementById('btn-seleccionar-todos-pend');
        if (btnTodos) {
            btnTodos.addEventListener('click', function() {
                document.querySelectorAll('.pend-check').forEach(function(c) { c.checked = true; });
                actualizarContador();
            });
        }

        // Botón Ninguno
        var btnNinguno = document.getElementById('btn-deseleccionar-todos-pend');
        if (btnNinguno) {
            btnNinguno.addEventListener('click', function() {
                document.querySelectorAll('.pend-check').forEach(function(c) { c.checked = false; });
                actualizarContador();
            });
        }

        /* ── Modal de confirmación ── */
        var overlayConsent = document.getElementById('modalConsentimientoOverlay');
        var modalCantidad = document.getElementById('modal-consent-cantidad');
        var btnEnviarConsent = document.getElementById('btn-enviar-consent');
        var btnCancelarConsent = document.getElementById('btn-cancelar-consent');
        var btnCerrarConsent = document.getElementById('btn-cerrar-modal-consent');

        if (overlayConsent) {
            document.body.appendChild(overlayConsent);
        }

        // Abrir modal
        if (btnConfirmar) {
            btnConfirmar.addEventListener('click', function() {
                var seleccionados = obtenerSeleccionados();
                if (seleccionados.length === 0) return;

                if (modalCantidad) modalCantidad.textContent = seleccionados.length;
                if (overlayConsent) overlayConsent.classList.add('open');
            });
        }

        function cerrarModalConsent() {
            if (overlayConsent) overlayConsent.classList.remove('open');
        }

        if (btnCancelarConsent) btnCancelarConsent.addEventListener('click', cerrarModalConsent);
        if (btnCerrarConsent) btnCerrarConsent.addEventListener('click', cerrarModalConsent);
        if (overlayConsent) {
            overlayConsent.addEventListener('click', function(e) {
                if (e.target === overlayConsent) cerrarModalConsent();
            });
        }

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && overlayConsent && overlayConsent.classList.contains('open')) {
                cerrarModalConsent();
            }
        });

        // Enviar consentimiento masivo
        var enviarHTMLOriginal = btnEnviarConsent ? btnEnviarConsent.innerHTML : '';

        if (btnEnviarConsent) {
            btnEnviarConsent.addEventListener('click', function() {
                var ids = obtenerSeleccionados();
                if (ids.length === 0) return;

                btnEnviarConsent.disabled = true;
                btnEnviarConsent.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation:spin .8s linear infinite"><path d="M21 12a9 9 0 1 1-6.219-8.56"></path></svg> Enviando...';

                SionUtils.fetchSeguro('/api/notificaciones/consentimiento-masivo', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(ids)
                })
                .then(function(res) {
                    if (!res.ok) throw new Error('Error en el servidor');
                    return res.json();
                })
                .then(function(resultado) {
                    cerrarModalConsent();

                    var enviados = resultado.enviados || 0;
                    var fallidos = resultado.fallidos || 0;
                    var sinTelefono = resultado.sinTelefono || 0;
                    var total = resultado.totalProcesados || 0;

                    var msg = 'Consentimiento enviado — ' + enviados + ' de ' + total + ' procesados.';
                    if (fallidos > 0) msg += ' Fallidos: ' + fallidos + '.';
                    if (sinTelefono > 0) msg += ' Sin teléfono: ' + sinTelefono + '.';

                    if (typeof showToast === 'function') {
                        showToast(msg, enviados > 0 ? 'success' : 'error');
                    }

                    // Recargar la lista de pendientes
                    pendientesCargados = false;
                    cargarPendientes();
                })
                .catch(function(err) {
                    if (typeof showToast === 'function') showToast('Error al enviar consentimientos: ' + err.message, 'error');
                })
                .finally(function() {
                    btnEnviarConsent.disabled = false;
                    btnEnviarConsent.innerHTML = enviarHTMLOriginal;
                });
            });
        }

        /* ══════════════════════════════════════════
           MODAL: CAMBIAR FECHA DE ENVÍO DE RECORDATORIO
           ══════════════════════════════════════════ */
        var overlay     = document.getElementById('modalFechaOverlay');
        var inputFecha  = document.getElementById('inputNuevaFecha');
        var btnGuardar  = document.getElementById('btnGuardarFecha');
        var btnCancelar = document.getElementById('btnCancelarFecha');
        var btnCerrar   = document.getElementById('btnCerrarModalFecha');
        var alerta      = document.getElementById('alertaFechaExito');
        var toggleCal   = document.getElementById('mfechaToggleCal');
        var calWrap     = document.getElementById('mfechaCalendario');

        // Elementos del calendario
        var elMes   = document.getElementById('mfechaMes');
        var elAnio  = document.getElementById('mfechaAnio');
        var elDias  = document.getElementById('mfechaDias');
        var btnPrev = document.getElementById('mfechaPrev');
        var btnNext = document.getElementById('mfechaNext');
        var btnHoy  = document.getElementById('mfechaBtnHoy');

        if (!overlay || !btnGuardar || !inputFecha) return;

        // Mover al body para evitar problemas de stacking context
        document.body.appendChild(overlay);

        var MESES = [
            'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
            'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
        ];

        var notifIdActual  = null;
        var celdaActual    = null;
        var mesVisible     = new Date().getMonth();
        var anioVisible    = new Date().getFullYear();
        var fechaSeleccion = null;

        /* ── Formato automático del input dd/mm/aaaa ── */
        inputFecha.addEventListener('input', function() {
            var v = inputFecha.value.replace(/\D/g, '');
            if (v.length > 8) v = v.slice(0, 8);
            var formateado = '';
            if (v.length > 0) formateado += v.slice(0, 2);
            if (v.length > 2) formateado += '/' + v.slice(2, 4);
            if (v.length > 4) formateado += '/' + v.slice(4, 8);
            inputFecha.value = formateado;

            // Sincronizar con el calendario si la fecha es completa
            if (v.length === 8) {
                var d = parseInt(v.slice(0, 2), 10);
                var m = parseInt(v.slice(2, 4), 10) - 1;
                var a = parseInt(v.slice(4, 8), 10);
                var fecha = new Date(a, m, d);
                if (fecha.getDate() === d && fecha.getMonth() === m && fecha.getFullYear() === a) {
                    fechaSeleccion = fecha;
                    mesVisible  = m;
                    anioVisible = a;
                    if (calWrap.classList.contains('abierto')) renderizarMes();
                }
            }
        });

        /* ── Sincronizar input de texto con la fecha seleccionada ── */
        function sincronizarInput() {
            if (!fechaSeleccion) {
                inputFecha.value = '';
                return;
            }
            var d = String(fechaSeleccion.getDate()).padStart(2, '0');
            var m = String(fechaSeleccion.getMonth() + 1).padStart(2, '0');
            var a = fechaSeleccion.getFullYear();
            inputFecha.value = d + '/' + m + '/' + a;
        }

        function obtenerFechaISO() {
            if (!fechaSeleccion) return '';
            var a = fechaSeleccion.getFullYear();
            var m = String(fechaSeleccion.getMonth() + 1).padStart(2, '0');
            var d = String(fechaSeleccion.getDate()).padStart(2, '0');
            return a + '-' + m + '-' + d;
        }

        /* ── Toggle del calendario lateral ── */
        var modalEl = overlay.querySelector('.mfecha-modal');

        toggleCal.addEventListener('click', function() {
            var abierto = modalEl.classList.toggle('cal-abierto');
            toggleCal.classList.toggle('activo', abierto);
            if (abierto) renderizarMes();
        });

        /* ── Renderizado del calendario ── */
        function renderizarMes() {
            elMes.textContent  = MESES[mesVisible];
            elAnio.textContent = anioVisible;
            elDias.innerHTML   = '';

            var primerDia = new Date(anioVisible, mesVisible, 1);
            var ultimoDia = new Date(anioVisible, mesVisible + 1, 0);
            var inicioSem = (primerDia.getDay() + 6) % 7;

            var hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            // Días del mes anterior
            var diasMesAnt = new Date(anioVisible, mesVisible, 0).getDate();
            for (var i = inicioSem - 1; i >= 0; i--) {
                elDias.appendChild(crearDia(diasMesAnt - i, true));
            }

            // Días del mes actual
            for (var d = 1; d <= ultimoDia.getDate(); d++) {
                var fecha = new Date(anioVisible, mesVisible, d);
                var btn = crearDia(d, false);

                if (fecha.getTime() === hoy.getTime()) btn.classList.add('hoy');

                if (fechaSeleccion &&
                    fecha.getFullYear() === fechaSeleccion.getFullYear() &&
                    fecha.getMonth()    === fechaSeleccion.getMonth() &&
                    fecha.getDate()     === fechaSeleccion.getDate()) {
                    btn.classList.add('seleccionado');
                }

                (function(f) {
                    btn.addEventListener('click', function() {
                        fechaSeleccion = f;
                        sincronizarInput();
                        renderizarMes();
                    });
                })(fecha);

                elDias.appendChild(btn);
            }

            // Días del mes siguiente
            var total = inicioSem + ultimoDia.getDate();
            var rest  = total % 7 === 0 ? 0 : 7 - (total % 7);
            for (var x = 1; x <= rest; x++) {
                elDias.appendChild(crearDia(x, true));
            }
        }

        function crearDia(numero, esOtroMes) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'mfecha-dia' + (esOtroMes ? ' otro-mes' : '');
            btn.textContent = numero;
            if (esOtroMes) btn.disabled = true;
            return btn;
        }

        // Navegación de meses
        btnPrev.addEventListener('click', function() {
            mesVisible--;
            if (mesVisible < 0) { mesVisible = 11; anioVisible--; }
            renderizarMes();
        });
        btnNext.addEventListener('click', function() {
            mesVisible++;
            if (mesVisible > 11) { mesVisible = 0; anioVisible++; }
            renderizarMes();
        });

        // Botón "Hoy"
        btnHoy.addEventListener('click', function() {
            var hoy = new Date();
            fechaSeleccion = hoy;
            mesVisible  = hoy.getMonth();
            anioVisible = hoy.getFullYear();
            sincronizarInput();
            renderizarMes();
        });

        /* ── Abrir modal desde la tabla ── */
        document.addEventListener('click', function(e) {
            var btn = e.target.closest('.btn-editar-fecha');
            if (!btn) return;

            notifIdActual = btn.getAttribute('data-id');
            celdaActual   = btn.closest('td').querySelector('.notif-fecha-texto');

            // Parsear fecha actual del recordatorio
            var fechaStr = btn.getAttribute('data-fecha') || '';
            if (fechaStr) {
                var p = fechaStr.split('-');
                fechaSeleccion = new Date(parseInt(p[0]), parseInt(p[1]) - 1, parseInt(p[2]));
                mesVisible  = fechaSeleccion.getMonth();
                anioVisible = fechaSeleccion.getFullYear();
            } else {
                fechaSeleccion = null;
                mesVisible  = new Date().getMonth();
                anioVisible = new Date().getFullYear();
            }

            sincronizarInput();

            // Cerrar calendario y resetear estado visual
            modalEl.classList.remove('cal-abierto');
            toggleCal.classList.remove('activo');
            if (alerta) alerta.classList.remove('visible');

            overlay.classList.add('open');
            setTimeout(function() { inputFecha.focus(); }, 80);
        });

        /* ── Cerrar modal ── */
        function cerrarModal() {
            overlay.classList.remove('open');
            modalEl.classList.remove('cal-abierto');
            toggleCal.classList.remove('activo');
            notifIdActual = null;
            celdaActual   = null;
        }

        if (btnCerrar)   btnCerrar.addEventListener('click', cerrarModal);
        if (btnCancelar)  btnCancelar.addEventListener('click', cerrarModal);

        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) cerrarModal();
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && overlay.classList.contains('open')) cerrarModal();
        });

        /* ── Guardar fecha ── */
        var guardarHTML = btnGuardar.innerHTML;

        btnGuardar.addEventListener('click', function() {
            // Parsear desde el input si no hay selección del calendario
            if (!fechaSeleccion && inputFecha.value.length === 10) {
                var partes = inputFecha.value.split('/');
                var d = parseInt(partes[0], 10);
                var m = parseInt(partes[1], 10) - 1;
                var a = parseInt(partes[2], 10);
                var f = new Date(a, m, d);
                if (f.getDate() === d && f.getMonth() === m) fechaSeleccion = f;
            }

            var fecha = obtenerFechaISO();
            if (!fecha || !notifIdActual) return;

            btnGuardar.disabled = true;
            btnGuardar.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation:spin .8s linear infinite"><path d="M21 12a9 9 0 1 1-6.219-8.56"></path></svg> Guardando...';

            SionUtils.fetchSeguro('/api/promociones/recordatorio/' + notifIdActual + '/fecha', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ fecha: fecha })
            })
            .then(function(res) {
                if (!res.ok) return res.json().then(function(d) { throw new Error(d.mensaje || 'Error al guardar'); });
                return res.json();
            })
            .then(function() {
                // Actualizar celda visible en la tabla
                if (celdaActual) {
                    var p = fecha.split('-');
                    celdaActual.textContent = p[2] + '/' + p[1] + '/' + p[0];
                }
                // Actualizar data-fecha del botón de edición
                var btnEditar = document.querySelector('.btn-editar-fecha[data-id="' + notifIdActual + '"]');
                if (btnEditar) btnEditar.setAttribute('data-fecha', fecha);

                // Mostrar alerta de éxito
                if (alerta) {
                    alerta.classList.add('visible');
                    setTimeout(function() {
                        alerta.classList.remove('visible');
                        cerrarModal();
                    }, 1500);
                } else {
                    cerrarModal();
                }

                if (typeof showToast === 'function') showToast('Fecha actualizada correctamente', 'success');
            })
            .catch(function(err) {
                if (typeof showToast === 'function') showToast(err.message, 'error');
            })
            .finally(function() {
                btnGuardar.disabled = false;
                btnGuardar.innerHTML = guardarHTML;
            });
        });
    });
})();
