/**
 * SionTrack - Proveedores Teléfono
 * Selector de País con Búsqueda (mismo estilo que clientes)
 * Formato: "+57 3183260547"
 */
(function() {
    'use strict';

    var PAISES = [
        { grupo: 'América del Norte', codigo: '+1', nombre: 'Estados Unidos' },
        { grupo: 'América del Norte', codigo: '+1', nombre: 'Canadá' },
        { grupo: 'América del Norte', codigo: '+52', nombre: 'México' },
        { grupo: 'América Central', codigo: '+502', nombre: 'Guatemala' },
        { grupo: 'América Central', codigo: '+503', nombre: 'El Salvador' },
        { grupo: 'América Central', codigo: '+504', nombre: 'Honduras' },
        { grupo: 'América Central', codigo: '+505', nombre: 'Nicaragua' },
        { grupo: 'América Central', codigo: '+506', nombre: 'Costa Rica' },
        { grupo: 'América Central', codigo: '+507', nombre: 'Panamá' },
        { grupo: 'América del Sur', codigo: '+51', nombre: 'Perú' },
        { grupo: 'América del Sur', codigo: '+54', nombre: 'Argentina' },
        { grupo: 'América del Sur', codigo: '+55', nombre: 'Brasil' },
        { grupo: 'América del Sur', codigo: '+56', nombre: 'Chile' },
        { grupo: 'América del Sur', codigo: '+57', nombre: 'Colombia' },
        { grupo: 'América del Sur', codigo: '+58', nombre: 'Venezuela' },
        { grupo: 'América del Sur', codigo: '+591', nombre: 'Bolivia' },
        { grupo: 'América del Sur', codigo: '+593', nombre: 'Ecuador' },
        { grupo: 'América del Sur', codigo: '+595', nombre: 'Paraguay' },
        { grupo: 'América del Sur', codigo: '+598', nombre: 'Uruguay' },
        { grupo: 'Europa', codigo: '+34', nombre: 'España' },
        { grupo: 'Europa', codigo: '+33', nombre: 'Francia' },
        { grupo: 'Europa', codigo: '+39', nombre: 'Italia' },
        { grupo: 'Europa', codigo: '+44', nombre: 'Reino Unido' },
        { grupo: 'Europa', codigo: '+49', nombre: 'Alemania' },
        { grupo: 'Europa', codigo: '+351', nombre: 'Portugal' }
    ];

    function generarListaHTML(codigoSeleccionado) {
        var html = '';
        var grupoActual = '';
        
        PAISES.forEach(function(pais) {
            if (pais.grupo !== grupoActual) {
                grupoActual = pais.grupo;
                html += '<div class="pais-grupo">' + pais.grupo + '</div>';
            }
            var sel = pais.codigo === codigoSeleccionado ? ' seleccionado' : '';
            html += '<div class="pais-opcion' + sel + '" data-codigo="' + pais.codigo + '" data-nombre="' + pais.nombre + '">' +
                '<span class="op-codigo">' + pais.codigo + '</span>' +
                '<span class="op-nombre">' + pais.nombre + '</span>' +
            '</div>';
        });
        
        html += '<div class="pais-opcion otro-opcion" data-codigo="otro" data-nombre="Otro">' +
            '<span class="op-codigo">+ Otro</span>' +
            '<span class="op-nombre">Código personalizado</span>' +
        '</div>';
        
        return html;
    }

    function buscarNombrePais(codigo) {
        for (var i = 0; i < PAISES.length; i++) {
            if (PAISES[i].codigo === codigo) return PAISES[i].nombre;
        }
        return null;
    }

    function initFila(fila) {
        var selector = fila.querySelector('.pais-selector');
        var boton = fila.querySelector('.pais-boton');
        var dropdown = fila.querySelector('.pais-dropdown');
        var lista = fila.querySelector('.pais-lista');
        var busqueda = fila.querySelector('.pais-busqueda input');
        var valorInput = fila.querySelector('.pais-valor');
        var prefijoContainer = fila.querySelector('.prefijo-container');
        var prefijoInput = fila.querySelector('.prefijo-input');
        var btnVolver = fila.querySelector('.btn-volver-pais');
        var numeroInput = fila.querySelector('.numero-input');

        if (!selector || !boton || !lista || !numeroInput) return;

        var codigoActual = '+57';
        var esOtro = false;

        lista.innerHTML = generarListaHTML(codigoActual);

        // Parsear valor existente
        var val = numeroInput.value;
        if (val && val.indexOf(' ') > 0) {
            var partes = val.split(' ');
            var codigo = partes[0];
            var numero = partes.slice(1).join('');

            var nombrePais = buscarNombrePais(codigo);
            if (nombrePais) {
                codigoActual = codigo;
                esOtro = false;
                boton.querySelector('.codigo-mostrar').textContent = codigo;
                boton.querySelector('.pais-nombre-mostrar').textContent = nombrePais;
                valorInput.value = codigo;
                prefijoContainer.classList.remove('visible');
                fila.classList.remove('modo-otro');
            } else {
                codigoActual = codigo;
                esOtro = true;
                fila.classList.add('modo-otro');
                prefijoContainer.classList.add('visible');
                prefijoInput.value = codigo;
                valorInput.value = 'otro';
            }
            numeroInput.value = numero;
            lista.innerHTML = generarListaHTML(esOtro ? 'otro' : codigoActual);
        }

        function getCodigoFinal() {
            if (esOtro) {
                return prefijoInput.value.trim() || '+';
            }
            return codigoActual;
        }

        fila.getCodigoFinal = getCodigoFinal;

        // Botón volver al selector de país
        if (btnVolver) {
            btnVolver.addEventListener('click', function(e) {
                e.preventDefault();
                esOtro = false;
                fila.classList.remove('modo-otro');
                prefijoContainer.classList.remove('visible');
                codigoActual = '+57';
                boton.querySelector('.codigo-mostrar').textContent = '+57';
                boton.querySelector('.pais-nombre-mostrar').textContent = 'Colombia';
                valorInput.value = '+57';
                lista.innerHTML = generarListaHTML('+57');
            });
        }

        // Abrir/cerrar dropdown
        boton.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var abierto = selector.classList.toggle('abierto');
            if (abierto && busqueda) {
                busqueda.value = '';
                busqueda.focus();
                filtrar('');
            }
        });

        // Filtrar países
        function filtrar(texto) {
            texto = texto.toLowerCase();
            lista.querySelectorAll('.pais-opcion').forEach(function(op) {
                var nombre = (op.dataset.nombre || '').toLowerCase();
                var codigo = (op.dataset.codigo || '').toLowerCase();
                var visible = nombre.indexOf(texto) !== -1 || codigo.indexOf(texto) !== -1;
                op.style.display = visible ? '' : 'none';
            });
            lista.querySelectorAll('.pais-grupo').forEach(function(g) {
                var next = g.nextElementSibling;
                var algunoVisible = false;
                while (next && !next.classList.contains('pais-grupo')) {
                    if (next.style.display !== 'none') algunoVisible = true;
                    next = next.nextElementSibling;
                }
                g.style.display = algunoVisible ? '' : 'none';
            });
        }

        if (busqueda) {
            busqueda.addEventListener('input', function() {
                filtrar(this.value);
            });
            busqueda.addEventListener('click', function(e) {
                e.stopPropagation();
            });
        }

        // Seleccionar país
        lista.addEventListener('click', function(e) {
            var opcion = e.target.closest('.pais-opcion');
            if (!opcion) return;

            var codigo = opcion.dataset.codigo;
            var nombre = opcion.dataset.nombre;

            lista.querySelectorAll('.pais-opcion').forEach(function(op) {
                op.classList.remove('seleccionado');
            });
            opcion.classList.add('seleccionado');

            if (codigo === 'otro') {
                esOtro = true;
                fila.classList.add('modo-otro');
                boton.querySelector('.codigo-mostrar').textContent = '';
                boton.querySelector('.pais-nombre-mostrar').textContent = 'Otro';
                prefijoContainer.classList.add('visible');
                prefijoInput.value = '+';
                prefijoInput.focus();
                valorInput.value = 'otro';
            } else {
                esOtro = false;
                fila.classList.remove('modo-otro');
                codigoActual = codigo;
                boton.querySelector('.codigo-mostrar').textContent = codigo;
                boton.querySelector('.pais-nombre-mostrar').textContent = nombre;
                prefijoContainer.classList.remove('visible');
                valorInput.value = codigo;
            }

            selector.classList.remove('abierto');
        });

        // Cerrar al clic afuera
        document.addEventListener('click', function(e) {
            if (!selector.contains(e.target)) {
                selector.classList.remove('abierto');
            }
        });

        // Validar prefijo personalizado - Solo validar, no actualizar el botón
        if (prefijoInput) {
            prefijoInput.addEventListener('input', function() {
                var val = this.value;
                if (val && val[0] !== '+') val = '+' + val;
                val = '+' + val.slice(1).replace(/[^0-9]/g, '');
                this.value = val.substring(0, 5);
            });
        }

        // Solo números en teléfono
        numeroInput.addEventListener('input', function() {
            this.value = this.value.replace(/[^0-9]/g, '');
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        // Inicializar selector de teléfono
        var filas = document.querySelectorAll('.telefono-fila');
        filas.forEach(initFila);

        // Validar correo
        var emailInput = document.getElementById('email');
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                var val = this.value.trim();
                if (val && !val.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                    this.classList.add('error');
                    if (typeof showToast === 'function') {
                        showToast('Ingresa un correo electrónico válido', 'error');
                    }
                } else {
                    this.classList.remove('error');
                }
            });
        }

        // Submit del formulario
        var form = document.getElementById('proveedorForm');
        if (form) {
            form.addEventListener('submit', function(e) {
                var ok = true;
                var errores = [];

                // Validar nombre
                var nombre = document.getElementById('nombre');
                if (nombre && !nombre.value.trim()) {
                    ok = false;
                    nombre.classList.add('error');
                    errores.push('El nombre es obligatorio');
                } else if (nombre) {
                    nombre.classList.remove('error');
                }

                // Validar teléfono
                var telefonoFila = document.querySelector('.telefono-fila');
                var numeroInput = telefonoFila ? telefonoFila.querySelector('.numero-input') : null;
                if (numeroInput) {
                    if (!numeroInput.value.trim()) {
                        ok = false;
                        numeroInput.classList.add('error');
                        errores.push('El teléfono es obligatorio');
                    } else if (numeroInput.value.length < 7) {
                        ok = false;
                        numeroInput.classList.add('error');
                        errores.push('El teléfono debe tener al menos 7 dígitos');
                    } else {
                        numeroInput.classList.remove('error');
                        // Formatear teléfono con código de país
                        var codigo = telefonoFila.getCodigoFinal ? telefonoFila.getCodigoFinal() : '+57';
                        numeroInput.value = codigo + ' ' + numeroInput.value.replace(/[^0-9]/g, '');
                    }
                }

                // Validar email
                var email = document.getElementById('email');
                if (email) {
                    var emailVal = email.value.trim();
                    if (!emailVal) {
                        ok = false;
                        email.classList.add('error');
                        errores.push('El email es obligatorio');
                    } else if (!emailVal.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                        ok = false;
                        email.classList.add('error');
                        errores.push('El formato del email no es válido');
                    } else {
                        email.classList.remove('error');
                    }
                }

                // Validar dirección
                var direccion = document.getElementById('direccion');
                if (direccion && !direccion.value.trim()) {
                    ok = false;
                    direccion.classList.add('error');
                    errores.push('La dirección es obligatoria');
                } else if (direccion) {
                    direccion.classList.remove('error');
                }

                // Validar nombre de contacto
                var nombreContacto = document.getElementById('nombre_contacto');
                if (nombreContacto && !nombreContacto.value.trim()) {
                    ok = false;
                    nombreContacto.classList.add('error');
                    errores.push('El nombre de contacto es obligatorio');
                } else if (nombreContacto) {
                    nombreContacto.classList.remove('error');
                }

                if (!ok) {
                    e.preventDefault();
                    if (typeof showToast === 'function') {
                        showToast(errores[0] || 'Por favor corrige los errores', 'error');
                    }
                }
            });
        }
    });
})();