
(function() {
    'use strict';

    var closeSvg = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';
    var arrowDownSvg = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<polyline points="7 13 12 18 17 13"></polyline><polyline points="7 6 12 11 17 6"></polyline></svg>';

    function debounce(fn, delay) {
        var timer;
        return function() {
            var ctx = this, args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function() { fn.apply(ctx, args); }, delay);
        };
    }

    function abrirDropdown(dd, toggle) {
        dd.classList.add('visible');
        if (toggle) toggle.classList.add('abierto');
    }
    function cerrarDropdown(dd, toggle) {
        dd.classList.remove('visible');
        if (toggle) toggle.classList.remove('abierto');
    }
    function isDropdownVisible(dd) {
        return dd.classList.contains('visible');
    }

    document.addEventListener('DOMContentLoaded', function() {
        var provSearchInput = document.getElementById('proveedor_search');
        var provDropdown = document.getElementById('proveedorDropdown');
        var provToggle = document.getElementById('proveedorToggle');
        var provSearchBtn = document.getElementById('proveedorSearchBtn');
        var provSelected = document.getElementById('proveedorSelected');
        var provSelectedName = document.getElementById('proveedorSelectedName');
        var provClear = document.getElementById('proveedorClear');
        var provInputWrap = document.getElementById('proveedorInputWrap');
        var provHidden = document.getElementById('proveedor_id');

        if (!provSearchInput || !provDropdown) return;

        var provPage = 0;
        var provQuery = '';

        
        if (provHidden && provHidden.value) {
            cargarProveedorExistente(provHidden.value);
        }

        function cargarProveedorExistente(id) {
            
            fetch('/api/proveedores/buscar?q=&page=0&size=1000', {
                headers: { 'Accept': 'application/json' }
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var items = data.content || [];
                var encontrado = null;
                for (var i = 0; i < items.length; i++) {
                    if (String(items[i].proveedor_id) === String(id)) {
                        encontrado = items[i];
                        break;
                    }
                }
                if (encontrado) {
                    provInputWrap.style.display = 'none';
                    provSelected.style.display = '';
                    var nombre = encontrado.nombre || '';
                    if (encontrado.nombre_contacto) nombre += ' — ' + encontrado.nombre_contacto;
                    provSelectedName.textContent = nombre;
                }
            })
            .catch(function() {});
        }

        function buscarProveedores(query, page, append) {
            provQuery = query;
            provPage = page;

            if (!append) {
                provDropdown.innerHTML = '<div class="ajax-select-loading">Buscando proveedores...</div>';
                abrirDropdown(provDropdown, provToggle);
            }

            fetch('/api/proveedores/buscar?q=' + encodeURIComponent(query) + '&page=' + page + '&size=20', {
                headers: { 'Accept': 'application/json' }
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var items = data.content || [];
                var totalElements = data.totalElements || 0;

                if (!append) provDropdown.innerHTML = '';

                if (items.length === 0 && !append) {
                    provDropdown.innerHTML =
                        '<div class="ajax-select-empty">' +
                            '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>' +
                            'No se encontraron proveedores' +
                        '</div>';
                    return;
                }

                
                if (!append) {
                    var header = document.createElement('div');
                    header.className = 'ajax-select-dropdown-header';
                    header.innerHTML =
                        '<span class="ajax-select-dropdown-count">' + totalElements + ' proveedores encontrados</span>' +
                        '<button type="button" class="ajax-select-dropdown-close" title="Cerrar">' + closeSvg + '</button>';
                    header.querySelector('.ajax-select-dropdown-close').addEventListener('click', function(e) {
                        e.stopPropagation();
                        cerrarDropdown(provDropdown, provToggle);
                    });
                    provDropdown.appendChild(header);
                }

                items.forEach(function(p) {
                    var div = document.createElement('div');
                    div.className = 'ajax-select-option';
                    div.dataset.id = p.proveedor_id;
                    var label = esc(p.nombre || '');
                    var sub = esc(p.nombre_contacto || '');
                    if (p.telefono) sub += (sub ? ' — ' : '') + esc(p.telefono);
                    div.innerHTML =
                        '<span class="ajax-select-option-main">' + label + '</span>' +
                        '<span class="ajax-select-option-sub">' + sub + '</span>';
                    div.addEventListener('click', function() {
                        seleccionarProveedor(p);
                    });
                    provDropdown.appendChild(div);
                });

                
                var existingMore = provDropdown.querySelector('.ajax-select-more');
                if (existingMore) existingMore.remove();

                if (!data.last) {
                    var moreBtn = document.createElement('button');
                    moreBtn.type = 'button';
                    moreBtn.className = 'ajax-select-more';
                    moreBtn.innerHTML = arrowDownSvg + ' Cargar mas resultados...';
                    moreBtn.addEventListener('click', function(e) {
                        e.stopPropagation();
                        buscarProveedores(provQuery, provPage + 1, true);
                    });
                    provDropdown.appendChild(moreBtn);
                }
            })
            .catch(function() {
                provDropdown.innerHTML = '<div class="ajax-select-empty">Error al buscar</div>';
            });
        }

        function seleccionarProveedor(p) {
            provHidden.value = p.proveedor_id;
            cerrarDropdown(provDropdown, provToggle);
            provInputWrap.style.display = 'none';
            provSelected.style.display = '';
            var nombre = p.nombre || '';
            if (p.nombre_contacto) nombre += ' — ' + p.nombre_contacto;
            provSelectedName.textContent = nombre;
        }

        function limpiarProveedor() {
            provHidden.value = '';
            provSearchInput.value = '';
            provInputWrap.style.display = '';
            provSelected.style.display = 'none';
            cerrarDropdown(provDropdown, provToggle);
            provSearchInput.focus();
        }

        var debouncedSearch = debounce(function() {
            buscarProveedores(provSearchInput.value.trim(), 0, false);
        }, 350);

        provSearchInput.addEventListener('input', debouncedSearch);
        provSearchInput.addEventListener('focus', function() {
            if (!provHidden.value && !isDropdownVisible(provDropdown)) {
                buscarProveedores(this.value.trim(), 0, false);
            }
        });
        provSearchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarProveedores(this.value.trim(), 0, false);
            }
            if (e.key === 'Escape') {
                cerrarDropdown(provDropdown, provToggle);
            }
        });

        if (provSearchBtn) {
            provSearchBtn.addEventListener('click', function(e) {
                e.preventDefault();
                buscarProveedores(provSearchInput.value.trim(), 0, false);
            });
        }

        if (provToggle) {
            provToggle.addEventListener('click', function(e) {
                e.preventDefault();
                if (isDropdownVisible(provDropdown)) {
                    cerrarDropdown(provDropdown, provToggle);
                } else {
                    buscarProveedores(provSearchInput.value.trim(), 0, false);
                }
            });
        }

        if (provClear) {
            provClear.addEventListener('click', limpiarProveedor);
        }

        
        document.addEventListener('click', function(e) {
            if (provDropdown && !e.target.closest('#proveedorSelectWrap')) {
                cerrarDropdown(provDropdown, provToggle);
            }
        });
    });

    function esc(t) { return SionUtils.esc(t, ''); }
})();
