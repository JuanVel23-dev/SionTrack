
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

        
        
        
        function actualizarCampo() {
            var tipo = tipoHidden.value;

            if (tipo === 'Empresa') {
                campoCedula.style.display = 'none';
                campoNit.style.display = '';
                label.textContent = 'NIT';

                
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

        
        
        
        cedulaInput.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 10) this.value = this.value.substring(0, 10);
            this.classList.remove('error');
            sincronizarHidden();
        });

        cedulaInput.addEventListener('keypress', function(e) {
            if (!/[0-9]/.test(String.fromCharCode(e.which))) e.preventDefault();
        });

        
        
        
        nitNumero.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 9) this.value = this.value.substring(0, 9);
            this.classList.remove('error');
            sincronizarHidden();
        });

        nitNumero.addEventListener('keypress', function(e) {
            if (!/[0-9]/.test(String.fromCharCode(e.which))) e.preventDefault();
        });

        
        nitNumero.addEventListener('input', function() {
            if (this.value.length === 9 && nitDv) {
                nitDv.focus();
                nitDv.select();
            }
        });

        
        
        
        nitDv.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 1) this.value = this.value.substring(0, 1);
            this.classList.remove('error');
            sincronizarHidden();
        });

        nitDv.addEventListener('keypress', function(e) {
            if (!/[0-9]/.test(String.fromCharCode(e.which))) e.preventDefault();
            
            if (this.value.length >= 1) {
                this.value = '';
            }
        });

        
        nitDv.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && this.value === '') {
                e.preventDefault();
                nitNumero.focus();
                
                var len = nitNumero.value.length;
                nitNumero.setSelectionRange(len, len);
            }
        });

        
        
        
        tipoHidden.addEventListener('change', actualizarCampo);

        
        var observer = new MutationObserver(function() {
            actualizarCampo();
        });
        observer.observe(tipoHidden, { attributes: true, attributeFilter: ['value'] });

        
        var descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
        Object.defineProperty(tipoHidden, 'value', {
            get: function() { return descriptor.get.call(this); },
            set: function(val) {
                descriptor.set.call(this, val);
                actualizarCampo();
            }
        });

        
        
        
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