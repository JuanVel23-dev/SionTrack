/**
 * SionTrack - Servicios Form
 * Lógica del formulario de creación de servicios.
 *
 * Los datos de vehículos y productos se leen del DOM (div ocultos
 * renderizados por Thymeleaf con data-attributes), NO de JSON inline.
 */
(function() {
    'use strict';

    var detalleIndex = 0;

    var removeSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="18" y1="6" x2="6" y2="18"></line>' +
        '<line x1="6" y1="6" x2="18" y2="18"></line></svg>';

    document.addEventListener('DOMContentLoaded', function() {
        var clienteSelect = document.getElementById('cliente_id');
        var vehiculoSelect = document.getElementById('vehiculo_id');
        var detallesContainer = document.getElementById('detalles-container');
        var addDetalleBtn = document.getElementById('add-detalle-btn');
        var totalDisplay = document.getElementById('total-display');
        var form = document.getElementById('servicioForm');

        // ===== LEER DATOS DEL DOM =====
        var vehiculosData = leerVehiculosDelDOM();
        var productosData = leerProductosDelDOM();

        // Fecha de hoy por defecto
        var fechaInput = document.getElementById('fecha_servicio');
        if (fechaInput && !fechaInput.value) {
            fechaInput.value = new Date().toISOString().split('T')[0];
        }

        // ===== CASCADA CLIENTE → VEHÍCULOS =====
        if (clienteSelect) {
            clienteSelect.addEventListener('change', function() {
                cargarVehiculos(this.value);
            });
        }

        function cargarVehiculos(clienteId) {
            vehiculoSelect.innerHTML = '';

            if (!clienteId) {
                vehiculoSelect.innerHTML = '<option value="">Seleccione primero un cliente</option>';
                return;
            }

            // Filtrar vehículos de este cliente
            var vehiculosCliente = vehiculosData.filter(function(v) {
                return v.clienteId == clienteId;
            });

            if (vehiculosCliente.length === 0) {
                vehiculoSelect.innerHTML = '<option value="">Este cliente no tiene vehículos registrados</option>';
                return;
            }

            var html = '<option value="">Seleccione un vehículo</option>';
            vehiculosCliente.forEach(function(v) {
                var label = v.marca + ' ' + v.modelo;
                if (v.placa) label += ' (' + v.placa + ')';
                if (v.anio) label += ' - ' + v.anio;
                html += '<option value="' + v.vehiculoId + '">' + escapeHtml(label) + '</option>';
            });

            vehiculoSelect.innerHTML = html;
        }

        // ===== FILAS DE DETALLE DINÁMICAS =====
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

            // Construir opciones de productos desde los datos del DOM
            var productosOptions = '<option value="">Seleccione producto</option>';
            productosData.forEach(function(p) {
                var label = p.nombre;
                if (p.marca) label += ' - ' + p.marca;
                productosOptions += '<option value="' + p.id + '" data-precio="' + p.precio + '">' + escapeHtml(label) + '</option>';
            });

            fila.innerHTML =
                '<div class="detalle-fila-grid">' +
                    '<div class="form-group">' +
                        '<label class="form-label">Producto</label>' +
                        '<select name="detalles[' + detalleIndex + '].producto_id" class="form-select detalle-producto" required>' +
                            productosOptions +
                        '</select>' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<label class="form-label">Tipo</label>' +
                        '<select name="detalles[' + detalleIndex + '].tipoItem" class="form-select detalle-tipo">' +
                            '<option value="PRODUCTO">Producto</option>' +
                            '<option value="SERVICIO">Servicio</option>' +
                            '<option value="INSUMO">Insumo</option>' +
                            '<option value="PAQUETE">Paquete</option>' +
                        '</select>' +
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
            detalleIndex++;

            // Animación de entrada
            requestAnimationFrame(function() {
                fila.style.transition = 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)';
                fila.style.opacity = '1';
                fila.style.transform = 'translateY(0)';
            });

            // Eventos de la nueva fila
            var productoSelect = fila.querySelector('.detalle-producto');
            var tipoSelect = fila.querySelector('.detalle-tipo');
            var cantidadInput = fila.querySelector('.detalle-cantidad');
            var precioInput = fila.querySelector('.detalle-precio');

            // Al seleccionar producto → auto-llenar precio y tipo
            productoSelect.addEventListener('change', function() {
                var selected = this.options[this.selectedIndex];
                var precio = selected.getAttribute('data-precio');

                // Auto-llenar precio
                if (precio) {
                    precioInput.value = parseFloat(precio).toFixed(2);
                }

                // Auto-seleccionar tipo basado en categoría del producto
                var productoId = this.value;
                var producto = productosData.find(function(p) { return p.id == productoId; });
                if (producto && producto.categoria) {
                    var cat = producto.categoria.toLowerCase();
                    if (cat.indexOf('servicio') !== -1 || cat.indexOf('mano de obra') !== -1) {
                        tipoSelect.value = 'SERVICIO';
                    } else if (cat.indexOf('insumo') !== -1) {
                        tipoSelect.value = 'INSUMO';
                    } else if (cat.indexOf('paquete') !== -1 || cat.indexOf('combo') !== -1) {
                        tipoSelect.value = 'PAQUETE';
                    } else {
                        tipoSelect.value = 'PRODUCTO';
                    }
                }

                recalcularTotal();
            });

            cantidadInput.addEventListener('input', recalcularTotal);
            precioInput.addEventListener('input', recalcularTotal);

            // Focus en el select de producto
            productoSelect.focus();
        }

        // ===== ELIMINAR FILA DE DETALLE (event delegation) =====
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

        // ===== REINDEXAR DETALLES =====
        function reindexarDetalles() {
            var filas = detallesContainer.querySelectorAll('.detalle-fila');
            filas.forEach(function(fila, idx) {
                var inputs = fila.querySelectorAll('[name]');
                inputs.forEach(function(input) {
                    var name = input.getAttribute('name');
                    input.setAttribute('name', name.replace(/detalles\[\d+\]/, 'detalles[' + idx + ']'));
                });
            });
            detalleIndex = filas.length;
        }

        // ===== RECALCULAR TOTAL =====
        function recalcularTotal() {
            var total = 0;
            var filas = detallesContainer.querySelectorAll('.detalle-fila');

            filas.forEach(function(fila) {
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

        // ===== VALIDACIÓN AL ENVIAR =====
        if (form) {
            form.addEventListener('submit', function(e) {
                var ok = true;
                var errores = [];

                if (!clienteSelect.value) {
                    ok = false;
                    clienteSelect.classList.add('error');
                    errores.push('Seleccione un cliente');
                } else {
                    clienteSelect.classList.remove('error');
                }

                if (!vehiculoSelect.value) {
                    ok = false;
                    vehiculoSelect.classList.add('error');
                    errores.push('Seleccione un vehículo');
                } else {
                    vehiculoSelect.classList.remove('error');
                }

                var filas = detallesContainer.querySelectorAll('.detalle-fila');
                if (filas.length === 0) {
                    ok = false;
                    errores.push('Agregue al menos un producto o item');
                }

                filas.forEach(function(fila, idx) {
                    var producto = fila.querySelector('.detalle-producto');
                    var cantidad = fila.querySelector('.detalle-cantidad');

                    if (!producto.value) {
                        ok = false;
                        producto.classList.add('error');
                    } else {
                        producto.classList.remove('error');
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

        // ===== AGREGAR PRIMERA FILA POR DEFECTO =====
        agregarFilaDetalle();
    });

    // ===== FUNCIONES DE LECTURA DE DATOS DEL DOM =====

    /**
     * Lee los vehículos desde los divs ocultos que Thymeleaf renderizó
     */
    function leerVehiculosDelDOM() {
        var items = document.querySelectorAll('#vehiculos-data .vehiculo-item');
        var vehiculos = [];

        items.forEach(function(el) {
            vehiculos.push({
                clienteId: el.dataset.clienteId,
                vehiculoId: el.dataset.vehiculoId,
                marca: el.dataset.marca || '',
                modelo: el.dataset.modelo || '',
                placa: el.dataset.placa || '',
                anio: el.dataset.anio || ''
            });
        });

        return vehiculos;
    }

    /**
     * Lee los productos desde los divs ocultos que Thymeleaf renderizó
     */
    function leerProductosDelDOM() {
        var items = document.querySelectorAll('#productos-data .producto-item');
        var productos = [];

        items.forEach(function(el) {
            productos.push({
                id: el.dataset.id,
                nombre: el.dataset.nombre || '',
                marca: el.dataset.marca || '',
                precio: el.dataset.precio || '0',
                categoria: el.dataset.categoria || ''
            });
        });

        return productos;
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
})();