/**
 * SionTrack - Number Input
 * Transforma input[type="number"].form-input en campos con botones +/-
 * y aplica filtro numerico para aceptar solo digitos (y punto decimal si aplica).
 * Se aplica automaticamente a todos los inputs numericos del formulario.
 */
(function() {
    'use strict';

    /**
     * Aplica filtro numerico a un input: convierte a type=text,
     * filtra caracteres no validos, y selecciona todo al enfocar.
     * @param {HTMLInputElement} input - El input a transformar
     */
    function aplicarFiltroNumerico(input) {
        // Guarda para evitar doble-inicializacion
        if (input.dataset.filtroNumerico) return;
        input.dataset.filtroNumerico = 'true';

        // Determinar si permite decimales basandose en el atributo step
        var stepAttr = input.getAttribute('step');
        var permiteDecimal = stepAttr && stepAttr.indexOf('.') !== -1;

        // Convertir a type=text para control total (elimina spinners nativos)
        input.setAttribute('type', 'text');
        input.setAttribute('inputmode', permiteDecimal ? 'decimal' : 'numeric');
        input.setAttribute('autocomplete', 'off');

        // Filtrar en cada input (cubre escritura, pegado, autocompletado)
        input.addEventListener('input', function() {
            var pos = this.selectionStart;
            var antes = this.value.length;

            if (permiteDecimal) {
                var limpio = this.value.replace(/[^\d.]/g, '');
                // Solo permitir un punto decimal
                var partes = limpio.split('.');
                if (partes.length > 2) {
                    limpio = partes[0] + '.' + partes.slice(1).join('');
                }
                this.value = limpio;
            } else {
                this.value = this.value.replace(/\D/g, '');
            }

            // Restaurar posicion del cursor
            var diff = antes - this.value.length;
            var nuevaPos = Math.max(0, pos - diff);
            this.setSelectionRange(nuevaPos, nuevaPos);
        });

        // Bloquear teclas no validas (retroalimentacion inmediata)
        input.addEventListener('keydown', function(e) {
            // Permitir combinaciones con Ctrl/Meta (copiar, pegar, etc)
            if (e.ctrlKey || e.metaKey || e.altKey) return;

            // Permitir teclas de navegacion y edicion
            var teclasPermitidas = [
                'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
                'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
                'Home', 'End'
            ];
            if (teclasPermitidas.indexOf(e.key) !== -1) return;

            // Permitir punto decimal (solo uno) en campos decimales
            if (permiteDecimal && e.key === '.' && this.value.indexOf('.') === -1) return;

            // Solo permitir digitos
            if (!/^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });

        // Sanitizar texto pegado
        input.addEventListener('paste', function(e) {
            e.preventDefault();
            var pegado = (e.clipboardData || window.clipboardData).getData('text');
            if (permiteDecimal) {
                pegado = pegado.replace(/[^\d.]/g, '');
                var partes = pegado.split('.');
                if (partes.length > 2) pegado = partes[0] + '.' + partes.slice(1).join('');
            } else {
                pegado = pegado.replace(/\D/g, '');
            }
            document.execCommand('insertText', false, pegado);
        });

        // Seleccionar todo al enfocar (resuelve el bug de concatenacion)
        input.addEventListener('focus', function() {
            var el = this;
            setTimeout(function() { el.select(); }, 0);
        });
    }

    // Exponer globalmente para inputs dinamicos
    window.SionNumericFilter = aplicarFiltroNumerico;

    document.addEventListener('DOMContentLoaded', function() {
        var inputs = document.querySelectorAll('input[type="number"].form-input');

        inputs.forEach(function(input) {
            // No transformar si ya fue procesado
            if (input.parentElement.classList.contains('number-input-wrap')) return;

            var step = parseFloat(input.getAttribute('step')) || 1;
            var min = input.hasAttribute('min') ? parseFloat(input.getAttribute('min')) : null;
            var max = input.hasAttribute('max') ? parseFloat(input.getAttribute('max')) : null;

            // Aplicar filtro numerico antes de envolver
            aplicarFiltroNumerico(input);

            // Crear wrapper
            var wrap = document.createElement('div');
            wrap.className = 'number-input-wrap';
            input.parentNode.insertBefore(wrap, input);
            wrap.appendChild(input);

            // Boton decrementar
            var btnDec = document.createElement('button');
            btnDec.type = 'button';
            btnDec.className = 'number-input-btn decrement';
            btnDec.setAttribute('tabindex', '-1');
            btnDec.setAttribute('aria-label', 'Disminuir');
            btnDec.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>';

            // Boton incrementar
            var btnInc = document.createElement('button');
            btnInc.type = 'button';
            btnInc.className = 'number-input-btn increment';
            btnInc.setAttribute('tabindex', '-1');
            btnInc.setAttribute('aria-label', 'Aumentar');
            btnInc.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>';

            wrap.appendChild(btnDec);
            wrap.appendChild(btnInc);

            // Cambiar valor con validacion
            function cambiarValor(delta) {
                var current = parseFloat(input.value) || 0;
                var nuevo = Math.round((current + delta) * 1000) / 1000;

                if (min !== null && nuevo < min) nuevo = min;
                if (max !== null && nuevo > max) nuevo = max;

                // Formatear decimales segun step
                if (step < 1) {
                    var decimales = String(step).split('.')[1] ? String(step).split('.')[1].length : 0;
                    input.value = nuevo.toFixed(decimales);
                } else {
                    input.value = nuevo;
                }

                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            }

            btnDec.addEventListener('click', function() { cambiarValor(-step); });
            btnInc.addEventListener('click', function() { cambiarValor(step); });

            // Mantener presionado para incremento continuo
            var interval = null;
            function iniciarRepeticion(delta) {
                cambiarValor(delta);
                interval = setInterval(function() { cambiarValor(delta); }, 120);
            }

            function detenerRepeticion() {
                if (interval) { clearInterval(interval); interval = null; }
            }

            btnDec.addEventListener('mousedown', function() { iniciarRepeticion(-step); });
            btnInc.addEventListener('mousedown', function() { iniciarRepeticion(step); });
            document.addEventListener('mouseup', detenerRepeticion);
            document.addEventListener('mouseleave', detenerRepeticion);
        });
    });
})();
