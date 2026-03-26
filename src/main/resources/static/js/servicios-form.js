/**
 * SionTrack - Servicios Form
 *
 * Toggle tipo de servicio (PRODUCTO / MANO_DE_OBRA)
 * Vehiculos: se cargan via FETCH a /api/clientes/{id}/vehiculos
 * Productos: se leen del DOM (divs ocultos con data-attributes)
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

        // Leer productos del DOM
        var productosData = leerProductosDelDOM();

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

                // Actualizar visual
                tipoBtns.forEach(function(b) { b.classList.remove('activo'); });
                this.classList.add('activo');

                // Actualizar input oculto
                tipoInput.value = valor;
                tipoActual = valor;

                // Configurar campos segun tipo
                configurarCamposVehiculo();
            });
        });

        function configurarCamposVehiculo() {
            if (tipoActual === 'MANO_DE_OBRA') {
                // Vehiculo y km son obligatorios, siempre visibles
                vehiculoWrap.style.display = '';
                vehiculoAddBtn.style.display = 'none';
                vehiculoCloseBtn.style.display = 'none';
                expandirCampos(true);
                vehiculoLabel.classList.add('required');
                kmLabel.classList.add('required');
                vehiculoSelect.setAttribute('required', '');
            } else if (tipoActual === 'PRODUCTO') {
                // Vehiculo y km son opcionales
                vehiculoWrap.style.display = '';
                vehiculoLabel.classList.remove('required');
                kmLabel.classList.remove('required');
                vehiculoSelect.removeAttribute('required');

                // Resetear: ocultar campos, mostrar boton de añadir
                vehiculoVisible = false;
                vehiculoSection.style.display = 'none';
                vehiculoSection.style.opacity = '';
                vehiculoSection.style.transform = '';
                vehiculoSection.style.maxHeight = '';
                vehiculoAddBtn.style.display = '';
                vehiculoCloseBtn.style.display = 'none';

                // Limpiar valores
                vehiculoSelect.value = '';
                var kmInput = document.getElementById('kilometraje_servicio');
                if (kmInput) kmInput.value = '';
            } else {
                // Sin tipo seleccionado — ocultar todo
                vehiculoWrap.style.display = 'none';
                vehiculoVisible = false;
                vehiculoLabel.classList.remove('required');
                kmLabel.classList.remove('required');
                vehiculoSelect.removeAttribute('required');
            }
        }

        // Expandir campos con animacion
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

        // Colapsar campos con animacion elegante
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

            // Limpiar valores
            vehiculoSelect.value = '';
            var kmInput = document.getElementById('kilometraje_servicio');
            if (kmInput) kmInput.value = '';
        }

        // Boton "Añadir vehiculo" (modo PRODUCTO)
        if (vehiculoAddBtn) {
            vehiculoAddBtn.addEventListener('click', function() {
                // Ocultar boton de añadir con animacion
                this.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
                this.style.opacity = '0';
                this.style.transform = 'scale(0.97)';

                var addBtn = this;
                setTimeout(function() {
                    addBtn.style.display = 'none';
                    addBtn.style.opacity = '';
                    addBtn.style.transform = '';

                    // Mostrar campos y boton cerrar
                    vehiculoCloseBtn.style.display = '';
                    expandirCampos(false);
                }, 200);
            });
        }

        // Boton "Quitar vehiculo" (X en header de campos)
        if (vehiculoCloseBtn) {
            vehiculoCloseBtn.addEventListener('click', function() {
                colapsarCampos();

                // Materializar boton de añadir tras colapso
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

                        // Limpiar estilos inline despues de la animacion
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
        // CASCADA CLIENTE → VEHICULOS (via FETCH)
        // =============================================
        if (clienteSelect) {
            clienteSelect.addEventListener('change', function() {
                cargarVehiculos(this.value);
            });
        }

        function cargarVehiculos(clienteId) {
            if (!clienteId) {
                vehiculoSelect.innerHTML = '<option value="">Seleccione primero un cliente</option>';
                return;
            }

            vehiculoSelect.innerHTML = '<option value="">Cargando vehículos...</option>';

            fetch('/api/clientes/' + clienteId + '/vehiculos', {
                headers: { 'Accept': 'application/json' }
            })
            .then(function(response) {
                if (!response.ok) throw new Error('Error ' + response.status);
                return response.json();
            })
            .then(function(vehiculos) {
                if (!vehiculos || vehiculos.length === 0) {
                    vehiculoSelect.innerHTML = '<option value="">Este cliente no tiene vehículos registrados</option>';
                    return;
                }

                var html = '<option value="">Seleccione un vehículo</option>';
                vehiculos.forEach(function(v) {
                    var label = (v.marca || '') + ' ' + (v.modelo || '');
                    if (v.placa) label += ' (' + v.placa + ')';
                    if (v.anio) label += ' - ' + v.anio;
                    html += '<option value="' + v.vehiculo_id + '">' + escapeHtml(label.trim()) + '</option>';
                });

                vehiculoSelect.innerHTML = html;
            })
            .catch(function(err) {
                console.error('Error cargando vehículos:', err);
                vehiculoSelect.innerHTML = '<option value="">Error al cargar vehículos</option>';
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

            // Opciones de productos
            var productosOptions = '<option value="">Seleccione producto</option>';
            productosData.forEach(function(p) {
                var label = p.nombre;
                if (p.marca) label += ' - ' + p.marca;
                productosOptions += '<option value="' + p.id + '" data-precio="' + p.precio + '" data-categoria="' + escapeAttr(p.categoria) + '">' + escapeHtml(label) + '</option>';
            });

            fila.innerHTML =
                '<input type="hidden" name="detalles[' + detalleIndex + '].tipoItem" value="PRODUCTO" />' +
                '<div class="detalle-fila-grid">' +
                    '<div class="form-group">' +
                        '<label class="form-label">Producto</label>' +
                        '<select name="detalles[' + detalleIndex + '].producto_id" class="form-select detalle-producto" required>' +
                            productosOptions +
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

            // Inicializar custom selects de la fila
            if (window.SionSelect) {
                fila.querySelectorAll('select.form-select').forEach(function(s) {
                    SionSelect.init(s);
                });
            }

            // Animacion
            requestAnimationFrame(function() {
                fila.style.transition = 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)';
                fila.style.opacity = '1';
                fila.style.transform = 'translateY(0)';
            });

            var productoSelect = fila.querySelector('.detalle-producto');
            var cantidadInput = fila.querySelector('.detalle-cantidad');
            var precioInput = fila.querySelector('.detalle-precio');

            // Al seleccionar producto → auto-precio
            productoSelect.addEventListener('change', function() {
                var selected = this.options[this.selectedIndex];
                var precio = selected.getAttribute('data-precio');
                if (precio) {
                    precioInput.value = parseFloat(precio).toFixed(2);
                }
                recalcularTotal();
            });

            cantidadInput.addEventListener('input', recalcularTotal);
            precioInput.addEventListener('input', recalcularTotal);

            productoSelect.focus();
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

                // Tipo de servicio obligatorio
                if (!tipoInput.value) {
                    ok = false;
                    errores.push('Seleccione un tipo de servicio');
                    tipoToggle.classList.add('error');
                } else {
                    tipoToggle.classList.remove('error');
                }

                if (!clienteSelect.value) {
                    ok = false;
                    clienteSelect.classList.add('error');
                    errores.push('Seleccione un cliente');
                } else {
                    clienteSelect.classList.remove('error');
                }

                // Vehiculo obligatorio solo en MANO_DE_OBRA
                if (tipoActual === 'MANO_DE_OBRA' && !vehiculoSelect.value) {
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

                filas.forEach(function(fila) {
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

        // Agregar primera fila por defecto
        agregarFilaDetalle();
    });

    // =============================================
    // LEER PRODUCTOS DEL DOM
    // =============================================
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
        return SionUtils.esc(text, '');
    }

    function escapeAttr(text) {
        return SionUtils.escAttr(text);
    }
})();
