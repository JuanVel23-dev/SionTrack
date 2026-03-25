/**
 * SionTrack - Number Input
 * Transforma input[type="number"].form-input en campos con botones +/-
 * Se aplica automáticamente a todos los inputs numéricos del formulario.
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var inputs = document.querySelectorAll('input[type="number"].form-input');

        inputs.forEach(function(input) {
            // No transformar si ya fue procesado
            if (input.parentElement.classList.contains('number-input-wrap')) return;

            var step = parseFloat(input.getAttribute('step')) || 1;
            var min = input.hasAttribute('min') ? parseFloat(input.getAttribute('min')) : null;
            var max = input.hasAttribute('max') ? parseFloat(input.getAttribute('max')) : null;

            // Crear wrapper
            var wrap = document.createElement('div');
            wrap.className = 'number-input-wrap';
            input.parentNode.insertBefore(wrap, input);
            wrap.appendChild(input);

            // Botón decrementar
            var btnDec = document.createElement('button');
            btnDec.type = 'button';
            btnDec.className = 'number-input-btn decrement';
            btnDec.setAttribute('tabindex', '-1');
            btnDec.setAttribute('aria-label', 'Disminuir');
            btnDec.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>';

            // Botón incrementar
            var btnInc = document.createElement('button');
            btnInc.type = 'button';
            btnInc.className = 'number-input-btn increment';
            btnInc.setAttribute('tabindex', '-1');
            btnInc.setAttribute('aria-label', 'Aumentar');
            btnInc.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>';

            wrap.appendChild(btnDec);
            wrap.appendChild(btnInc);

            // Cambiar valor con validación
            function cambiarValor(delta) {
                var current = parseFloat(input.value) || 0;
                var nuevo = Math.round((current + delta) * 1000) / 1000;

                if (min !== null && nuevo < min) nuevo = min;
                if (max !== null && nuevo > max) nuevo = max;

                input.value = nuevo;
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
