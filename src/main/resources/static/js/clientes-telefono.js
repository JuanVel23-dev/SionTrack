/**
 * SionTrack - Teléfono de Clientes
 * Gestión de teléfonos, direcciones y correos en el formulario de clientes
 *
 * Dependencias: selector-telefono.js, utilidades.js
 * NOTA: La validación de cédula/NIT se maneja en clientes-cedula-nit.js
 */
(function() {
    'use strict';

    // ============================================
    // SELECT PERSONALIZADO (tipo de cliente)
    // ============================================
    function initCustomSelect(selectId) {
        var container = document.getElementById(selectId);
        if (!container) return;

        var btn = container.querySelector('.custom-select-btn');
        var dropdown = container.querySelector('.custom-select-dropdown');
        var opciones = container.querySelectorAll('.custom-select-option');
        var hiddenInput = container.querySelector('input[type="hidden"]');
        var textoSpan = btn.querySelector('.select-texto');

        // Verificar valor inicial de Thymeleaf
        var valorInicial = hiddenInput.value;
        if (valorInicial) {
            opciones.forEach(function(op) {
                if (op.dataset.value === valorInicial) {
                    op.classList.add('seleccionado');
                    textoSpan.textContent = op.textContent;
                    textoSpan.classList.remove('placeholder');
                }
            });
        }

        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            document.querySelectorAll('.custom-select.abierto').forEach(function(s) {
                if (s !== container) {
                    s.classList.remove('abierto');
                    s.querySelector('.custom-select-btn').classList.remove('activo');
                }
            });

            var abierto = container.classList.toggle('abierto');
            btn.classList.toggle('activo', abierto);
        });

        opciones.forEach(function(opcion) {
            opcion.addEventListener('click', function() {
                if (this.classList.contains('disabled')) return;

                opciones.forEach(function(op) { op.classList.remove('seleccionado'); });
                this.classList.add('seleccionado');

                textoSpan.textContent = this.textContent;
                textoSpan.classList.remove('placeholder');
                hiddenInput.value = this.dataset.value;

                container.classList.remove('abierto');
                btn.classList.remove('activo');
            });
        });

        document.addEventListener('click', function(e) {
            if (!container.contains(e.target)) {
                container.classList.remove('abierto');
                btn.classList.remove('activo');
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && container.classList.contains('abierto')) {
                container.classList.remove('abierto');
                btn.classList.remove('activo');
            }
        });
    }

    window.initCustomSelect = initCustomSelect;

    // ============================================
    // INICIALIZACIÓN
    // ============================================
    document.addEventListener('DOMContentLoaded', function() {
        var telContainer = document.getElementById('telefonos-container');
        var dirContainer = document.getElementById('direcciones-container');
        var corContainer = document.getElementById('correos-container');

        var telIdx = telContainer ? telContainer.querySelectorAll('.telefono-fila').length : 0;
        var dirIdx = dirContainer ? dirContainer.querySelectorAll('.dynamic-field').length : 0;
        var corIdx = corContainer ? corContainer.querySelectorAll('.dynamic-field').length : 0;

        // Inicializar filas existentes con módulo compartido
        SelectorTelefono.inicializarFilas('.telefono-fila');

        // Eliminar campos (delegación de eventos)
        document.addEventListener('click', function(e) {
            var btn = e.target.closest('.btn-remove');
            if (btn) {
                e.preventDefault();
                var row = btn.closest('.telefono-fila') || btn.closest('.dynamic-field');
                if (row) {
                    row.style.opacity = '0';
                    row.style.transition = 'opacity 0.2s';
                    setTimeout(function() { row.remove(); }, 200);
                }
            }
        });

        // Añadir teléfono
        var btnTel = document.getElementById('add-telefono-btn');
        if (btnTel) {
            btnTel.addEventListener('click', function(e) {
                e.preventDefault();
                var nombreCampo = 'telefonos[' + telIdx + '].telefono';
                var fila = SelectorTelefono.crearFila(nombreCampo);
                telContainer.appendChild(fila);
                telIdx++;
                fila.querySelector('.numero-input').focus();
            });
        }

        // Añadir dirección
        var btnDir = document.getElementById('add-direccion-btn');
        if (btnDir) {
            btnDir.addEventListener('click', function(e) {
                e.preventDefault();
                var div = document.createElement('div');
                div.className = 'dynamic-field';
                div.innerHTML =
                    '<input type="text" name="direcciones[' + dirIdx + '].direccion" class="form-input" placeholder="Ej: Av. Principal 123" />' +
                    '<button type="button" class="btn btn-danger btn-sm btn-remove">' + SelectorTelefono.ICONO_ELIMINAR + '</button>';
                dirContainer.appendChild(div);
                dirIdx++;
                div.querySelector('input').focus();
            });
        }

        // Añadir correo
        var btnCor = document.getElementById('add-correo-btn');
        if (btnCor) {
            btnCor.addEventListener('click', function(e) {
                e.preventDefault();
                var div = document.createElement('div');
                div.className = 'dynamic-field';
                div.innerHTML =
                    '<input type="email" name="correos[' + corIdx + '].correo" class="form-input input-correo" placeholder="Ej: cliente@ejemplo.com" />' +
                    '<button type="button" class="btn btn-danger btn-sm btn-remove">' + SelectorTelefono.ICONO_ELIMINAR + '</button>';
                corContainer.appendChild(div);
                corIdx++;
                div.querySelector('input').focus();
            });
        }

        // Inicializar select de tipo de cliente
        initCustomSelect('tipo-cliente-select');

        // Validar correos
        document.addEventListener('blur', function(e) {
            if (e.target.classList.contains('input-correo')) {
                var val = e.target.value.trim();
                if (val && !val.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                    e.target.classList.add('error');
                    if (typeof showToast === 'function') {
                        showToast('Ingresa un correo electrónico válido', 'error');
                    }
                } else {
                    e.target.classList.remove('error');
                }
            }
        }, true);

        // Submit: formatear teléfonos y validar correos
        var form = document.getElementById('clienteForm');
        if (form) {
            form.addEventListener('submit', function(e) {
                var ok = SelectorTelefono.formatearParaSubmit('.telefono-fila');

                // Validar correos
                document.querySelectorAll('.input-correo').forEach(function(inp) {
                    var val = inp.value.trim();
                    if (val && !val.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                        ok = false;
                        inp.classList.add('error');
                    }
                });

                if (!ok) {
                    e.preventDefault();
                    if (typeof showToast === 'function') {
                        showToast('Por favor corrige los errores', 'error');
                    }
                }
            });
        }
    });
})();
