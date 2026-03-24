/**
 * SionTrack - Selector de Teléfono con País
 * Módulo unificado para clientes y proveedores
 * Formato: "+57 3183260547"
 *
 * Uso: SelectorTelefono.inicializarFilas('.telefono-fila')
 */
var SelectorTelefono = (function() {
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

    // Códigos ordenados de mayor a menor longitud para matching correcto
    var CODIGOS = PAISES.map(function(p) { return p.codigo.replace('+', ''); })
        .filter(function(v, i, a) { return a.indexOf(v) === i; })
        .sort(function(a, b) { return b.length - a.length; });

    var ICONO_ELIMINAR = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';

    // ============================================
    // PARSEO DE TELÉFONO
    // ============================================
    function parsearTelefono(val) {
        if (!val) return null;
        val = val.trim();

        // Formato con espacio: "+57 3183252987"
        if (val.indexOf(' ') > 0) {
            var partes = val.split(' ');
            return { codigo: partes[0], numero: partes.slice(1).join('') };
        }

        // Formato con +: "+573183252987"
        if (val.charAt(0) === '+') {
            var sinPlus = val.substring(1);
            for (var i = 0; i < CODIGOS.length; i++) {
                if (sinPlus.indexOf(CODIGOS[i]) === 0) {
                    return { codigo: '+' + CODIGOS[i], numero: sinPlus.substring(CODIGOS[i].length) };
                }
            }
            return null;
        }

        // Formato BD puro: "573183252987"
        if (/^\d+$/.test(val)) {
            for (var j = 0; j < CODIGOS.length; j++) {
                if (val.indexOf(CODIGOS[j]) === 0 && val.length > CODIGOS[j].length) {
                    return { codigo: '+' + CODIGOS[j], numero: val.substring(CODIGOS[j].length) };
                }
            }
        }

        return null;
    }

    // ============================================
    // GENERACIÓN DE HTML
    // ============================================
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

    // ============================================
    // FILTRADO DE LISTA
    // ============================================
    function filtrarLista(lista, termino) {
        termino = termino.toLowerCase();
        lista.querySelectorAll('.pais-opcion').forEach(function(op) {
            var codigo = (op.dataset.codigo || '').toLowerCase();
            var nombre = (op.dataset.nombre || '').toLowerCase();
            var coincide = codigo.indexOf(termino) !== -1 || nombre.indexOf(termino) !== -1;
            op.style.display = (!coincide && termino !== '') ? 'none' : '';
        });

        lista.querySelectorAll('.pais-grupo').forEach(function(grupo) {
            var siguiente = grupo.nextElementSibling;
            var tieneVisibles = false;
            while (siguiente && !siguiente.classList.contains('pais-grupo')) {
                if (siguiente.classList.contains('pais-opcion') &&
                    siguiente.style.display !== 'none' &&
                    !siguiente.classList.contains('otro-opcion')) {
                    tieneVisibles = true;
                    break;
                }
                siguiente = siguiente.nextElementSibling;
            }
            grupo.style.display = tieneVisibles ? '' : 'none';
        });
    }

    // ============================================
    // INICIALIZACIÓN DE FILA
    // ============================================
    function inicializarFila(fila) {
        var selector = fila.querySelector('.pais-selector');
        var boton = fila.querySelector('.pais-boton');
        var lista = fila.querySelector('.pais-lista');
        var busqueda = fila.querySelector('.pais-busqueda input');
        var valorInput = fila.querySelector('.pais-valor');
        var prefijoContainer = fila.querySelector('.prefijo-container');
        var prefijoInput = fila.querySelector('.prefijo-input');
        var numeroInput = fila.querySelector('.numero-input');

        if (!selector || !boton || !lista || !numeroInput) return;

        var codigoActual = '+57';
        var esOtro = false;

        lista.innerHTML = generarListaHTML(codigoActual);

        // Parsear valor existente
        var parsed = parsearTelefono(numeroInput.value);
        if (parsed) {
            var nombrePais = buscarNombrePais(parsed.codigo);
            if (nombrePais) {
                codigoActual = parsed.codigo;
                // Soportar ambos formatos de botón (clientes y proveedores)
                var textoSpan = boton.querySelector('.pais-texto');
                var codigoSpan = boton.querySelector('.codigo-mostrar');
                var nombreSpan = boton.querySelector('.pais-nombre-mostrar');

                if (textoSpan) {
                    textoSpan.textContent = parsed.codigo + ' ' + nombrePais;
                }
                if (codigoSpan) codigoSpan.textContent = parsed.codigo;
                if (nombreSpan) nombreSpan.textContent = nombrePais;

                valorInput.value = parsed.codigo;
                lista.innerHTML = generarListaHTML(codigoActual);
            } else if (parsed.codigo && parsed.codigo.charAt(0) === '+') {
                esOtro = true;
                codigoActual = parsed.codigo;
                var textoS = boton.querySelector('.pais-texto');
                var nombreS = boton.querySelector('.pais-nombre-mostrar');
                if (textoS) textoS.textContent = 'Otro';
                if (nombreS) nombreS.textContent = 'Otro';
                valorInput.value = 'otro';
                if (prefijoInput) prefijoInput.value = parsed.codigo;
                if (prefijoContainer) prefijoContainer.classList.add('visible');
                if (fila.classList) fila.classList.add('modo-otro');
            }
            numeroInput.value = parsed.numero;
        }

        // Obtener código final (usado en submit)
        fila.getCodigoFinal = function() {
            if (esOtro || (valorInput && valorInput.value === 'otro')) {
                return prefijoInput ? prefijoInput.value.trim() || '+' : '+';
            }
            return valorInput ? valorInput.value : codigoActual;
        };

        // Botón volver (solo proveedores)
        var btnVolver = fila.querySelector('.btn-volver-pais');
        if (btnVolver) {
            btnVolver.addEventListener('click', function(e) {
                e.preventDefault();
                esOtro = false;
                fila.classList.remove('modo-otro');
                if (prefijoContainer) prefijoContainer.classList.remove('visible');
                codigoActual = '+57';
                var cs = boton.querySelector('.codigo-mostrar');
                var ns = boton.querySelector('.pais-nombre-mostrar');
                if (cs) cs.textContent = '+57';
                if (ns) ns.textContent = 'Colombia';
                valorInput.value = '+57';
                lista.innerHTML = generarListaHTML('+57');
            });
        }

        // Abrir/cerrar dropdown
        boton.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            document.querySelectorAll('.pais-selector.abierto').forEach(function(s) {
                if (s !== selector) {
                    s.classList.remove('abierto');
                    var btn = s.querySelector('.pais-boton');
                    if (btn) btn.classList.remove('activo');
                }
            });

            var abierto = selector.classList.toggle('abierto');
            boton.classList.toggle('activo', abierto);

            if (abierto && busqueda) {
                busqueda.value = '';
                filtrarLista(lista, '');
                setTimeout(function() { busqueda.focus(); }, 100);
            }
        });

        // Búsqueda
        if (busqueda) {
            busqueda.addEventListener('input', function() {
                filtrarLista(lista, this.value);
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

            var textoSpan = boton.querySelector('.pais-texto');
            var codigoSpan = boton.querySelector('.codigo-mostrar');
            var nombreSpan = boton.querySelector('.pais-nombre-mostrar');

            if (codigo === 'otro') {
                esOtro = true;
                if (textoSpan) textoSpan.textContent = 'Otro';
                if (nombreSpan) nombreSpan.textContent = 'Otro';
                if (codigoSpan) codigoSpan.textContent = '';
                valorInput.value = 'otro';
                if (prefijoContainer) prefijoContainer.classList.add('visible');
                if (fila.classList) fila.classList.add('modo-otro');
                if (prefijoInput) {
                    prefijoInput.value = '+';
                    setTimeout(function() { prefijoInput.focus(); }, 300);
                }
            } else {
                esOtro = false;
                codigoActual = codigo;
                if (textoSpan) textoSpan.textContent = codigo + ' ' + nombre;
                if (codigoSpan) codigoSpan.textContent = codigo;
                if (nombreSpan) nombreSpan.textContent = nombre;
                valorInput.value = codigo;
                if (prefijoContainer) prefijoContainer.classList.remove('visible');
                if (fila.classList) fila.classList.remove('modo-otro');
            }

            selector.classList.remove('abierto');
            boton.classList.remove('activo');
        });

        // Cerrar al clic fuera
        document.addEventListener('click', function(e) {
            if (!selector.contains(e.target)) {
                selector.classList.remove('abierto');
                boton.classList.remove('activo');
            }
        });

        // Escape
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && selector.classList.contains('abierto')) {
                selector.classList.remove('abierto');
                boton.classList.remove('activo');
            }
        });

        // Validar prefijo personalizado
        if (prefijoInput) {
            prefijoInput.addEventListener('input', function() {
                var v = this.value;
                if (!v.startsWith('+')) v = '+';
                var nums = v.substring(1).replace(/[^0-9]/g, '');
                if (nums.startsWith('0')) nums = nums.substring(1);
                this.value = '+' + nums.substring(0, 4);
            });

            prefijoInput.addEventListener('keypress', function(e) {
                var c = String.fromCharCode(e.which);
                if (!/[0-9]/.test(c)) e.preventDefault();
                if (c === '0' && this.value === '+') e.preventDefault();
            });
        }

        // Solo números en teléfono
        numeroInput.addEventListener('input', function() {
            var nums = this.value.replace(/[^0-9]/g, '');
            if (nums.startsWith('0')) nums = nums.substring(1);
            this.value = nums.substring(0, 10);
        });

        numeroInput.addEventListener('keypress', function(e) {
            var c = String.fromCharCode(e.which);
            if (!/[0-9]/.test(c)) { e.preventDefault(); return; }
            if (c === '0' && this.value.length === 0) { e.preventDefault(); return; }
            if (this.value.length >= 10) e.preventDefault();
        });
    }

    // ============================================
    // INICIALIZACIÓN MÚLTIPLE
    // ============================================

    /**
     * Inicializa todas las filas de teléfono que coincidan con el selector
     * @param {string} selector - Selector CSS (ej: '.telefono-fila')
     */
    function inicializarFilas(selector) {
        document.querySelectorAll(selector || '.telefono-fila').forEach(inicializarFila);
    }

    /**
     * Crea el HTML de una nueva fila de teléfono
     * @param {string} nombreCampo - Nombre del campo para el binding (ej: 'telefonos[0].telefono')
     * @returns {HTMLElement}
     */
    function crearFila(nombreCampo) {
        var fila = document.createElement('div');
        fila.className = 'telefono-fila';
        fila.innerHTML =
            '<div class="pais-selector">' +
                '<button type="button" class="pais-boton">' +
                    '<span class="pais-texto">+57 Colombia</span>' +
                    '<svg class="pais-flecha" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>' +
                '</button>' +
                '<div class="pais-dropdown">' +
                    '<div class="pais-busqueda"><input type="text" placeholder="Buscar país..." /></div>' +
                    '<div class="pais-lista"></div>' +
                '</div>' +
                '<input type="hidden" class="pais-valor" value="+57" />' +
            '</div>' +
            '<div class="prefijo-container">' +
                '<input type="text" class="prefijo-input" placeholder="+00" maxlength="5" />' +
            '</div>' +
            '<input type="text" name="' + nombreCampo + '" class="form-input numero-input" maxlength="10" placeholder="3001234567" />' +
            '<button type="button" class="btn btn-danger btn-sm btn-remove">' + ICONO_ELIMINAR + '</button>';

        inicializarFila(fila);
        return fila;
    }

    /**
     * Formatea todos los teléfonos de un formulario antes del submit
     * @param {string} selector - Selector de filas de teléfono
     * @returns {boolean} true si todos son válidos
     */
    function formatearParaSubmit(selector) {
        var ok = true;
        document.querySelectorAll(selector || '.telefono-fila').forEach(function(fila) {
            var numeroInput = fila.querySelector('.numero-input');
            var prefijoInput = fila.querySelector('.prefijo-input');

            if (numeroInput && numeroInput.value) {
                var codigo = fila.getCodigoFinal ? fila.getCodigoFinal() : '+57';

                if (!codigo || codigo === '+') {
                    ok = false;
                    if (prefijoInput) prefijoInput.classList.add('error');
                    return;
                }

                numeroInput.value = codigo + ' ' + numeroInput.value.replace(/[^0-9]/g, '');
            }
        });
        return ok;
    }

    // ============================================
    // API PÚBLICA
    // ============================================
    return {
        inicializarFila: inicializarFila,
        inicializarFilas: inicializarFilas,
        crearFila: crearFila,
        formatearParaSubmit: formatearParaSubmit,
        parsearTelefono: parsearTelefono,
        ICONO_ELIMINAR: ICONO_ELIMINAR
    };

})();
