
(function() {
    'use strict';

    
    function aplicarFiltroNumerico(input) {
        
        if (input.dataset.filtroNumerico) return;
        input.dataset.filtroNumerico = 'true';

        
        var stepAttr = input.getAttribute('step');
        var permiteDecimal = stepAttr && stepAttr.indexOf('.') !== -1;

        
        input.setAttribute('type', 'text');
        input.setAttribute('inputmode', permiteDecimal ? 'decimal' : 'numeric');
        input.setAttribute('autocomplete', 'off');

        
        input.addEventListener('input', function() {
            var pos = this.selectionStart;
            var antes = this.value.length;

            if (permiteDecimal) {
                var limpio = this.value.replace(/[^\d.]/g, '');
                
                var partes = limpio.split('.');
                if (partes.length > 2) {
                    limpio = partes[0] + '.' + partes.slice(1).join('');
                }
                this.value = limpio;
            } else {
                this.value = this.value.replace(/\D/g, '');
            }

            
            var diff = antes - this.value.length;
            var nuevaPos = Math.max(0, pos - diff);
            this.setSelectionRange(nuevaPos, nuevaPos);
        });

        
        input.addEventListener('keydown', function(e) {
            
            if (e.ctrlKey || e.metaKey || e.altKey) return;

            
            var teclasPermitidas = [
                'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
                'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
                'Home', 'End'
            ];
            if (teclasPermitidas.indexOf(e.key) !== -1) return;

            
            if (permiteDecimal && e.key === '.' && this.value.indexOf('.') === -1) return;

            
            if (!/^\d$/.test(e.key)) {
                e.preventDefault();
            }
        });

        
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

        
        input.addEventListener('focus', function() {
            var el = this;
            setTimeout(function() { el.select(); }, 0);
        });
    }

    
    window.SionNumericFilter = aplicarFiltroNumerico;

    document.addEventListener('DOMContentLoaded', function() {
        var inputs = document.querySelectorAll('input[type="number"].form-input');

        inputs.forEach(function(input) {
            
            if (input.parentElement.classList.contains('number-input-wrap')) return;

            var step = parseFloat(input.getAttribute('step')) || 1;
            var min = input.hasAttribute('min') ? parseFloat(input.getAttribute('min')) : null;
            var max = input.hasAttribute('max') ? parseFloat(input.getAttribute('max')) : null;

            
            aplicarFiltroNumerico(input);

            
            var wrap = document.createElement('div');
            wrap.className = 'number-input-wrap';
            input.parentNode.insertBefore(wrap, input);
            wrap.appendChild(input);

            
            var btnDec = document.createElement('button');
            btnDec.type = 'button';
            btnDec.className = 'number-input-btn decrement';
            btnDec.setAttribute('tabindex', '-1');
            btnDec.setAttribute('aria-label', 'Disminuir');
            btnDec.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>';

            
            var btnInc = document.createElement('button');
            btnInc.type = 'button';
            btnInc.className = 'number-input-btn increment';
            btnInc.setAttribute('tabindex', '-1');
            btnInc.setAttribute('aria-label', 'Aumentar');
            btnInc.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>';

            wrap.appendChild(btnDec);
            wrap.appendChild(btnInc);

            
            function cambiarValor(delta) {
                var current = parseFloat(input.value) || 0;
                var nuevo = Math.round((current + delta) * 1000) / 1000;

                if (min !== null && nuevo < min) nuevo = min;
                if (max !== null && nuevo > max) nuevo = max;

                
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
