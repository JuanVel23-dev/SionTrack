/**
 * SionTrack - Select Custom
 * Transforma automáticamente <select class="form-select"> en dropdowns personalizados.
 * Mantiene el <select> original oculto y sincronizado para compatibilidad con formularios.
 *
 * API global:
 *   SionSelect.init(selectElement)   — Inicializa un select específico
 *   SionSelect.refresh(selectElement) — Reconstruye opciones tras cambio dinámico
 *   SionSelect.initAll()             — Inicializa todos los selects pendientes
 */
(function() {
    'use strict';

    var FLECHA_SVG = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
        'stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">' +
        '<polyline points="6 9 12 15 18 9"></polyline></svg>';

    var ATTR_INIT = 'data-custom-select';

    /**
     * Inicializa un <select> convirtiéndolo en custom dropdown
     */
    function init(select) {
        if (!select || select.getAttribute(ATTR_INIT)) return;
        // No transformar selects dentro de un .custom-select manual (clientes-form)
        if (select.closest('.custom-select')) return;

        select.setAttribute(ATTR_INIT, '1');
        select.style.display = 'none';

        // Crear estructura
        var wrap = document.createElement('div');
        wrap.className = 'custom-select';

        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'custom-select-btn';
        btn.setAttribute('tabindex', '0');

        var texto = document.createElement('span');
        texto.className = 'select-texto placeholder';
        texto.textContent = getPlaceholder(select);

        var flecha = document.createElement('span');
        flecha.className = 'select-flecha';
        flecha.innerHTML = FLECHA_SVG;

        btn.appendChild(texto);
        btn.appendChild(flecha);

        var dropdown = document.createElement('div');
        dropdown.className = 'custom-select-dropdown';

        wrap.appendChild(btn);
        wrap.appendChild(dropdown);

        // Insertar después del select original
        select.parentNode.insertBefore(wrap, select.nextSibling);

        // Construir opciones
        buildOptions(select, dropdown, texto);

        // Sincronizar valor inicial
        syncFromSelect(select, dropdown, texto);

        // Eventos
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            toggleDropdown(wrap, btn);
        });

        dropdown.addEventListener('click', function(e) {
            var opcion = e.target.closest('.custom-select-option');
            if (!opcion || opcion.classList.contains('disabled')) return;

            selectOption(select, wrap, btn, texto, dropdown, opcion);
        });

        // Cerrar al hacer clic fuera
        document.addEventListener('click', function(e) {
            if (!wrap.contains(e.target)) {
                closeDropdown(wrap, btn);
            }
        });

        // Teclado: Escape cierra, Enter/Space toggle, flechas navegan
        btn.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeDropdown(wrap, btn);
            } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                e.preventDefault();
                if (!wrap.classList.contains('abierto')) {
                    toggleDropdown(wrap, btn);
                }
                navegarOpciones(dropdown, e.key === 'ArrowDown' ? 1 : -1);
            } else if (e.key === 'Enter' && wrap.classList.contains('abierto')) {
                e.preventDefault();
                var highlighted = dropdown.querySelector('.custom-select-option.highlighted');
                if (highlighted && !highlighted.classList.contains('disabled')) {
                    selectOption(select, wrap, btn, texto, dropdown, highlighted);
                }
            }
        });

        // Escape global
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && wrap.classList.contains('abierto')) {
                closeDropdown(wrap, btn);
            }
        });

        // Observar cambios en las opciones del select original (para selects dinámicos)
        var observer = new MutationObserver(function() {
            buildOptions(select, dropdown, texto);
            syncFromSelect(select, dropdown, texto);
        });
        observer.observe(select, { childList: true, subtree: true });

        // Escuchar cambios programáticos en el valor
        select.addEventListener('change', function() {
            syncFromSelect(select, dropdown, texto);
        });

        // Guardar referencia
        select._customSelect = { wrap: wrap, btn: btn, texto: texto, dropdown: dropdown, observer: observer };
    }

    /**
     * Obtiene el placeholder del select (primer option vacía o disabled)
     */
    function getPlaceholder(select) {
        var first = select.querySelector('option[value=""], option:disabled');
        if (first) return first.textContent.trim();
        return 'Seleccione...';
    }

    /**
     * Construye las opciones del dropdown desde el <select> original
     */
    function buildOptions(select, dropdown, texto) {
        dropdown.innerHTML = '';
        var options = select.querySelectorAll('option');

        options.forEach(function(opt) {
            var div = document.createElement('div');
            div.className = 'custom-select-option';
            div.textContent = opt.textContent.trim();
            div.setAttribute('data-value', opt.value);

            // Opciones placeholder o disabled
            if (opt.value === '' || opt.disabled) {
                div.classList.add('disabled');
            }

            // Opción seleccionada
            if (opt.selected && opt.value !== '') {
                div.classList.add('seleccionado');
            }

            dropdown.appendChild(div);
        });
    }

    /**
     * Sincroniza el estado visual desde el valor actual del <select>
     */
    function syncFromSelect(select, dropdown, texto) {
        var valor = select.value;
        var opciones = dropdown.querySelectorAll('.custom-select-option');

        opciones.forEach(function(op) {
            op.classList.remove('seleccionado');
        });

        if (valor) {
            var match = dropdown.querySelector('.custom-select-option[data-value="' + CSS.escape(valor) + '"]');
            if (match) {
                match.classList.add('seleccionado');
                texto.textContent = match.textContent;
                texto.classList.remove('placeholder');
                return;
            }
        }

        // Sin valor: mostrar placeholder
        texto.textContent = getPlaceholder(select);
        texto.classList.add('placeholder');
    }

    /**
     * Selecciona una opción
     */
    function selectOption(select, wrap, btn, texto, dropdown, opcion) {
        var valor = opcion.getAttribute('data-value');

        // Actualizar visual
        dropdown.querySelectorAll('.custom-select-option').forEach(function(op) {
            op.classList.remove('seleccionado');
        });
        opcion.classList.add('seleccionado');

        texto.textContent = opcion.textContent;
        texto.classList.remove('placeholder');

        // Actualizar select original
        select.value = valor;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        select.dispatchEvent(new Event('input', { bubbles: true }));

        closeDropdown(wrap, btn);
    }

    /**
     * Abre/cierra el dropdown
     */
    function toggleDropdown(wrap, btn) {
        // Cerrar otros dropdowns abiertos
        document.querySelectorAll('.custom-select.abierto').forEach(function(s) {
            if (s !== wrap) {
                s.classList.remove('abierto');
                s.querySelector('.custom-select-btn').classList.remove('activo');
            }
        });

        var abierto = wrap.classList.toggle('abierto');
        btn.classList.toggle('activo', abierto);

        // Scroll al elemento seleccionado
        if (abierto) {
            var selected = wrap.querySelector('.custom-select-option.seleccionado');
            if (selected) {
                var dd = wrap.querySelector('.custom-select-dropdown');
                dd.scrollTop = selected.offsetTop - dd.offsetHeight / 2;
            }
        }
    }

    function closeDropdown(wrap, btn) {
        wrap.classList.remove('abierto');
        btn.classList.remove('activo');
        // Limpiar highlights
        wrap.querySelectorAll('.custom-select-option.highlighted').forEach(function(op) {
            op.classList.remove('highlighted');
        });
    }

    /**
     * Navegación con flechas del teclado
     */
    function navegarOpciones(dropdown, direccion) {
        var opciones = Array.prototype.slice.call(
            dropdown.querySelectorAll('.custom-select-option:not(.disabled)')
        );
        if (!opciones.length) return;

        var actual = dropdown.querySelector('.custom-select-option.highlighted');
        var idx = actual ? opciones.indexOf(actual) : -1;

        if (actual) actual.classList.remove('highlighted');

        idx += direccion;
        if (idx < 0) idx = opciones.length - 1;
        if (idx >= opciones.length) idx = 0;

        opciones[idx].classList.add('highlighted');
        opciones[idx].scrollIntoView({ block: 'nearest' });
    }

    /**
     * Reconstruye las opciones de un custom select (para uso externo)
     */
    function refresh(select) {
        if (!select || !select._customSelect) return;
        var cs = select._customSelect;
        buildOptions(select, cs.dropdown, cs.texto);
        syncFromSelect(select, cs.dropdown, cs.texto);
    }

    /**
     * Inicializa todos los selects pendientes
     */
    function initAll() {
        var selects = document.querySelectorAll('select.form-select:not([' + ATTR_INIT + '])');
        selects.forEach(function(s) { init(s); });
    }

    // Exponer API global
    window.SionSelect = {
        init: init,
        refresh: refresh,
        initAll: initAll
    };

    // Auto-inicializar al cargar
    document.addEventListener('DOMContentLoaded', function() {
        initAll();
    });
})();
