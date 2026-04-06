/**
 * SionTrack - Servicios Form
 *
 * Toggle tipo de servicio (PRODUCTO / MANO_DE_OBRA)
 * Cliente: busqueda AJAX paginada en /api/clientes/buscar
 * Vehiculos: cascada via FETCH a /api/clientes/{id}/vehiculos
 * Productos: busqueda AJAX paginada en /api/productos/buscar
 */
(function() {
    'use strict';

    var detalleIndex = 0;

    var removeSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="18" y1="6" x2="6" y2="18"></line>' +
        '<line x1="6" y1="6" x2="18" y2="18"></line></svg>';

    var searchSvg = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>';

    var closeSvg = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';

    var chevronSvg = '<svg class="toggle-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<polyline points="6 9 12 15 18 9"></polyline></svg>';

    var packageSvg = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="16.5" y1="9.4" x2="7.5" y2="4.21"></line>' +
        '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>' +
        '<polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>' +
        '<line x1="12" y1="22.08" x2="12" y2="12"></line></svg>';

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

    // =============================================
    // Abrir/cerrar dropdown con animacion
    // =============================================
    function abrirDropdown(dropdown, toggleBtn) {
        dropdown.classList.add('visible');
        if (toggleBtn) toggleBtn.classList.add('abierto');
    }

    function cerrarDropdown(dropdown, toggleBtn) {
        dropdown.classList.remove('visible');
        if (toggleBtn) toggleBtn.classList.remove('abierto');
    }

    function isDropdownVisible(dropdown) {
        return dropdown.classList.contains('visible');
    }

    document.addEventListener('DOMContentLoaded', function() {
        var clienteIdInput = document.getElementById('cliente_id');
        var clienteSearchInput = document.getElementById('cliente_search');
        var clienteDropdown = document.getElementById('clienteDropdown');
        var clienteSelected = document.getElementById('clienteSelected');
        var clienteSelectedName = document.getElementById('clienteSelectedName');
        var clienteClear = document.getElementById('clienteClear');
        var clienteSearchBtn = document.getElementById('clienteSearchBtn');
        var clienteToggle = document.getElementById('clienteToggle');

        var vehiculoSelect = document.getElementById('vehiculo_id');
        var detallesContainer = document.getElementById('detalles-container');
        var addDetalleBtn = document.getElementById('add-detalle-btn');
        var totalDisplay = document.getElementById('total-display');
        var form = document.getElementById('servicioForm');

        // Tipo de servicio
        var tipoToggle = document.getElementById('tipoServicioToggle');
        var tipoInput = document.getElementById('tipo_servicio');
        var tipoBtns = tipoToggle ? tipoToggle.querySelectorAll('.tipo-servicio-btn') : [];

        // Seccion vehiculo
        var vehiculoWrap = document.getElementById('vehiculoWrap');
        var vehiculoSection = document.getElementById('vehiculoSection');
        var vehiculoAddBtn = document.getElementById('toggleVehiculoAdd');
        var vehiculoCloseBtn = document.getElementById('toggleVehiculoClose');
        var vehiculoLabel = document.getElementById('vehiculoLabel');
        var kmLabel = document.getElementById('kmLabel');

        var tipoActual = '';
        var vehiculoVisible = false;

        // Fecha de hoy por defecto
        var fechaInput = document.getElementById('fecha_servicio');
        if (fechaInput && !fechaInput.value) {
            fechaInput.value = new Date().toISOString().split('T')[0];
        }

        // =============================================
        // TOGGLE TIPO DE SERVICIO
        // =============================================
        tipoBtns.forEach(function(btn) {
            btn.addEventListener('click', function() {
                var valor = this.dataset.value;
                tipoBtns.forEach(function(b) { b.classList.remove('activo'); });
                this.classList.add('activo');
                tipoInput.value = valor;
                tipoActual = valor;
                configurarCamposVehiculo();
            });
        });

        function configurarCamposVehiculo() {
            if (tipoActual === 'MANO_DE_OBRA') {
                vehiculoWrap.style.display = '';
                vehiculoAddBtn.style.display = 'none';
                vehiculoCloseBtn.style.display = 'none';
                expandirCampos(true);
                vehiculoLabel.classList.add('required');
                kmLabel.classList.add('required');
                vehiculoSelect.setAttribute('required', '');
            } else if (tipoActual === 'PRODUCTO') {
                vehiculoWrap.style.display = '';
                vehiculoLabel.classList.remove('required');
                kmLabel.classList.remove('required');
                vehiculoSelect.removeAttribute('required');
                vehiculoVisible = false;
                vehiculoSection.style.display = 'none';
                vehiculoSection.style.opacity = '';
                vehiculoSection.style.transform = '';
                vehiculoSection.style.maxHeight = '';
                vehiculoAddBtn.style.display = '';
                vehiculoCloseBtn.style.display = 'none';
                vehiculoSelect.value = '';
                var kmInput = document.getElementById('kilometraje_servicio');
                if (kmInput) kmInput.value = '';
            } else {
                vehiculoWrap.style.display = 'none';
                vehiculoVisible = false;
                vehiculoLabel.classList.remove('required');
                kmLabel.classList.remove('required');
                vehiculoSelect.removeAttribute('required');
            }
        }

        function expandirCampos(inmediato) {
            vehiculoVisible = true;
            vehiculoSection.style.display = '';
            if (inmediato) {
                vehiculoSection.style.opacity = '1';
                vehiculoSection.style.transform = 'translateY(0)';
                vehiculoSection.style.maxHeight = '300px';
                return;
            }
            vehiculoSection.style.opacity = '0';
            vehiculoSection.style.transform = 'translateY(-10px)';
            vehiculoSection.style.maxHeight = '0';
            vehiculoSection.style.overflow = 'hidden';
            requestAnimationFrame(function() {
                vehiculoSection.style.transition = 'opacity 0.35s ease, transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), max-height 0.35s ease';
                vehiculoSection.style.opacity = '1';
                vehiculoSection.style.transform = 'translateY(0)';
                vehiculoSection.style.maxHeight = '300px';
                setTimeout(function() {
                    vehiculoSection.style.overflow = '';
                    vehiculoSection.style.maxHeight = '';
                }, 350);
            });
        }

        function colapsarCampos() {
            vehiculoVisible = false;
            vehiculoSection.style.overflow = 'hidden';
            vehiculoSection.style.transition =
                'opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1), ' +
                'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), ' +
                'filter 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
            vehiculoSection.style.opacity = '0';
            vehiculoSection.style.transform = 'scale(0.96)';
            vehiculoSection.style.filter = 'blur(4px)';
            setTimeout(function() {
                vehiculoSection.style.display = 'none';
                vehiculoSection.style.overflow = '';
                vehiculoSection.style.filter = '';
                vehiculoSection.style.transform = '';
            }, 300);
            vehiculoSelect.value = '';
            var kmInput = document.getElementById('kilometraje_servicio');
            if (kmInput) kmInput.value = '';
        }

        if (vehiculoAddBtn) {
            vehiculoAddBtn.addEventListener('click', function() {
                this.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
                this.style.opacity = '0';
                this.style.transform = 'scale(0.97)';
                var addBtn = this;
                setTimeout(function() {
                    addBtn.style.display = 'none';
                    addBtn.style.opacity = '';
                    addBtn.style.transform = '';
                    vehiculoCloseBtn.style.display = '';
                    expandirCampos(false);
                }, 200);
            });
        }

        if (vehiculoCloseBtn) {
            vehiculoCloseBtn.addEventListener('click', function() {
                colapsarCampos();
                setTimeout(function() {
                    vehiculoAddBtn.style.display = '';
                    vehiculoAddBtn.style.opacity = '0';
                    vehiculoAddBtn.style.transform = 'translateY(8px) scale(0.98)';
                    vehiculoAddBtn.style.filter = 'blur(3px)';
                    requestAnimationFrame(function() {
                        vehiculoAddBtn.style.transition =
                            'opacity 0.4s cubic-bezier(0, 0, 0.2, 1), ' +
                            'transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), ' +
                            'filter 0.35s cubic-bezier(0, 0, 0.2, 1)';
                        vehiculoAddBtn.style.opacity = '1';
                        vehiculoAddBtn.style.transform = 'translateY(0) scale(1)';
                        vehiculoAddBtn.style.filter = 'blur(0px)';
                        setTimeout(function() {
                            vehiculoAddBtn.style.transition = '';
                            vehiculoAddBtn.style.filter = '';
                            vehiculoAddBtn.style.transform = '';
                        }, 420);
                    });
                }, 280);
            });
        }

        // =============================================
        // BUSQUEDA AJAX DE CLIENTES
        // =============================================
        var clientePage = 0;
        var clienteQuery = '';
        var clienteTotalElements = 0;

        function buscarClientes(query, page, append) {
            clienteQuery = query;
            clientePage = page;

            if (!append) {
                clienteDropdown.innerHTML = '<div class="ajax-select-loading">Buscando clientes...</div>';
                abrirDropdown(clienteDropdown, clienteToggle);
            }

            fetch('/api/clientes/buscar?q=' + encodeURIComponent(query) + '&page=' + page + '&size=20', {
                headers: { 'Accept': 'application/json' }
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var items = data.content || [];
                clienteTotalElements = data.totalElements || 0;

                if (!append) clienteDropdown.innerHTML = '';

                if (items.length === 0 && !append) {
                    clienteDropdown.innerHTML =
                        '<div class="ajax-select-empty">' +
                            '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>' +
                            'No se encontraron clientes' +
                        '</div>';
                    return;
                }

                // Header con contador y boton cerrar
                if (!append) {
                    var header = document.createElement('div');
                    header.className = 'ajax-select-dropdown-header';
                    header.innerHTML =
                        '<span class="ajax-select-dropdown-count">' + clienteTotalElements + ' clientes encontrados</span>' +
                        '<button type="button" class="ajax-select-dropdown-close" title="Cerrar">' + closeSvg + '</button>';
                    header.querySelector('.ajax-select-dropdown-close').addEventListener('click', function(e) {
                        e.stopPropagation();
                        cerrarDropdown(clienteDropdown, clienteToggle);
                    });
                    clienteDropdown.appendChild(header);
                }

                items.forEach(function(c) {
                    var div = document.createElement('div');
                    div.className = 'ajax-select-option';
                    div.dataset.id = c.cliente_id;
                    div.innerHTML =
                        '<span class="ajax-select-option-main">' + escapeHtml(c.nombre) + '</span>' +
                        '<span class="ajax-select-option-sub">' + escapeHtml(c.cedula_ruc || '') + '</span>';
                    div.addEventListener('click', function() {
                        seleccionarCliente(c.cliente_id, c.nombre, c.cedula_ruc);
                    });
                    clienteDropdown.appendChild(div);
                });

                // Eliminar boton anterior de "cargar mas"
                var existingMore = clienteDropdown.querySelector('.ajax-select-more');
                if (existingMore) existingMore.remove();

                if (!data.last) {
                    var moreBtn = document.createElement('button');
                    moreBtn.type = 'button';
                    moreBtn.className = 'ajax-select-more';
                    moreBtn.innerHTML = arrowDownSvg + ' Cargar mas resultados...';
                    moreBtn.addEventListener('click', function(e) {
                        e.stopPropagation();
                        buscarClientes(clienteQuery, clientePage + 1, true);
                    });
                    clienteDropdown.appendChild(moreBtn);
                }
            })
            .catch(function() {
                clienteDropdown.innerHTML = '<div class="ajax-select-empty">Error al buscar</div>';
            });
        }

        function seleccionarCliente(id, nombre, cedula) {
            clienteIdInput.value = id;
            cerrarDropdown(clienteDropdown, clienteToggle);
            clienteSearchInput.closest('.ajax-select-input-wrap').style.display = 'none';
            clienteSelected.style.display = '';
            clienteSelectedName.textContent = nombre + (cedula ? ' — ' + cedula : '');
            cargarVehiculos(id);
        }

        function limpiarCliente() {
            clienteIdInput.value = '';
            clienteSearchInput.value = '';
            clienteSearchInput.closest('.ajax-select-input-wrap').style.display = '';
            clienteSelected.style.display = 'none';
            cerrarDropdown(clienteDropdown, clienteToggle);
            vehiculoSelect.innerHTML = '<option value="">Seleccione primero un cliente</option>';
            clienteSearchInput.focus();
        }

        var debouncedClientSearch = debounce(function() {
            var q = clienteSearchInput.value.trim();
            buscarClientes(q, 0, false);
        }, 350);

        if (clienteSearchInput) {
            clienteSearchInput.addEventListener('input', debouncedClientSearch);
            clienteSearchInput.addEventListener('focus', function() {
                if (!clienteIdInput.value && !isDropdownVisible(clienteDropdown)) {
                    buscarClientes(this.value.trim(), 0, false);
                }
            });
            clienteSearchInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    buscarClientes(this.value.trim(), 0, false);
                }
                if (e.key === 'Escape') {
                    cerrarDropdown(clienteDropdown, clienteToggle);
                }
            });
        }

        if (clienteSearchBtn) {
            clienteSearchBtn.addEventListener('click', function(e) {
                e.preventDefault();
                buscarClientes(clienteSearchInput.value.trim(), 0, false);
            });
        }

        // Toggle: abre o cierra el dropdown
        if (clienteToggle) {
            clienteToggle.addEventListener('click', function(e) {
                e.preventDefault();
                if (isDropdownVisible(clienteDropdown)) {
                    cerrarDropdown(clienteDropdown, clienteToggle);
                } else {
                    buscarClientes(clienteSearchInput.value.trim(), 0, false);
                }
            });
        }

        if (clienteClear) {
            clienteClear.addEventListener('click', limpiarCliente);
        }

        // Cerrar dropdown al hacer clic fuera
        document.addEventListener('click', function(e) {
            if (clienteDropdown && !e.target.closest('#clienteSelectWrap')) {
                cerrarDropdown(clienteDropdown, clienteToggle);
            }
        });

        // =============================================
        // CASCADA CLIENTE → VEHICULOS (via FETCH)
        // =============================================
        function cargarVehiculos(clienteId) {
            if (!clienteId) {
                vehiculoSelect.innerHTML = '<option value="">Seleccione primero un cliente</option>';
                return;
            }
            vehiculoSelect.innerHTML = '<option value="">Cargando vehiculos...</option>';
            fetch('/api/clientes/' + clienteId + '/vehiculos', {
                headers: { 'Accept': 'application/json' }
            })
            .then(function(response) {
                if (!response.ok) throw new Error('Error ' + response.status);
                return response.json();
            })
            .then(function(vehiculos) {
                if (!vehiculos || vehiculos.length === 0) {
                    vehiculoSelect.innerHTML = '<option value="">Este cliente no tiene vehiculos registrados</option>';
                    return;
                }
                var html = '<option value="">Seleccione un vehiculo</option>';
                vehiculos.forEach(function(v) {
                    var label = (v.marca || '') + ' ' + (v.modelo || '');
                    if (v.placa) label += ' (' + v.placa + ')';
                    if (v.anio) label += ' - ' + v.anio;
                    html += '<option value="' + v.vehiculo_id + '">' + escapeHtml(label.trim()) + '</option>';
                });
                vehiculoSelect.innerHTML = html;
            })
            .catch(function(err) {
                console.error('Error cargando vehiculos:', err);
                vehiculoSelect.innerHTML = '<option value="">Error al cargar vehiculos</option>';
            });
        }

        // =============================================
        // BUSQUEDA AJAX DE PRODUCTOS (en filas de detalle)
        // =============================================
        function crearBuscadorProducto(fila, idx) {
            var wrap = fila.querySelector('.detalle-producto-search-wrap');
            var searchInput = wrap.querySelector('.detalle-prod-search');
            var searchBtn = wrap.querySelector('.detalle-producto-search-btn');
            var toggleBtn = wrap.querySelector('.detalle-producto-toggle');
            var dropdown = wrap.querySelector('.detalle-producto-dropdown');
            var hiddenInput = fila.querySelector('.detalle-producto-id');
            var precioInput = fila.querySelector('.detalle-precio');
            var selectedDiv = fila.querySelector('.detalle-producto-selected');
            var selectedName = selectedDiv.querySelector('span');
            var clearBtn = selectedDiv.querySelector('.detalle-producto-clear');

            var prodPage = 0;
            var prodQuery = '';

            function buscarProductos(query, page, append) {
                prodQuery = query;
                prodPage = page;

                // Subir z-index de la fila activa
                var todasFilas = detallesContainer.querySelectorAll('.detalle-fila');
                todasFilas.forEach(function(f) { f.classList.remove('dropdown-activo'); });
                fila.classList.add('dropdown-activo');

                if (!append) {
                    dropdown.innerHTML = '<div class="ajax-select-loading">Buscando productos...</div>';
                    abrirDropdown(dropdown, toggleBtn);
                }

                fetch('/api/productos/buscar?q=' + encodeURIComponent(query) + '&page=' + page + '&size=20', {
                    headers: { 'Accept': 'application/json' }
                })
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    var items = data.content || [];
                    var totalElements = data.totalElements || 0;

                    if (!append) dropdown.innerHTML = '';

                    if (items.length === 0 && !append) {
                        dropdown.innerHTML =
                            '<div class="ajax-select-empty">' +
                                '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>' +
                                'No se encontraron productos' +
                            '</div>';
                        return;
                    }

                    // Header con contador y boton cerrar
                    if (!append) {
                        var header = document.createElement('div');
                        header.className = 'ajax-select-dropdown-header';
                        header.innerHTML =
                            '<span class="ajax-select-dropdown-count">' + totalElements + ' productos encontrados</span>' +
                            '<button type="button" class="ajax-select-dropdown-close" title="Cerrar">' + closeSvg + '</button>';
                        header.querySelector('.ajax-select-dropdown-close').addEventListener('click', function(e) {
                            e.stopPropagation();
                            cerrarDropdown(dropdown, toggleBtn);
                            fila.classList.remove('dropdown-activo');
                        });
                        dropdown.appendChild(header);
                    }

                    items.forEach(function(p) {
                        var div = document.createElement('div');
                        div.className = 'ajax-select-option';
                        div.dataset.id = p.producto_id;
                        div.dataset.precio = p.precio_venta || 0;
                        var label = escapeHtml(p.nombre || '');
                        var sub = escapeHtml(p.categoria || '');
                        if (p.precio_venta) sub += ' — $' + parseFloat(p.precio_venta).toLocaleString('es-CO');
                        div.innerHTML =
                            '<span class="ajax-select-option-main">' + label + '</span>' +
                            '<span class="ajax-select-option-sub">' + sub + '</span>';
                        div.addEventListener('click', function() {
                            seleccionarProducto(p);
                        });
                        dropdown.appendChild(div);
                    });

                    var existingMore = dropdown.querySelector('.ajax-select-more');
                    if (existingMore) existingMore.remove();

                    if (!data.last) {
                        var moreBtn = document.createElement('button');
                        moreBtn.type = 'button';
                        moreBtn.className = 'ajax-select-more';
                        moreBtn.innerHTML = arrowDownSvg + ' Cargar mas...';
                        moreBtn.addEventListener('click', function(e) {
                            e.stopPropagation();
                            buscarProductos(prodQuery, prodPage + 1, true);
                        });
                        dropdown.appendChild(moreBtn);
                    }
                })
                .catch(function() {
                    dropdown.innerHTML = '<div class="ajax-select-empty">Error al buscar</div>';
                });
            }

            function seleccionarProducto(p) {
                hiddenInput.value = p.producto_id;
                cerrarDropdown(dropdown, toggleBtn);
                fila.classList.remove('dropdown-activo');
                searchInput.style.display = 'none';
                wrap.querySelector('.detalle-producto-actions').style.display = 'none';
                selectedDiv.style.display = '';
                var nombre = p.nombre || '';
                if (p.categoria) nombre += ' — ' + p.categoria;
                selectedName.textContent = nombre;
                if (p.precio_venta) {
                    precioInput.value = parseFloat(p.precio_venta).toFixed(2);
                }
                recalcularTotal();
            }

            function limpiarProducto() {
                hiddenInput.value = '';
                searchInput.value = '';
                searchInput.style.display = '';
                wrap.querySelector('.detalle-producto-actions').style.display = '';
                selectedDiv.style.display = 'none';
                cerrarDropdown(dropdown, toggleBtn);
                fila.classList.remove('dropdown-activo');
                precioInput.value = '';
                recalcularTotal();
                searchInput.focus();
            }

            var debouncedSearch = debounce(function() {
                buscarProductos(searchInput.value.trim(), 0, false);
            }, 350);

            searchInput.addEventListener('input', debouncedSearch);
            searchInput.addEventListener('focus', function() {
                if (!hiddenInput.value && !isDropdownVisible(dropdown)) {
                    buscarProductos(this.value.trim(), 0, false);
                }
            });
            searchInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    buscarProductos(this.value.trim(), 0, false);
                }
                if (e.key === 'Escape') {
                    cerrarDropdown(dropdown, toggleBtn);
                    fila.classList.remove('dropdown-activo');
                }
            });

            searchBtn.addEventListener('click', function(e) {
                e.preventDefault();
                buscarProductos(searchInput.value.trim(), 0, false);
            });

            // Toggle: abre o cierra
            toggleBtn.addEventListener('click', function(e) {
                e.preventDefault();
                if (isDropdownVisible(dropdown)) {
                    cerrarDropdown(dropdown, toggleBtn);
                    fila.classList.remove('dropdown-activo');
                } else {
                    buscarProductos(searchInput.value.trim(), 0, false);
                }
            });

            clearBtn.addEventListener('click', limpiarProducto);

            // Cerrar dropdown al clic fuera
            document.addEventListener('click', function(e) {
                if (!wrap.contains(e.target)) {
                    cerrarDropdown(dropdown, toggleBtn);
                    fila.classList.remove('dropdown-activo');
                }
            });
        }

        // =============================================
        // FILAS DE DETALLE DINAMICAS
        // =============================================
        if (addDetalleBtn) {
            addDetalleBtn.addEventListener('click', function(e) {
                e.preventDefault();
                agregarFilaDetalle();
            });
        }

        function agregarFilaDetalle() {
            var fila = document.createElement('div');
            fila.className = 'detalle-fila';
            fila.style.opacity = '0';
            fila.style.transform = 'translateY(-10px)';

            fila.innerHTML =
                '<input type="hidden" name="detalles[' + detalleIndex + '].tipoItem" value="PRODUCTO" />' +
                '<div class="detalle-fila-grid">' +
                    '<div class="form-group">' +
                        '<label class="form-label">Producto</label>' +
                        '<div class="detalle-producto-search-wrap">' +
                            '<input type="text" class="form-input detalle-prod-search" placeholder="Buscar producto..." autocomplete="off">' +
                            '<div class="detalle-producto-actions">' +
                                '<button type="button" class="detalle-producto-search-btn" title="Buscar">' + searchSvg + '</button>' +
                                '<button type="button" class="detalle-producto-toggle" title="Abrir/Cerrar lista">' + chevronSvg + '</button>' +
                            '</div>' +
                            '<div class="detalle-producto-dropdown"></div>' +
                            '<input type="hidden" name="detalles[' + detalleIndex + '].producto_id" class="detalle-producto-id" required>' +
                            '<div class="detalle-producto-selected" style="display:none;">' +
                                '<div class="detalle-producto-selected-icon">' + packageSvg + '</div>' +
                                '<span></span>' +
                                '<button type="button" class="detalle-producto-clear" title="Cambiar">' + closeSvg + '</button>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<label class="form-label">Cantidad</label>' +
                        '<input type="number" name="detalles[' + detalleIndex + '].cantidad" class="form-input detalle-cantidad" value="1" min="1" step="1" required />' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<label class="form-label">Precio Unit.</label>' +
                        '<input type="number" name="detalles[' + detalleIndex + '].precio_unitario_congelado" class="form-input detalle-precio" step="0.01" min="0" placeholder="Auto" />' +
                    '</div>' +
                    '<div class="form-group detalle-subtotal-wrap">' +
                        '<label class="form-label">Subtotal</label>' +
                        '<div class="detalle-subtotal">$ 0.00</div>' +
                    '</div>' +
                    '<div class="detalle-remove-wrap">' +
                        '<button type="button" class="btn btn-danger btn-sm btn-remove-detalle">' + removeSvg + '</button>' +
                    '</div>' +
                '</div>';

            detallesContainer.appendChild(fila);

            // Inicializar buscador de producto para esta fila
            crearBuscadorProducto(fila, detalleIndex);

            detalleIndex++;

            // Animacion de entrada
            requestAnimationFrame(function() {
                fila.style.transition = 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)';
                fila.style.opacity = '1';
                fila.style.transform = 'translateY(0)';
            });

            var cantidadInput = fila.querySelector('.detalle-cantidad');
            var precioInput = fila.querySelector('.detalle-precio');

            if (window.SionNumericFilter) {
                SionNumericFilter(cantidadInput);
                SionNumericFilter(precioInput);
            }

            cantidadInput.addEventListener('input', recalcularTotal);
            precioInput.addEventListener('input', recalcularTotal);

            fila.querySelector('.detalle-prod-search').focus();
        }

        // =============================================
        // ELIMINAR FILA (event delegation)
        // =============================================
        detallesContainer.addEventListener('click', function(e) {
            var removeBtn = e.target.closest('.btn-remove-detalle');
            if (!removeBtn) return;
            e.preventDefault();
            var fila = removeBtn.closest('.detalle-fila');
            if (!fila) return;
            fila.style.transition = 'all 0.2s ease-out';
            fila.style.opacity = '0';
            fila.style.transform = 'translateX(-10px)';
            setTimeout(function() {
                fila.remove();
                reindexarDetalles();
                recalcularTotal();
            }, 200);
        });

        // =============================================
        // REINDEXAR
        // =============================================
        function reindexarDetalles() {
            var filas = detallesContainer.querySelectorAll('.detalle-fila');
            filas.forEach(function(fila, idx) {
                fila.querySelectorAll('[name]').forEach(function(input) {
                    var name = input.getAttribute('name');
                    input.setAttribute('name', name.replace(/detalles\[\d+\]/, 'detalles[' + idx + ']'));
                });
            });
            detalleIndex = filas.length;
        }

        // =============================================
        // RECALCULAR TOTAL
        // =============================================
        function recalcularTotal() {
            var total = 0;
            detallesContainer.querySelectorAll('.detalle-fila').forEach(function(fila) {
                var cantidad = parseFloat(fila.querySelector('.detalle-cantidad').value) || 0;
                var precio = parseFloat(fila.querySelector('.detalle-precio').value) || 0;
                var subtotal = cantidad * precio;
                var subtotalEl = fila.querySelector('.detalle-subtotal');
                if (subtotalEl) {
                    subtotalEl.textContent = '$ ' + subtotal.toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                }
                total += subtotal;
            });
            if (totalDisplay) {
                totalDisplay.textContent = '$ ' + total.toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            }
        }

        // =============================================
        // VALIDACION
        // =============================================
        if (form) {
            form.addEventListener('submit', function(e) {
                var ok = true;
                var errores = [];

                if (!tipoInput.value) {
                    ok = false;
                    errores.push('Seleccione un tipo de servicio');
                    tipoToggle.classList.add('error');
                } else {
                    tipoToggle.classList.remove('error');
                }

                if (!clienteIdInput.value) {
                    ok = false;
                    errores.push('Seleccione un cliente');
                    var wrap = document.getElementById('clienteSelectWrap');
                    if (wrap) wrap.classList.add('error');
                } else {
                    var wrap = document.getElementById('clienteSelectWrap');
                    if (wrap) wrap.classList.remove('error');
                }

                if (tipoActual === 'MANO_DE_OBRA' && !vehiculoSelect.value) {
                    ok = false;
                    vehiculoSelect.classList.add('error');
                    errores.push('Seleccione un vehiculo');
                } else {
                    vehiculoSelect.classList.remove('error');
                }

                var filas = detallesContainer.querySelectorAll('.detalle-fila');
                if (filas.length === 0) {
                    ok = false;
                    errores.push('Agregue al menos un producto o item');
                }

                filas.forEach(function(fila) {
                    var productoId = fila.querySelector('.detalle-producto-id');
                    var cantidad = fila.querySelector('.detalle-cantidad');

                    if (!productoId || !productoId.value) {
                        ok = false;
                        var searchWrap = fila.querySelector('.detalle-producto-search-wrap');
                        if (searchWrap) searchWrap.classList.add('error');
                    } else {
                        var searchWrap = fila.querySelector('.detalle-producto-search-wrap');
                        if (searchWrap) searchWrap.classList.remove('error');
                    }

                    if (!cantidad.value || parseFloat(cantidad.value) <= 0) {
                        ok = false;
                        cantidad.classList.add('error');
                    } else {
                        cantidad.classList.remove('error');
                    }
                });

                if (!ok) {
                    e.preventDefault();
                    if (typeof showToast === 'function') {
                        showToast(errores[0] || 'Por favor corrige los errores', 'error');
                    }
                }
            });
        }

        // Agregar primera fila por defecto
        agregarFilaDetalle();
    });

    function escapeHtml(text) {
        return SionUtils.esc(text, '');
    }

    function escapeAttr(text) {
        return SionUtils.escAttr(text);
    }
})();
