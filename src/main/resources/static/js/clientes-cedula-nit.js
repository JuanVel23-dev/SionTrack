/**
 * SionTrack - Cédula / NIT dinámico
 *
 * Reglas Colombia:
 * - Persona Natural: Cédula de ciudadanía, 6-10 dígitos, solo números, SIN guión.
 * - Empresa (Persona Jurídica): NIT = 9 dígitos + guión + dígito de verificación (DV).
 *   El DV es un solo dígito del 0 al 9, asignado por la DIAN.
 *   Formato final: 900123456-7
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var tipoHidden   = document.getElementById('tipo_cliente');
        var campoCedula   = document.getElementById('campo-cedula');
        var campoNit      = document.getElementById('campo-nit');
        var cedulaInput   = document.getElementById('cedula_input');
        var nitNumero     = document.getElementById('nit_numero');
        var nitDv         = document.getElementById('nit_dv');
        var hiddenField   = document.getElementById('cedula_ruc');
        var label         = document.getElementById('cedula-nit-label');

        if (!tipoHidden || !campoCedula || !campoNit || !hiddenField) return;

        // =============================================
        // CAMBIAR ENTRE CÉDULA Y NIT
        // =============================================
        function actualizarCampo() {
            var tipo = tipoHidden.value;

            if (tipo === 'Empresa') {
                campoCedula.style.display = 'none';
                campoNit.style.display = '';
                label.textContent = 'NIT';

                // Si hay valor existente con guión, parsear
                var val = hiddenField.value || '';
                if (val.indexOf('-') !== -1) {
                    var partes = val.split('-');
                    nitNumero.value = partes[0];
                    nitDv.value = partes[1] || '';
                } else if (val.length >= 9 && /^\d+$/.test(val)) {
                    nitNumero.value = val.substring(0, 9);
                    nitDv.value = '';
                } else if (val) {
                    nitNumero.value = '';
                    nitDv.value = '';
                }

            } else {
                campoCedula.style.display = '';
                campoNit.style.display = 'none';
                label.textContent = 'Cédula de Ciudadanía';

                var val = hiddenField.value || '';
                if (val.indexOf('-') === -1) {
                    cedulaInput.value = val;
                } else {
                    cedulaInput.value = '';
                }
            }

            sincronizarHidden();
        }

        // =============================================
        // SINCRONIZAR VALOR AL HIDDEN
        // =============================================
        function sincronizarHidden() {
            var tipo = tipoHidden.value;
            if (tipo === 'Empresa') {
                var num = nitNumero.value.replace(/[^0-9]/g, '');
                var dv = nitDv.value.replace(/[^0-9]/g, '');
                if (num && dv) {
                    hiddenField.value = num + '-' + dv;
                } else {
                    hiddenField.value = num;
                }
            } else {
                hiddenField.value = cedulaInput.value.replace(/[^0-9]/g, '');
            }
        }

        // =============================================
        // CÉDULA: solo números, 6-10 dígitos
        // =============================================
        cedulaInput.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 10) this.value = this.value.substring(0, 10);
            this.classList.remove('error');
            sincronizarHidden();
        });

        cedulaInput.addEventListener('keypress', function(e) {
            if (!/[0-9]/.test(String.fromCharCode(e.which))) e.preventDefault();
        });

        // =============================================
        // NIT: 9 dígitos
        // =============================================
        nitNumero.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 9) this.value = this.value.substring(0, 9);
            this.classList.remove('error');
            sincronizarHidden();
        });

        nitNumero.addEventListener('keypress', function(e) {
            if (!/[0-9]/.test(String.fromCharCode(e.which))) e.preventDefault();
        });

        // Auto-focus al DV cuando se completan los 9 dígitos
        nitNumero.addEventListener('input', function() {
            if (this.value.length === 9 && nitDv) {
                nitDv.focus();
                nitDv.select();
            }
        });

        // =============================================
        // DV: un solo dígito 0-9
        // =============================================
        nitDv.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 1) this.value = this.value.substring(0, 1);
            this.classList.remove('error');
            sincronizarHidden();
        });

        nitDv.addEventListener('keypress', function(e) {
            if (!/[0-9]/.test(String.fromCharCode(e.which))) e.preventDefault();
            // Si ya tiene un dígito, reemplazar
            if (this.value.length >= 1) {
                this.value = '';
            }
        });

        // Backspace en DV vacío → volver al NIT
        nitDv.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && this.value === '') {
                e.preventDefault();
                nitNumero.focus();
                // Poner cursor al final
                var len = nitNumero.value.length;
                nitNumero.setSelectionRange(len, len);
            }
        });

        // =============================================
        // DETECTAR CAMBIO DE TIPO DE CLIENTE
        // =============================================
        var ultimoTipo = tipoHidden.value;
        setInterval(function() {
            if (tipoHidden.value !== ultimoTipo) {
                ultimoTipo = tipoHidden.value;
                actualizarCampo();
            }
        }, 200);

        tipoHidden.addEventListener('change', actualizarCampo);

        // =============================================
        // VALIDACIÓN AL SUBMIT
        // =============================================
        var form = document.getElementById('clienteForm');
        if (form) {
            form.addEventListener('submit', function(e) {
                var tipo = tipoHidden.value;

                if (!tipo) {
                    e.preventDefault();
                    if (typeof showToast === 'function') showToast('Selecciona el tipo de cliente', 'error');
                    return;
                }

                sincronizarHidden();

                if (tipo === 'Persona Natural') {
                    var cedula = cedulaInput.value.replace(/[^0-9]/g, '');
                    if (cedula.length < 6 || cedula.length > 10) {
                        e.preventDefault();
                        cedulaInput.classList.add('error');
                        if (typeof showToast === 'function') {
                            showToast('La cédula debe tener entre 6 y 10 dígitos', 'error');
                        }
                        return;
                    }
                    cedulaInput.classList.remove('error');
                }

                if (tipo === 'Empresa') {
                    var nit = nitNumero.value.replace(/[^0-9]/g, '');
                    var dv = nitDv.value.replace(/[^0-9]/g, '');

                    if (nit.length !== 9) {
                        e.preventDefault();
                        nitNumero.classList.add('error');
                        if (typeof showToast === 'function') {
                            showToast('El NIT debe tener exactamente 9 dígitos', 'error');
                        }
                        return;
                    }

                    if (dv.length !== 1) {
                        e.preventDefault();
                        nitDv.classList.add('error');
                        if (typeof showToast === 'function') {
                            showToast('Ingresa el dígito de verificación (0-9)', 'error');
                        }
                        return;
                    }

                    nitNumero.classList.remove('error');
                    nitDv.classList.remove('error');
                }
            });
        }

        // =============================================
        // INICIALIZACIÓN
        // =============================================
        if (tipoHidden.value) {
            actualizarCampo();
        } else {
            campoCedula.style.display = '';
            campoNit.style.display = 'none';
            if (hiddenField.value && hiddenField.value.indexOf('-') === -1) {
                cedulaInput.value = hiddenField.value;
            }
        }
    });
})();