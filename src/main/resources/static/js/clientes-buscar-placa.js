/**
 * SionTrack - Búsqueda de vehículos por placa
 * Permite buscar vehículos por placa y ver/eliminar desde un modal
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var btnAbrir = document.getElementById('btnBuscarPlaca');
        var overlay = document.getElementById('placa-modal-overlay');
        var btnCerrar = document.getElementById('placa-modal-close');
        var btnCerrarFooter = document.getElementById('placa-modal-close-btn');
        var inputPlaca = document.getElementById('placaSearchInput');
        var estadoInicial = document.getElementById('placa-initial-state');
        var resultados = document.getElementById('placa-results');
        var sinResultados = document.getElementById('placa-no-results');
        var cargando = document.getElementById('placa-loading');
        var contadorResultados = document.getElementById('placa-result-count');

        if (!btnAbrir || !overlay) return;

        var estadoActual = 'inicial';
        var transicionEnCurso = false;

        // Abrir modal
        btnAbrir.addEventListener('click', function() {
            overlay.classList.add('open');
            setTimeout(function() {
                inputPlaca.focus();
            }, 350);
        });

        // Cerrar modal
        function cerrarModal() {
            overlay.classList.remove('open');
            setTimeout(function() {
                inputPlaca.value = '';
                mostrarEstadoInmediato('inicial');
                contadorResultados.textContent = 'Ingresa una placa para buscar';
            }, 350);
        }

        btnCerrar.addEventListener('click', cerrarModal);
        btnCerrarFooter.addEventListener('click', cerrarModal);

        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) cerrarModal();
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && overlay.classList.contains('open')) {
                cerrarModal();
            }
        });

        // Mapa de secciones
        function obtenerElemento(nombre) {
            switch(nombre) {
                case 'inicial': return estadoInicial;
                case 'resultados': return resultados;
                case 'vacio': return sinResultados;
                case 'cargando': return cargando;
            }
        }

        // Mostrar estado sin animación (para reset)
        function mostrarEstadoInmediato(nombre) {
            transicionEnCurso = false;
            estadoActual = nombre;
            ['inicial', 'resultados', 'vacio', 'cargando'].forEach(function(n) {
                var el = obtenerElemento(n);
                el.style.transition = 'none';
                el.style.opacity = n === nombre ? '1' : '0';
                el.style.transform = 'none';
                el.style.display = n === nombre ? '' : 'none';
            });
        }

        // Transición suave entre estados
        function mostrarEstado(nombre) {
            if (nombre === estadoActual) return;

            var saliente = obtenerElemento(estadoActual);
            var entrante = obtenerElemento(nombre);
            var anterior = estadoActual;
            estadoActual = nombre;

            // Si hay una transición en curso, cortar de inmediato
            if (transicionEnCurso) {
                ['inicial', 'resultados', 'vacio', 'cargando'].forEach(function(n) {
                    var el = obtenerElemento(n);
                    el.style.transition = 'none';
                    el.style.opacity = '0';
                    el.style.display = 'none';
                    el.style.transform = 'none';
                });
            }

            transicionEnCurso = true;

            // Fade out del saliente
            if (saliente && saliente.style.display !== 'none') {
                saliente.style.transition = 'opacity 0.15s ease';
                saliente.style.opacity = '0';
            }

            // Esperar a que salga, luego mostrar entrante
            setTimeout(function() {
                // Ocultar saliente
                if (saliente) saliente.style.display = 'none';

                // Verificar que sigue siendo el estado actual
                if (estadoActual !== nombre) {
                    transicionEnCurso = false;
                    return;
                }

                // Preparar entrante
                entrante.style.transition = 'none';
                entrante.style.opacity = '0';
                entrante.style.transform = 'translateY(6px)';
                entrante.style.display = '';

                // Forzar reflow
                void entrante.offsetHeight;

                // Animar entrada
                entrante.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
                entrante.style.opacity = '1';
                entrante.style.transform = 'translateY(0)';

                setTimeout(function() {
                    transicionEnCurso = false;
                }, 200);
            }, 160);
        }

        // Búsqueda sensible desde 1 carácter
        inputPlaca.addEventListener('input', SionUtils.debounce(function() {
            var placa = inputPlaca.value.trim();

            if (!placa) {
                mostrarEstado('inicial');
                contadorResultados.textContent = 'Ingresa una placa para buscar';
                return;
            }

            mostrarEstado('cargando');

            fetch('/api/vehiculos/buscar?placa=' + encodeURIComponent(placa))
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (!data.length) {
                        mostrarEstado('vacio');
                        contadorResultados.textContent = 'Sin resultados para "' + placa + '"';
                        return;
                    }

                    var texto = data.length === 1 ? '1 vehículo encontrado' : data.length + ' vehículos encontrados';
                    contadorResultados.textContent = texto;
                    renderizarResultados(data);
                    mostrarEstado('resultados');
                })
                .catch(function() {
                    mostrarEstado('vacio');
                    contadorResultados.textContent = 'Error en la búsqueda';
                });
        }, 250));

        // Renderizar lista de resultados
        function renderizarResultados(data) {
            var html = '<div class="placa-result-list">';

            data.forEach(function(v, idx) {
                html += '<div class="placa-result-card" data-id="' + v.vehiculo_id + '" style="animation-delay:' + (idx * 40) + 'ms">'
                    + '  <div class="placa-result-left">'
                    + '    <div class="placa-badge">' + escapeHtml(v.placa) + '</div>'
                    + '    <div class="placa-result-details">'
                    + '      <div class="placa-result-row">'
                    + '        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
                    + '        <span class="placa-result-name">' + escapeHtml(v.nombre_cliente) + '</span>'
                    + '      </div>'
                    + '      <div class="placa-result-row">'
                    + '        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="9" y1="4" x2="9" y2="10"/></svg>'
                    + '        <span class="placa-result-cedula">' + escapeHtml(v.cedula_cliente) + '</span>'
                    + '      </div>'
                    + '    </div>'
                    + '  </div>'
                    + '  <button type="button" class="placa-btn-delete" title="Eliminar vehículo" data-vehiculo-id="' + v.vehiculo_id + '" data-placa="' + escapeHtml(v.placa) + '">'
                    + '    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
                    + '      <polyline points="3 6 5 6 21 6"></polyline>'
                    + '      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>'
                    + '    </svg>'
                    + '  </button>'
                    + '</div>';
            });

            html += '</div>';
            resultados.innerHTML = html;
        }

        // Eliminar vehículo: cierra todo y recarga
        function eliminarVehiculo(vehiculoId) {
            SionUtils.fetchSeguro('/api/vehiculos/' + vehiculoId, { method: 'DELETE' })
                .then(function(res) {
                    if (res.ok) {
                        overlay.classList.remove('open');
                        setTimeout(function() {
                            SionTrack.showToast('Vehículo eliminado correctamente', 'success');
                            window.location.reload();
                        }, 350);
                    } else {
                        SionTrack.showToast('Error al eliminar el vehículo', 'error');
                    }
                })
                .catch(function() {
                    SionTrack.showToast('Error de conexión al eliminar el vehículo', 'error');
                });
        }

        // Delegación de eventos para eliminar
        resultados.addEventListener('click', function(e) {
            var btnDelete = e.target.closest('.placa-btn-delete');
            if (!btnDelete) return;

            var vehiculoId = btnDelete.getAttribute('data-vehiculo-id');
            var placaText = btnDelete.getAttribute('data-placa');

            // Cerrar panel antes de mostrar confirmación
            overlay.classList.remove('open');

            setTimeout(function() {
                confirmAction(
                    '¿Estás seguro de que deseas eliminar el vehículo con placa <strong>' + escapeHtml(placaText) + '</strong>?',
                    function() {
                        eliminarVehiculo(vehiculoId);
                    },
                    {
                        title: 'Eliminar Vehículo',
                        confirmText: 'Eliminar',
                        cancelText: 'Cancelar',
                        type: 'danger'
                    }
                );
            }, 300);
        });

        function escapeHtml(text) {
            var div = document.createElement('div');
            div.appendChild(document.createTextNode(text || ''));
            return div.innerHTML;
        }
    });
})();
