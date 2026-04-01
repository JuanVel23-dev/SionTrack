/**
 * SionTrack — Vehículo Form
 * Validación de placa colombiana y restricción de kilometraje.
 *
 * Formatos de placa válidos en Colombia:
 *   - Vehículos: 3 letras + 3 números        → ABC-123
 *   - Motos:     3 letras + 2 números + letra → ABC-12D
 */
(function() {
    'use strict';

    // Regex: 3 letras, guion opcional, 3 números  O  3 letras, guion opcional, 2 números + 1 letra
    var PLACA_REGEX = /^[A-Z]{3}-?(\d{3}|\d{2}[A-Z])$/;

    document.addEventListener('DOMContentLoaded', function() {
        var inputPlaca = document.getElementById('placa');
        var inputKm    = document.getElementById('kilometraje_actual');
        var form       = inputPlaca ? inputPlaca.closest('form') : null;

        if (!inputPlaca || !inputKm) return;

        /* ══════════════════════════════════════
           PLACA: formateo automático y validación
           ══════════════════════════════════════ */

        // Formateo en tiempo real: convierte a mayúscula, elimina caracteres no válidos,
        // inserta guion automáticamente después de las 3 letras
        inputPlaca.addEventListener('input', function() {
            var cursor = this.selectionStart;
            var raw = this.value.toUpperCase().replace(/[^A-Z0-9]/g, '');

            // Máximo 6 caracteres alfanuméricos (3 letras + 3 dígitos/letra)
            if (raw.length > 6) raw = raw.slice(0, 6);

            // Separar la parte de letras (máx 3) y la parte numérica
            var letras = '';
            var resto  = '';

            for (var i = 0; i < raw.length; i++) {
                if (i < 3) {
                    // Los primeros 3 caracteres deben ser letras
                    if (/[A-Z]/.test(raw[i])) {
                        letras += raw[i];
                    }
                } else {
                    resto += raw[i];
                }
            }

            // Construir valor formateado con guion
            var formateado = letras;
            if (letras.length === 3 && resto.length > 0) {
                formateado = letras + '-' + resto;
            }

            this.value = formateado;

            // Ajustar cursor
            if (cursor <= 3) {
                this.setSelectionRange(Math.min(cursor, formateado.length), Math.min(cursor, formateado.length));
            } else {
                // Compensar el guion insertado
                var nuevoCursor = Math.min(cursor + (formateado.length - (raw.length > 3 ? raw.length + 0 : raw.length)), formateado.length);
                this.setSelectionRange(nuevoCursor, nuevoCursor);
            }

            // Validación visual en tiempo real
            validarPlacaVisual(formateado);
        });

        // Prevenir caracteres no permitidos desde el teclado
        inputPlaca.addEventListener('keydown', function(e) {
            // Permitir teclas de control
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            if (['Backspace','Delete','Tab','Escape','Enter','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'].indexOf(e.key) !== -1) return;

            var raw = this.value.replace(/[^A-Z0-9]/gi, '');

            // Si ya tiene 6 caracteres, bloquear más entrada
            if (raw.length >= 6 && this.selectionStart === this.selectionEnd) {
                e.preventDefault();
                return;
            }

            // Solo permitir letras y números
            if (!/^[a-zA-Z0-9]$/.test(e.key)) {
                e.preventDefault();
            }
        });

        // Al perder foco, validar y mostrar feedback
        inputPlaca.addEventListener('blur', function() {
            var valor = this.value.trim();
            if (!valor) {
                limpiarEstadoPlaca();
                return;
            }
            validarPlacaVisual(valor);
        });

        // Configurar maxlength del input
        inputPlaca.setAttribute('maxlength', '7');

        function validarPlacaVisual(valor) {
            var helpEl = inputPlaca.parentElement.querySelector('.form-help-placa');

            if (!valor) {
                limpiarEstadoPlaca();
                return;
            }

            var esValida = PLACA_REGEX.test(valor);
            var raw = valor.replace(/[^A-Z0-9]/g, '');

            if (esValida) {
                inputPlaca.classList.remove('error');
                inputPlaca.classList.add('success');
                if (helpEl) {
                    helpEl.textContent = 'Placa válida';
                    helpEl.className = 'form-help-placa placa-valida';
                }
            } else if (raw.length >= 4) {
                // Solo mostrar error si el usuario ya escribió suficiente
                inputPlaca.classList.add('error');
                inputPlaca.classList.remove('success');
                if (helpEl) {
                    helpEl.textContent = 'Formato: ABC-123 o ABC-12D';
                    helpEl.className = 'form-help-placa placa-error';
                }
            } else {
                inputPlaca.classList.remove('error', 'success');
                if (helpEl) {
                    helpEl.textContent = 'Formato: ABC-123 o ABC-12D';
                    helpEl.className = 'form-help-placa';
                }
            }
        }

        function limpiarEstadoPlaca() {
            inputPlaca.classList.remove('error', 'success');
            var helpEl = inputPlaca.parentElement.querySelector('.form-help-placa');
            if (helpEl) {
                helpEl.textContent = 'Formato: ABC-123 o ABC-12D';
                helpEl.className = 'form-help-placa';
            }
        }

        /* ══════════════════════════════════════
           KILOMETRAJE: solo números enteros
           ══════════════════════════════════════ */

        // Cambiar tipo a text para control total del input
        inputKm.setAttribute('type', 'text');
        inputKm.setAttribute('inputmode', 'numeric');
        inputKm.setAttribute('pattern', '[0-9]*');

        inputKm.addEventListener('input', function() {
            // Eliminar todo lo que no sea dígito
            var limpio = this.value.replace(/\D/g, '');
            // Formatear con separador de miles
            if (limpio) {
                var num = parseInt(limpio, 10);
                this.value = num.toLocaleString('es-CO');
            } else {
                this.value = '';
            }
        });

        inputKm.addEventListener('keydown', function(e) {
            // Permitir teclas de control
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            if (['Backspace','Delete','Tab','Escape','Enter','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'].indexOf(e.key) !== -1) return;

            // Solo permitir dígitos
            if (!/^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });

        // Prevenir pegado de texto no numérico
        inputKm.addEventListener('paste', function(e) {
            var pegado = (e.clipboardData || window.clipboardData).getData('text');
            if (/[^\d]/.test(pegado.replace(/[.,\s]/g, ''))) {
                e.preventDefault();
            }
        });

        /* ══════════════════════════════════════
           VALIDACIÓN AL ENVIAR FORMULARIO
           ══════════════════════════════════════ */
        if (form) {
            form.addEventListener('submit', function(e) {
                var placa = inputPlaca.value.trim();
                var km = inputKm.value.trim();
                var errores = [];

                // Validar placa
                if (!placa) {
                    errores.push('La placa es requerida');
                    inputPlaca.classList.add('error');
                } else if (!PLACA_REGEX.test(placa)) {
                    errores.push('La placa no tiene un formato válido (ej: ABC-123 o ABC-12D)');
                    inputPlaca.classList.add('error');
                }

                // Validar kilometraje
                var kmLimpio = km.replace(/\D/g, '');
                if (!kmLimpio) {
                    errores.push('El kilometraje es requerido');
                    inputKm.classList.add('error');
                }

                if (errores.length) {
                    e.preventDefault();
                    if (typeof showToast === 'function') showToast(errores[0], 'error');
                    return;
                }

                // Enviar el kilometraje como número limpio (sin formato)
                inputKm.value = kmLimpio;
            });
        }
    });
})();
