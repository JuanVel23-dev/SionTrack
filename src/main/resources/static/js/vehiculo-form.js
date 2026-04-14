
(function() {
    'use strict';

    
    var PLACA_REGEX = /^[A-Z]{3}-?(\d{3}|\d{2}[A-Z])$/;

    document.addEventListener('DOMContentLoaded', function() {
        var inputPlaca = document.getElementById('placa');
        var inputKm    = document.getElementById('kilometraje_actual');
        var form       = inputPlaca ? inputPlaca.closest('form') : null;

        if (!inputPlaca || !inputKm) return;

        

        
        
        inputPlaca.addEventListener('input', function() {
            var cursor = this.selectionStart;
            var raw = this.value.toUpperCase().replace(/[^A-Z0-9]/g, '');

            
            if (raw.length > 6) raw = raw.slice(0, 6);

            
            var letras = '';
            var resto  = '';

            for (var i = 0; i < raw.length; i++) {
                if (i < 3) {
                    
                    if (/[A-Z]/.test(raw[i])) {
                        letras += raw[i];
                    }
                } else {
                    resto += raw[i];
                }
            }

            
            var formateado = letras;
            if (letras.length === 3 && resto.length > 0) {
                formateado = letras + '-' + resto;
            }

            this.value = formateado;

            
            if (cursor <= 3) {
                this.setSelectionRange(Math.min(cursor, formateado.length), Math.min(cursor, formateado.length));
            } else {
                
                var nuevoCursor = Math.min(cursor + (formateado.length - (raw.length > 3 ? raw.length + 0 : raw.length)), formateado.length);
                this.setSelectionRange(nuevoCursor, nuevoCursor);
            }

            
            validarPlacaVisual(formateado);
        });

        
        inputPlaca.addEventListener('keydown', function(e) {
            
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            if (['Backspace','Delete','Tab','Escape','Enter','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'].indexOf(e.key) !== -1) return;

            var raw = this.value.replace(/[^A-Z0-9]/gi, '');

            
            if (raw.length >= 6 && this.selectionStart === this.selectionEnd) {
                e.preventDefault();
                return;
            }

            
            if (!/^[a-zA-Z0-9]$/.test(e.key)) {
                e.preventDefault();
            }
        });

        
        inputPlaca.addEventListener('blur', function() {
            var valor = this.value.trim();
            if (!valor) {
                limpiarEstadoPlaca();
                return;
            }
            validarPlacaVisual(valor);
        });

        
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

        

        
        inputKm.setAttribute('type', 'text');
        inputKm.setAttribute('inputmode', 'numeric');
        inputKm.setAttribute('pattern', '[0-9]*');

        inputKm.addEventListener('input', function() {
            
            var limpio = this.value.replace(/\D/g, '');
            this.value = limpio;
        });

        inputKm.addEventListener('keydown', function(e) {
            
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            if (['Backspace','Delete','Tab','Escape','Enter','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'].indexOf(e.key) !== -1) return;

            
            if (!/^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });

        
        inputKm.addEventListener('paste', function(e) {
            var pegado = (e.clipboardData || window.clipboardData).getData('text');
            if (/[^\d]/.test(pegado.replace(/[.,\s]/g, ''))) {
                e.preventDefault();
            }
        });

        
        if (form) {
            form.addEventListener('submit', function(e) {
                var placa = inputPlaca.value.trim();
                var km = inputKm.value.trim();
                var errores = [];

                
                if (!placa) {
                    errores.push('La placa es requerida');
                    inputPlaca.classList.add('error');
                } else if (!PLACA_REGEX.test(placa)) {
                    errores.push('La placa no tiene un formato válido (ej: ABC-123 o ABC-12D)');
                    inputPlaca.classList.add('error');
                }

                
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

                
                inputKm.value = kmLimpio;
            });
        }
    });
})();
