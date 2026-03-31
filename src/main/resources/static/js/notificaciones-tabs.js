/**
 * SionTrack — Notificaciones Tabs
 * Sliding entre paneles, búsqueda por tab y modal de edición de fecha
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {

        /* ── Tabs: sliding entre paneles ── */
        var tabs = document.querySelectorAll('.ntab-btn');
        var track = document.getElementById('ntab-track');

        if (tabs.length && track) {
            tabs.forEach(function(tab, index) {
                tab.addEventListener('click', function() {
                    if (this.classList.contains('active')) return;

                    tabs.forEach(function(t) { t.classList.remove('active'); });
                    this.classList.add('active');

                    track.style.transform = 'translateX(-' + (index * 100) + '%)';
                });
            });
        }

        /* ── Búsqueda independiente por panel ── */
        document.querySelectorAll('.ntab-search').forEach(function(input) {
            input.addEventListener('input', SionUtils.debounce(function() {
                var target = input.dataset.target;
                var term = input.value.toLowerCase().trim();
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

        /* ── Mensajes flash (success/error) ── */
        var flash = document.getElementById('flashMessages');
        if (flash) {
            var sMsg = flash.getAttribute('data-success');
            var eMsg = flash.getAttribute('data-error');
            if (sMsg && typeof showToast === 'function') showToast(sMsg, 'success');
            if (eMsg && typeof showToast === 'function') showToast(eMsg, 'error');
        }

        /* ── Modal: cambiar fecha de envío de recordatorio ── */
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

            fetch('/api/promociones/recordatorio/' + notifIdActual + '/fecha', {
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