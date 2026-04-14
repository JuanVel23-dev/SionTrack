
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

    
    var MIN_OPCIONES_BUSQUEDA = 5;

    
    function init(select) {
        if (!select || select.getAttribute(ATTR_INIT)) return;
        
        if (select.closest('.custom-select')) return;

        select.setAttribute(ATTR_INIT, '1');
        select.style.display = 'none';

        
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

        
        var emptyMsg = document.createElement('div');
        emptyMsg.className = 'custom-select-empty';
        emptyMsg.textContent = 'Sin resultados';
        emptyMsg.style.display = 'none';

        
        dropdown.appendChild(searchWrap);
        
        
        dropdown.appendChild(emptyMsg);

        wrap.appendChild(btn);
        wrap.appendChild(dropdown);

        
        select.parentNode.insertBefore(wrap, select.nextSibling);

        
        buildOptions(select, dropdown, texto, searchWrap, emptyMsg);

        
        syncFromSelect(select, dropdown, texto);

        

        
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            toggleDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
        });

        
        dropdown.addEventListener('click', function(e) {
            var opcion = e.target.closest('.custom-select-option');
            if (!opcion || opcion.classList.contains('disabled')) return;
            selectOption(select, wrap, btn, texto, dropdown, opcion, searchInput, emptyMsg);
        });

        
        searchWrap.addEventListener('click', function(e) {
            e.stopPropagation();
        });

        
        searchInput.addEventListener('input', function() {
            filtrarOpciones(dropdown, emptyMsg, this.value);
        });

        
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

        
        document.addEventListener('click', function(e) {
            if (!wrap.contains(e.target)) {
                closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
            }
        });

        
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

        
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && wrap.classList.contains('abierto')) {
                closeDropdown(wrap, btn, dropdown, searchInput, emptyMsg);
            }
        });

        
        var observer = new MutationObserver(function() {
            buildOptions(select, dropdown, texto, searchWrap, emptyMsg);
            syncFromSelect(select, dropdown, texto);
        });
        observer.observe(select, { childList: true, subtree: true });

        
        select.addEventListener('change', function() {
            syncFromSelect(select, dropdown, texto);
        });

        
        select._customSelect = {
            wrap: wrap, btn: btn, texto: texto,
            dropdown: dropdown, searchInput: searchInput,
            emptyMsg: emptyMsg, searchWrap: searchWrap,
            observer: observer
        };
    }

    
    function getPlaceholder(select) {
        var first = select.querySelector('option[value=""], option:disabled');
        if (first) return first.textContent.trim();
        return 'Seleccione...';
    }

    
    function buildOptions(select, dropdown, texto, searchWrap, emptyMsg) {
        
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

            
            dropdown.insertBefore(div, emptyMsg);
        });

        
        if (searchWrap) {
            var forzarBusqueda = select.hasAttribute('data-searchable');
            searchWrap.style.display = (forzarBusqueda || conteoReales >= MIN_OPCIONES_BUSQUEDA) ? '' : 'none';
        }
    }

    
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

    
    function filtrarOpciones(dropdown, emptyMsg, termino) {
        var term = termino.toLowerCase().trim();
        var opciones = dropdown.querySelectorAll('.custom-select-option');
        var hayVisibles = false;

        opciones.forEach(function(op) {
            if (op.classList.contains('disabled')) {
                
                op.style.display = term ? 'none' : '';
                return;
            }

            var texto = op.textContent.toLowerCase();
            var visible = !term || texto.indexOf(term) !== -1;
            op.style.display = visible ? '' : 'none';
            if (visible) hayVisibles = true;
        });

        emptyMsg.style.display = (!term || hayVisibles) ? 'none' : '';

        
        dropdown.querySelectorAll('.custom-select-option.highlighted').forEach(function(op) {
            op.classList.remove('highlighted');
        });
    }

    
    function toggleDropdown(wrap, btn, dropdown, searchInput, emptyMsg) {
        
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

        
        var filaPadre = wrap.closest('.detalle-fila');
        if (filaPadre) {
            filaPadre.classList.toggle('dropdown-activo', abierto);
        }

        if (abierto) {
            
            var searchVisible = searchInput && searchInput.parentNode.style.display !== 'none';
            if (searchVisible) {
                setTimeout(function() { searchInput.focus(); }, 50);
            }

            
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

        
        var filaPadre = wrap.closest('.detalle-fila');
        if (filaPadre) {
            filaPadre.classList.remove('dropdown-activo');
        }

        
        dropdown.querySelectorAll('.custom-select-option.highlighted').forEach(function(op) {
            op.classList.remove('highlighted');
        });
        resetBusqueda(searchInput, dropdown, emptyMsg);
    }

    
    function resetBusqueda(searchInput, dropdown, emptyMsg) {
        if (searchInput) searchInput.value = '';
        if (emptyMsg) emptyMsg.style.display = 'none';
        if (dropdown) {
            dropdown.querySelectorAll('.custom-select-option').forEach(function(op) {
                op.style.display = '';
            });
        }
    }

    
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

    
    function refresh(select) {
        if (!select || !select._customSelect) return;
        var cs = select._customSelect;
        buildOptions(select, cs.dropdown, cs.texto, cs.searchWrap, cs.emptyMsg);
        syncFromSelect(select, cs.dropdown, cs.texto);
    }

    
    function initAll() {
        var selects = document.querySelectorAll('select.form-select:not([' + ATTR_INIT + '])');
        selects.forEach(function(s) { init(s); });
    }

    
    window.SionSelect = {
        init: init,
        refresh: refresh,
        initAll: initAll
    };

    
    document.addEventListener('DOMContentLoaded', function() {
        initAll();
    });
})();
