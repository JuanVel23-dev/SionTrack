/**
 * SionTrack - Select Custom
 * Transforma automáticamente <select class="form-select"> en dropdowns personalizados
 * con campo de búsqueda integrado para filtrar opciones.
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

    var BUSCAR_SVG = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
        'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
        '<circle cx="11" cy="11" r="8"></circle>' +
        '<line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>';

    var ATTR_INIT = 'data-custom-select';

    // Umbral mínimo de opciones reales para mostrar búsqueda
    var MIN_OPCIONES_BUSQUEDA = 5;

    /**
     * Inicializa un <select> convirtiéndolo en custom dropdown con búsqueda
     */
    function init(select) {
        if (!select || select.getAttribute(ATTR_INIT)) return;
        // No transformar selects dentro de un .custom-select manual
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

        // Dropdown — contiene búsqueda + opciones en un solo contenedor scrolleable
        var dropdown = document.createElement('div');
        dropdown.className = 'custom-select-dropdown';

        // Campo de búsqueda (sticky dentro del scroll)
        var searchWrap = document.createElement('div');
        searchWrap.className = 'custom-select-search';

        var searchIcon = document.createElement('span');
        searchIcon.className = 'custom-select-search-icon';
        searchIcon.innerHTML = BUSCAR_SVG;

        var searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchInput.className = 'custom-select-search-input';
        searchInput.placeholder = 'Buscar...';
        searchInput.setAttribute('autocomplete', 'off');

        searchWrap.appendChild(searchIcon);
        searchWrap.appendChild(searchInput);

        // Mensaje "sin resultados"
        var emptyMsg = document.createElement('div');
        emptyMsg.className = 'custom-select-empty';
        emptyMsg.textContent = 'Sin resultados';
        emptyMsg.style.display = 'none';

        // Ensamblar: search primero (sticky), luego opciones, luego empty
        dropdown.appendChild(searchWrap);
        // Las opciones se agregan directamente al dropdown via buildOptions
        // emptyMsg se agrega al final
        dropdown.appendChild(emptyMsg);

        wrap.appendChild(btn);
        wrap.appendChild(dropdown);

        // Insertar después del select original
        select.parentNode.insertBefore(wrap, select.nextSibling);

        // Construir opciones (se insertan antes de emptyMsg)
        buildOptions(select, dropdown, texto, searchWrap, emptyMsg);

        // Sincronizar valor inicial
        syncFromSelect(select, dropdown, texto);

        // ===== EVENTOS =====

        // Abrir/cerrar dropdown
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            toggleDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
        });

        // Click en opción
        dropdown.addEventListener('click', function(e) {
            var opcion = e.target.closest('.custom-select-option');
            if (!opcion || opcion.classList.contains('disabled')) return;
            selectOption(select, wrap, btn, texto, dropdown, opcion, searchInput, emptyMsg);
        });

        // Evitar que clicks en el search cierren el dropdown
        searchWrap.addEventListener('click', function(e) {
            e.stopPropagation();
        });

        // Búsqueda en tiempo real
        searchInput.addEventListener('input', function() {
            filtrarOpciones(dropdown, emptyMsg, this.value);
        });

        // Teclado en el campo de búsqueda
        searchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
            } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                e.preventDefault();
                navegarOpciones(dropdown, e.key === 'ArrowDown' ? 1 : -1);
            } else if (e.key === 'Enter') {
                e.preventDefault();
                var highlighted = dropdown.querySelector('.custom-select-option.highlighted');
                if (highlighted && !highlighted.classList.contains('disabled')) {
                    selectOption(select, wrap, btn, texto, dropdown, highlighted, searchInput, emptyMsg);
                }
            }
        });

        // Cerrar al hacer clic fuera
        document.addEventListener('click', function(e) {
            if (!wrap.contains(e.target)) {
                closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
            }
        });

        // Teclado en el botón
        btn.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
            } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                e.preventDefault();
                if (!wrap.classList.contains('abierto')) {
                    toggleDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
                }
                navegarOpciones(dropdown, e.key === 'ArrowDown' ? 1 : -1);
            } else if (e.key === 'Enter' && wrap.classList.contains('abierto')) {
                e.preventDefault();
                var highlighted = dropdown.querySelector('.custom-select-option.highlighted');
                if (highlighted && !highlighted.classList.contains('disabled')) {
                    selectOption(select, wrap, btn, texto, dropdown, highlighted, searchInput, emptyMsg);
                }
            }
        });

        // Escape global
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && wrap.classList.contains('abierto')) {
                closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
            }
        });

        // Observar cambios en las opciones del select original
        var observer = new MutationObserver(function() {
            buildOptions(select, dropdown, texto, searchWrap, emptyMsg);
            syncFromSelect(select, dropdown, texto);
        });
        observer.observe(select, { childList: true, subtree: true });

        // Escuchar cambios programáticos en el valor
        select.addEventListener('change', function() {
            syncFromSelect(select, dropdown, texto);
        });

        // Guardar referencia
        select._customSelect = {
            wrap: wrap, btn: btn, texto: texto,
            dropdown: dropdown, searchInput: searchInput,
            emptyMsg: emptyMsg, searchWrap: searchWrap,
            observer: observer
        };
    }

    /**
     * Obtiene el placeholder del select
     */
    function getPlaceholder(select) {
        var first = select.querySelector('option[value=""], option:disabled');
        if (first) return first.textContent.trim();
        return 'Seleccione...';
    }

    /**
     * Construye las opciones del dropdown desde el <select> original.
     * Las opciones se insertan antes del emptyMsg dentro del dropdown.
     */
    function buildOptions(select, dropdown, texto, searchWrap, emptyMsg) {
        // Eliminar opciones anteriores (no tocar search ni emptyMsg)
        var viejas = dropdown.querySelectorAll('.custom-select-option');
        viejas.forEach(function(op) { op.remove(); });

        var options = select.querySelectorAll('option');
        var conteoReales = 0;

        options.forEach(function(opt) {
            var div = document.createElement('div');
            div.className = 'custom-select-option';
            div.textContent = opt.textContent.trim();
            div.setAttribute('data-value', opt.value);

            if (opt.value === '' || opt.disabled) {
                div.classList.add('disabled');
            } else {
                conteoReales++;
            }

            if (opt.selected && opt.value !== '') {
                div.classList.add('seleccionado');
            }

            // Insertar antes del emptyMsg
            dropdown.insertBefore(div, emptyMsg);
        });

        // Mostrar/ocultar búsqueda según cantidad de opciones o atributo data-searchable
        if (searchWrap) {
            var forzarBusqueda = select.hasAttribute('data-searchable');
            searchWrap.style.display = (forzarBusqueda || conteoReales >= MIN_OPCIONES_BUSQUEDA) ? '' : 'none';
        }
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

        texto.textContent = getPlaceholder(select);
        texto.classList.add('placeholder');
    }

    /**
     * Selecciona una opción
     */
    function selectOption(select, wrap, btn, texto, dropdown, opcion, searchInput, emptyMsg) {
        var valor = opcion.getAttribute('data-value');

        dropdown.querySelectorAll('.custom-select-option').forEach(function(op) {
            op.classList.remove('seleccionado');
        });
        opcion.classList.add('seleccionado');

        texto.textContent = opcion.textContent;
        texto.classList.remove('placeholder');

        select.value = valor;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        select.dispatchEvent(new Event('input', { bubbles: true }));

        closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
    }

    /**
     * Filtra opciones según término de búsqueda
     */
    function filtrarOpciones(dropdown, emptyMsg, termino) {
        var term = termino.toLowerCase().trim();
        var opciones = dropdown.querySelectorAll('.custom-select-option');
        var hayVisibles = false;

        opciones.forEach(function(op) {
            if (op.classList.contains('disabled')) {
                // Ocultar placeholder al buscar, mostrar si búsqueda vacía
                op.style.display = term ? 'none' : '';
                return;
            }

            var texto = op.textContent.toLowerCase();
            var visible = !term || texto.indexOf(term) !== -1;
            op.style.display = visible ? '' : 'none';
            if (visible) hayVisibles = true;
        });

        emptyMsg.style.display = (!term || hayVisibles) ? 'none' : '';

        // Limpiar highlights al filtrar
        dropdown.querySelectorAll('.custom-select-option.highlighted').forEach(function(op) {
            op.classList.remove('highlighted');
        });
    }

    /**
     * Abre/cierra el dropdown
     */
    function toggleDropdown(wrap, btn, dropdown, searchInput, emptyMsg) {
        // Cerrar otros dropdowns abiertos
        document.querySelectorAll('.custom-select.abierto').forEach(function(s) {
            if (s !== wrap) {
                s.classList.remove('abierto');
                var otroBtn = s.querySelector('.custom-select-btn');
                if (otroBtn) otroBtn.classList.remove('activo');
                var otroSearch = s.querySelector('.custom-select-search-input');
                var otroDD = s.querySelector('.custom-select-dropdown');
                var otroEmpty = s.querySelector('.custom-select-empty');
                if (otroSearch) resetBusqueda(otroSearch, otroDD, otroEmpty);
            }
        });

        var abierto = wrap.classList.toggle('abierto');
        btn.classList.toggle('activo', abierto);

        // Elevar z-index de la fila padre (para filas de detalle apiladas)
        var filaPadre = wrap.closest('.detalle-fila');
        if (filaPadre) {
            filaPadre.classList.toggle('dropdown-activo', abierto);
        }

        if (abierto) {
            // Focus en el campo de búsqueda si es visible
            var searchVisible = searchInput && searchInput.parentNode.style.display !== 'none';
            if (searchVisible) {
                setTimeout(function() { searchInput.focus(); }, 50);
            }

            // Scroll al elemento seleccionado
            var selected = dropdown.querySelector('.custom-select-option.seleccionado');
            if (selected) {
                dropdown.scrollTop = selected.offsetTop - dropdown.offsetHeight / 2;
            }
        } else {
            resetBusqueda(searchInput, dropdown, emptyMsg);
        }
    }

    function closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg) {
        wrap.classList.remove('abierto');
        btn.classList.remove('activo');

        // Quitar z-index elevado de fila padre
        var filaPadre = wrap.closest('.detalle-fila');
        if (filaPadre) {
            filaPadre.classList.remove('dropdown-activo');
        }

        // Limpiar highlights y búsqueda
        dropdown.querySelectorAll('.custom-select-option.highlighted').forEach(function(op) {
            op.classList.remove('highlighted');
        });
        resetBusqueda(searchInput, dropdown, emptyMsg);
    }

    /**
     * Limpia el campo de búsqueda y restaura todas las opciones
     */
    function resetBusqueda(searchInput, dropdown, emptyMsg) {
        if (searchInput) searchInput.value = '';
        if (emptyMsg) emptyMsg.style.display = 'none';
        if (dropdown) {
            dropdown.querySelectorAll('.custom-select-option').forEach(function(op) {
                op.style.display = '';
            });
        }
    }

    /**
     * Navegación con flechas del teclado (solo opciones visibles)
     */
    function navegarOpciones(dropdown, direccion) {
        var opciones = Array.prototype.slice.call(
            dropdown.querySelectorAll('.custom-select-option:not(.disabled)')
        ).filter(function(op) {
            return op.style.display !== 'none';
        });
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
        buildOptions(select, cs.dropdown, cs.texto, cs.searchWrap, cs.emptyMsg);
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
